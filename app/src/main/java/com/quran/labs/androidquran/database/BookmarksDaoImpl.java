package com.quran.labs.androidquran.database;

import com.quran.data.dao.BookmarksDao;
import com.quran.data.model.bookmark.Bookmark;
import com.quran.data.model.bookmark.RecentPage;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.withContext;

import java.util.List;

import javax.inject.Inject;

public class BookmarksDaoImpl implements BookmarksDao {

    private final BookmarksDBAdapter bookmarksDBAdapter;

    @Inject
    public BookmarksDaoImpl(BookmarksDBAdapter bookmarksDBAdapter) {
        this.bookmarksDBAdapter = bookmarksDBAdapter;
    }

    @Override
    public List<Bookmark> bookmarks() {
        return withContext(Dispatchers.IO, () -> bookmarksDBAdapter.getBookmarks(BookmarksDBAdapter.SORT_DATE_ADDED));
    }

    @Override
    public void replaceBookmarks(List<Bookmark> bookmarks) {
        withContext(Dispatchers.IO, () -> bookmarksDBAdapter.updateBookmarks(bookmarks));
    }

    @Override
    public void removeBookmarksForPage(int page) {
        withContext(Dispatchers.IO, () -> bookmarksDBAdapter.removeBookmarksForPage(page));
    }

    @Override
    public List<RecentPage> recentPages() {
        return withContext(Dispatchers.IO, bookmarksDBAdapter::getRecentPages);
    }

    @Override
    public void replaceRecentPages(List<RecentPage> pages) {
        withContext(Dispatchers.IO, () -> bookmarksDBAdapter.replaceRecentPages(pages));
    }

    @Override
    public void removeRecentPages() {
        withContext(Dispatchers.IO, bookmarksDBAdapter::removeRecentPages);
    }

    @Override
    public void removeRecentsForPage(int page) {
        withContext(Dispatchers.IO, () -> bookmarksDBAdapter.removeRecentsForPage(page));
    }
}
