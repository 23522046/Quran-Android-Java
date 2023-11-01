package com.quran.labs.androidquran.dao.bookmark;

import com.quran.data.model.bookmark.Tag;
import com.quran.labs.androidquran.ui.helpers.QuranRow;
import java.util.List;
import java.util.Map;

public class BookmarkResult {
    private final List<QuranRow> rows;
    private final Map<Long, Tag> tagMap;

    public BookmarkResult(List<QuranRow> rows, Map<Long, Tag> tagMap) {
        this.rows = rows;
        this.tagMap = tagMap;
    }

    public List<QuranRow> getRows() {
        return rows;
    }

    public Map<Long, Tag> getTagMap() {
        return tagMap;
    }
}
