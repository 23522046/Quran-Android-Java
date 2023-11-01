package com.quran.labs.androidquran.widget;

import android.content.Intent;
import android.widget.RemoteViewsService;

/**
 * Service to populate list of bookmarks in {@link BookmarksWidget}
 */
public class BookmarksWidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new BookmarksWidgetListProvider(getApplicationContext());
    }
}
