package com.quran.labs.androidquran.presenter.translation;

import com.quran.data.core.QuranInfo;
import com.quran.data.di.QuranPageScope;
import com.quran.labs.androidquran.common.LocalTranslation;
import com.quran.labs.androidquran.common.QuranAyahInfo;
import com.quran.labs.androidquran.database.TranslationsDBAdapter;
import com.quran.labs.androidquran.model.translation.TranslationModel;
import com.quran.labs.androidquran.util.QuranSettings;
import com.quran.labs.androidquran.util.TranslationUtil;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.observers.DisposableObserver;

import javax.inject.Inject;

@QuranPageScope
public class TranslationPresenter {

    private final BaseTranslationPresenter<TranslationScreen> baseTranslationPresenter;
    private final QuranSettings quranSettings;
    private final QuranInfo quranInfo;
    private final int[] pages;

    @Inject
    public TranslationPresenter(TranslationModel translationModel,
                               QuranSettings quranSettings,
                               TranslationsDBAdapter translationsAdapter,
                               TranslationUtil translationUtil,
                               QuranInfo quranInfo,
                               int[] pages) {
        this.baseTranslationPresenter = new BaseTranslationPresenter<>(
                translationModel, translationsAdapter, translationUtil, quranInfo);
        this.quranSettings = quranSettings;
        this.quranInfo = quranInfo;
        this.pages = pages;
    }

    public void refresh() {
        baseTranslationPresenter.disposable.dispose();

        baseTranslationPresenter.disposable = Observable.fromArray(pages)
                .flatMap(page -> baseTranslationPresenter.getVerses(
                        quranSettings.wantArabicInTranslationView(),
                        baseTranslationPresenter.getTranslations(quranSettings),
                        quranInfo.getVerseRangeForPage(page)).toObservable())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<BaseTranslationPresenter.ResultHolder>() {
                    @Override
                    public void onNext(BaseTranslationPresenter.ResultHolder result) {
                        TranslationScreen screen = baseTranslationPresenter.translationScreen;
                        if (screen != null && !result.ayahInformation.isEmpty()) {
                            screen.setVerses(getPage(result.ayahInformation),
                                    result.translations, result.ayahInformation);
                            screen.updateScrollPosition();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }

    private int getPage(List<QuranAyahInfo> result) {
        Integer firstPage = pages.length > 0 ? pages[0] : null;
        if (pages.length == 1 && firstPage != null) {
            return firstPage;
        } else {
            return quranInfo.getPageFromSuraAyah(result.get(0).sura, result.get(0).ayah);
        }
    }

    public interface TranslationScreen {
        void setVerses(int page, LocalTranslation[] translations,
                       List<QuranAyahInfo> verses);

        void updateScrollPosition();
    }
}
