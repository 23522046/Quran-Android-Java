package com.quran.labs.androidquran.model.bookmark;

import android.annotation.SuppressLint;
import androidx.annotation.UiThread;
import com.quran.data.model.bookmark.RecentPage;
import com.quran.labs.androidquran.data.Constants;
import com.quran.labs.androidquran.database.BookmarksDBAdapter;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.DisposableSingleObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.PublishSubject;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RecentPageModel {

    private final BookmarksDBAdapter bookmarksDBAdapter;
    private final BehaviorSubject<Integer> lastPageSubject = BehaviorSubject.create();
    private final PublishSubject<Boolean> refreshRecentPagePublishSubject = PublishSubject.create();
    private final PublishSubject<PersistRecentPagesRequest> recentWriterSubject = PublishSubject.create();
    private final Observable<Boolean> recentPagesUpdatedObservable;
    private DisposableSingleObserver<List<RecentPage>> initialDataSubscription = null;

    @Inject
    public RecentPageModel(BookmarksDBAdapter bookmarksDBAdapter) {
        this.bookmarksDBAdapter = bookmarksDBAdapter;

        Observable<Boolean> recentWritesObservable = recentWriterSubject.hide()
                .observeOn(Schedulers.io())
                .map(update -> {
                    if (update.deleteRangeStart != null) {
                        bookmarksDBAdapter.replaceRecentRangeWithPage(
                                update.deleteRangeStart,
                                update.deleteRangeEnd,
                                update.page
                        );
                    } else {
                        bookmarksDBAdapter.addRecentPage(update.page);
                    }
                    return true;
                });

        recentPagesUpdatedObservable = Observable.merge(
                recentWritesObservable,
                refreshRecentPagePublishSubject.hide()
        ).share();

        recentPagesUpdatedObservable.subscribe();

        initialDataSubscription = getRecentPagesObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableSingleObserver<List<RecentPage>>() {
                    @Override
                    public void onSuccess(List<RecentPage> recentPages) {
                        int page = (recentPages.isEmpty()) ? Constants.NO_PAGE : recentPages.get(0).getPage();
                        lastPageSubject.onNext(page);
                        initialDataSubscription = null;
                    }

                    @Override
                    public void onError(Throwable e) {
                    }
                });
    }

    public void notifyRecentPagesUpdated() {
        refreshRecentPagePublishSubject.onNext(true);
    }

    @UiThread
    public void updateLatestPage(int page) {
        if (initialDataSubscription != null) {
            initialDataSubscription.dispose();
        }
        lastPageSubject.onNext(page);
    }

    @UiThread
    public void persistLatestPage(int minimumPage, int maximumPage, int lastPage) {
        Integer min = (minimumPage == maximumPage) ? null : minimumPage;
        Integer max = (min == null) ? null : maximumPage;
        recentWriterSubject.onNext(new PersistRecentPagesRequest(lastPage, min, max));
    }

    public Observable<Integer> getLatestPageObservable() {
        return lastPageSubject.hide();
    }

    public Observable<Boolean> getRecentPagesUpdatedObservable() {
        return recentPagesUpdatedObservable;
    }

    public Single<List<RecentPage>> getRecentPagesObservable() {
        return Single.fromCallable(() -> bookmarksDBAdapter.getRecentPages())
                .subscribeOn(Schedulers.io());
    }

    private static class PersistRecentPagesRequest {
        final int page;
        final Integer deleteRangeStart;
        final Integer deleteRangeEnd;

        PersistRecentPagesRequest(int page, Integer deleteRangeStart, Integer deleteRangeEnd) {
            this.page = page;
            this.deleteRangeStart = deleteRangeStart;
            this.deleteRangeEnd = deleteRangeEnd;
        }
    }
}
