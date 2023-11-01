package com.quran.labs.androidquran.model.bookmark;

import com.quran.data.model.bookmark.BookmarkData;
import okio.BufferedSink;

import java.io.IOException;

public class BookmarkCSVAdapter {
    @Throws(IOException.class)
    public static void toCSV(BufferedSink sink, BookmarkData bookmarks) throws IOException {
        String bookmarksName = bookmarks.getBookmarks().isEmpty() ? "" : bookmarks.getBookmarks().get(0).getCommaSeparatedNames();
        String bookmarksLine = bookmarks.getBookmarksByLine() != null ? bookmarks.getBookmarksByLine() : "";
        String recentPagesLine = bookmarks.getRecentPagesByLine() != null ? bookmarks.getRecentPagesByLine() : "";

        sink.writeUtf8(bookmarksName);
        sink.writeUtf8("\n");
        sink.writeUtf8(bookmarksLine);
        sink.writeUtf8(recentPagesLine);
    }
}
