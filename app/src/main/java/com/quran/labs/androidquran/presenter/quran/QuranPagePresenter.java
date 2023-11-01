package com.quran.labs.androidquran.presenter.quran;

import com.quran.data.core.QuranInfo;
import com.quran.data.di.QuranPageScope;
import com.quran.labs.androidquran.R;
import com.quran.labs.androidquran.common.Response;
import com.quran.labs.androidquran.model.quran.CoordinatesModel;
import com.quran.labs.androidquran.presenter.Presenter;
import com.quran.labs.androidquran.ui.helpers.QuranPageLoader;
import com.quran.labs.androidquran.util.QuranSettings;
import com.quran.page.common.data.AyahCoordinates;
import com.quran.page.common.data.PageCoordinates;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.observers.DisposableObserver;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

@QuranPageScope
public class QuranPagePresenter implements Presenter<QuranPageScreen> {

    private final CoordinatesModel coordinatesModel;
    private final QuranSettings quranSettings;
    private final QuranPageLoader quranPageLoader;
    private final QuranInfo quranInfo;
    private final int[] pages;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private QuranPageScreen screen;
    private boolean encounteredError = false;
    private boolean didDownloadImages = false;

    @Inject
    public QuranPagePresenter(
            CoordinatesModel coordinatesModel,
            QuranSettings quranSettings,
            QuranPageLoader quranPageLoader,
            QuranInfo quranInfo,
            int[] pages
    ) {
        this.coordinatesModel = coordinatesModel;
        this.quranSettings = quranSettings;
        this.quranPageLoader = quranPageLoader;
        this.quranInfo = quranInfo;
        this.pages = pages;
    }

    @Override
    public void bind(QuranPageScreen screen) {
        this.screen = screen;
        if (!didDownloadImages) {
            downloadImages();
        }
        getPageCoordinates(pages);
    }

    @Override
    public void unbind(QuranPageScreen screen) {
        this.screen = null;
        compositeDisposable.clear();
    }

    private void getPageCoordinates(int[] pages) {
        compositeDisposable.add(
                coordinatesModel.getPageCoordinates(quranSettings.shouldOverlayPageInfo(), pages)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(new DisposableObserver<PageCoordinates>() {
                            @Override
                            public void onNext(PageCoordinates pageCoordinates) {
                                if (screen != null) {
                                    screen.setPageCoordinates(pageCoordinates);
                                }
                            }

                            @Override
                            public void onError(Throwable e) {
                                encounteredError = true;
                                if (screen != null) {
                                    screen.setAyahCoordinatesError();
                                }
                            }

                            @Override
                            public void onComplete() {
                                getAyahCoordinates(pages);
                            }
                        })
        );
    }

    private void getAyahCoordinates(int[] pages) {
        compositeDisposable.add(
                Completable.timer(500, TimeUnit.MILLISECONDS)
                        .andThen(Observable.fromArray(pages))
                        .flatMap(page -> coordinatesModel.getAyahCoordinates(page))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(new DisposableObserver<AyahCoordinates>() {
                            @Override
                            public void onNext(AyahCoordinates coordinates) {
                                if (screen != null) {
                                    screen.setAyahCoordinatesData(coordinates);
                                }
                            }

                            @Override
                            public void onError(Throwable e) {
                            }

                            @Override
                            public void onComplete() {
                            }
                        })
        );
    }

    public void downloadImages() {
        if (screen != null) {
            screen.hidePageDownloadError();
        }
        // drop empty pages - this happens in Shemerly, for example, where there are an odd number of
        // pages. in dual page mode, we have an empty page at the end, so we don't want to try to load
        // the empty page.
        int[] actualPages = new int[pages.length];
        int actualPageCount = 0;
        for (int page : pages) {
            if (quranInfo.isValidPage(page)) {
                actualPages[actualPageCount] = page;
                actualPageCount++;
            }
        }

        compositeDisposable.add(
                quranPageLoader.loadPages(actualPages)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(new DisposableObserver<Response>() {
                            @Override
                            public void onNext(Response response) {
                                if (screen != null) {
                                    if (response.getBitmap() != null) {
                                        didDownloadImages = true;
                                        screen.setPageBitmap(response.getPageNumber(), response.getBitmap());
                                    } else {
                                        didDownloadImages = false;
                                        int errorRes;
                                        switch (response.getErrorCode()) {
                                            case Response.ERROR_SD_CARD_NOT_FOUND:
                                                errorRes = R.string.sdcard_error;
                                                break;
                                            case Response.ERROR_NO_INTERNET:
                                            case Response.ERROR_DOWNLOADING_ERROR:
                                                errorRes = R.string.download_error_network;
                                                break;
                                            default:
                                                errorRes = R.string.download_error_general;
                                                break;
                                        }
                                        screen.setPageDownloadError(errorRes);
                                    }
                                }
                            }

                            @Override
                            public void onError(Throwable e) {
                            }

                            @Override
                            public void onComplete() {
                            }
                        })
        );
    }

    public void refresh() {
        if (encounteredError) {
            encounteredError = false;
            getPageCoordinates(pages);
        }
    }
}
