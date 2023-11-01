package com.quran.labs.androidquran.database;

import androidx.core.util.Pair;
import com.quran.data.model.bookmark.Bookmark;
import com.quran.data.model.bookmark.BookmarkData;
import com.quran.data.model.bookmark.RecentPage;
import com.quran.data.model.bookmark.Tag;
import com.quran.labs.androidquran.BookmarksDatabase;
import com.quran.labs.androidquran.data.Constants;
import com.quran.mobile.bookmark.mapper.Mappers;
import com.quran.mobile.bookmark.mapper.convergeCommonlyTagged;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class BookmarksDBAdapter {

    private final TagQueries tagQueries;
    private final BookmarkQueries bookmarkQueries;
    private final LastPageQueries lastPageQueries;
    private final BookmarkTagQueries bookmarkTagQueries;

    public static final int SORT_DATE_ADDED = 0;
    public static final int SORT_LOCATION = 1;

    @Inject
    public BookmarksDBAdapter(BookmarksDatabase bookmarksDatabase) {
        this.tagQueries = bookmarksDatabase.tagQueries();
        this.bookmarkQueries = bookmarksDatabase.bookmarkQueries();
        this.lastPageQueries = bookmarksDatabase.lastPageQueries();
        this.bookmarkTagQueries = bookmarksDatabase.bookmarkTagQueries();
    }

    public List<Bookmark> getBookmarkedAyahsOnPage(int page) {
        return getBookmarks(SORT_LOCATION, page);
    }

    public List<Bookmark> getBookmarks(int sortOrder) {
        return getBookmarks(sortOrder, null);
    }

    private List<Bookmark> getBookmarks(int sortOrder, Integer pageFilter) {
        List<Bookmark> bookmarks;
        if (pageFilter != null) {
            bookmarks = bookmarkQueries.getBookmarksByPage(pageFilter, Mappers.bookmarkWithTagMapper).executeAsList();
        } else if (sortOrder == SORT_LOCATION) {
            bookmarks = bookmarkQueries.getBookmarksByLocation(Mappers.bookmarkWithTagMapper).executeAsList();
        } else {
            bookmarks = bookmarkQueries.getBookmarksByDateAdded(Mappers.bookmarkWithTagMapper).executeAsList();
        }
        return convergeCommonlyTagged(bookmarks);
    }

    public List<RecentPage> getRecentPages() {
        return lastPageQueries.getLastPages(Mappers.recentPageMapper).executeAsList();
    }

    public void replaceRecentPages(List<RecentPage> pages) {
        lastPageQueries.transaction(() -> {
            for (RecentPage page : pages) {
                addRecentPage(page.page());
            }
        });
    }

    public void removeRecentsForPage(int page) {
        lastPageQueries.transaction(() -> {
            List<RecentPage> lastPages = lastPageQueries.getLastPages().executeAsList();
            lastPages.removeIf(recentPage -> recentPage.page() == page);
            if (lastPages.size() != lastPagesWithoutPage.size()) {
                lastPageQueries.removeLastPages();
                lastPages.forEach(this::addRecentPage);
            }
        });
    }

    public void replaceRecentRangeWithPage(int deleteRangeStart, int deleteRangeEnd, int page) {
        long maxPages = Constants.MAX_RECENT_PAGES;
        lastPageQueries.replaceRangeWithPage(deleteRangeStart, deleteRangeEnd, page, maxPages);
    }

    public void addRecentPage(int page) {
        long maxPages = Constants.MAX_RECENT_PAGES;
        lastPageQueries.addLastPage(page, maxPages);
    }

    public void removeRecentPages() {
        lastPageQueries.removeLastPages();
    }

    public List<Long> getBookmarkTagIds(long bookmarkId) {
        return bookmarkTagQueries.getTagIdsForBookmark(bookmarkId).executeAsList();
    }

    public long getBookmarkId(Integer sura, Integer ayah, int page) {
        Bookmark bookmark;
        if (sura != null && ayah != null) {
            bookmark = bookmarkQueries.getBookmarkIdForSuraAyah(sura, ayah).executeAsOneOrNull();
        } else {
            bookmark = bookmarkQueries.getBookmarkIdForPage(page).executeAsOneOrNull();
        }
        return bookmark != null ? bookmark.id() : -1L;
    }

    public void bulkDelete(List<Long> tagIds, List<Long> bookmarkIds, List<Pair<Long, Long>> untag) {
        bookmarkQueries.transaction(() -> {
            if (!tagIds.isEmpty()) {
                bookmarkTagQueries.deleteByTagIds(tagIds);
                tagQueries.deleteByIds(tagIds);
            }

            if (!bookmarkIds.isEmpty()) {
                bookmarkQueries.deleteByIds(bookmarkIds);
            }

            for (Pair<Long, Long> pair : untag) {
                bookmarkTagQueries.untag(pair.first, pair.second);
            }
        });
    }

    public void updateBookmarks(List<Bookmark> bookmarks) {
        bookmarkQueries.transaction(() -> {
            for (Bookmark bookmark : bookmarks) {
                bookmarkQueries.update(bookmark.sura(), bookmark.ayah(), bookmark.page(), bookmark.id());
            }
        });
    }

    public void removeBookmarksForPage(int page) {
        Long bookmarkId = bookmarkQueries.getBookmarkIdForPage(page).executeAsOneOrNull();
        if (bookmarkId != null) {
            bookmarkTagQueries.transaction(() -> {
                bookmarkTagQueries.deleteByBookmarkIds(bookmarkId);
                bookmarkQueries.deleteByIds(bookmarkId);
            });
        }
    }

    public long addBookmarkIfNotExists(int sura, int ayah, int page) {
        long bookmarkId = getBookmarkId(sura, ayah, page);
        if (bookmarkId < 0) {
            bookmarkId = addBookmark(sura, ayah, page);
        }
        return bookmarkId;
    }

    public long addBookmark(Integer sura, Integer ayah, int page) {
        bookmarkQueries.addBookmark(sura, ayah, page);
        return getBookmarkId(sura, ayah, page);
    }

    public void removeBookmark(long bookmarkId) {
        bookmarkTagQueries.transaction(() -> {
            bookmarkTagQueries.deleteByBookmarkIds(bookmarkId);
            bookmarkQueries.deleteByIds(bookmarkId);
        });
    }

    public List<Tag> getTags() {
        return tagQueries.getTags(Mappers.tagMapper).executeAsList();
    }

    public long addTag(String name) {
        long existingTag = haveMatchingTag(name);
        if (existingTag == -1L) {
            tagQueries.addTag(name);
            return haveMatchingTag(name);
        } else {
            return existingTag;
        }
    }

    private long haveMatchingTag(String name) {
        Tag tag = tagQueries.tagByName(name).executeAsOneOrNull();
        return tag != null ? tag.id() : -1L;
    }

    public boolean updateTag(long id, String newName) {
        long existingTag = haveMatchingTag(newName);
        if (existingTag == -1L) {
            tagQueries.updateTag(newName, id);
            return true;
        } else {
            return false;
        }
    }

    public boolean tagBookmarks(long[] bookmarkIds, Set<Long> tagIds, boolean deleteNonTagged) {
        bookmarkTagQueries.transaction(() -> {
            if (deleteNonTagged) {
                bookmarkTagQueries.deleteByBookmarkIds(bookmarkIds);
            }

            for (Long tagId : tagIds) {
                for (Long bookmarkId : bookmarkIds) {
                    bookmarkTagQueries.replaceBookmarkTag(bookmarkId, tagId);
                }
            }
        });
        return true;
    }

    public boolean importBookmarks(BookmarkData data) {
        bookmarkQueries.transaction(() -> {
            bookmarkTagQueries.deleteAll();
            tagQueries.deleteAll();
            bookmarkQueries.deleteAll();

            List<Tag> tags = data.tags();
            for (Tag tag : tags) {
                tagQueries.restoreTag(tag.id(), tag.name(), System.currentTimeMillis());
            }

            List<Bookmark> bookmarks = data.bookmarks();
            for (Bookmark bookmark : bookmarks) {
                bookmarkQueries.restoreBookmark(
                    bookmark.id(),
                    bookmark.sura(),
                    bookmark.ayah(),
                    bookmark.page(),
                    bookmark.timestamp()
                );
                List<Long> tagIds = bookmark.tags();
                for (Long tagId : tagIds) {
                    bookmarkTagQueries.addBookmarkTag(bookmark.id(), tagId);
                }
            }
        });
        return true;
    }
}
