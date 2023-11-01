package com.quran.labs.androidquran.widget;

import com.quran.labs.androidquran.model.bookmark.BookmarkModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Singleton that monitors for changes to bookmarks and triggers {@link BookmarksWidget} updates.
 * Bookmark changes are only monitored if at least one {@link BookmarksWidget} exists.
 */
@Singleton
public class BookmarksWidgetSubscriber {

    private final BookmarkModel bookmarkModel;
    private final BookmarksWidgetUpdater bookmarksWidgetUpdater;
    private Disposable bookmarksWidgetDisposable;

    @Inject
    public BookmarksWidgetSubscriber(
            BookmarkModel bookmarkModel,
            BookmarksWidgetUpdater bookmarksWidgetUpdater
    ) {
        this.bookmarkModel = bookmarkModel;
        this.bookmarksWidgetUpdater = bookmarksWidgetUpdater;
    }

    public void subscribeBookmarksWidgetIfNecessary() {
        if (bookmarksWidgetUpdater.checkForAnyBookmarksWidgets()) {
            subscribeBookmarksWidget();
        }
    }

    private void subscribeBookmarksWidget() {
        bookmarksWidgetDisposable = bookmarkModel.bookmarksObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(bookmarksWidgetUpdater::updateBookmarksWidget);
    }

    public void onEnabledBookmarksWidget() {
        if (bookmarksWidgetDisposable == null) {
            subscribeBookmarksWidget();
        }
    }

    public void onDisabledBookmarksWidget() {
        if (bookmarksWidgetDisposable != null) {
            bookmarksWidgetDisposable.dispose();
        }
    }
}
