package com.quran.labs.androidquran.widget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;

import javax.inject.Inject;

/**
 * Interface to allow for checking for {@link BookmarksWidget}s and updating them.
 */
public interface BookmarksWidgetUpdater {

    /**
     * Check if there's any {@link BookmarksWidget}s
     */
    boolean checkForAnyBookmarksWidgets();

    /**
     * Trigger updates of any {@link BookmarksWidget}s that currently exist
     */
    void updateBookmarksWidget();
}

public class BookmarksWidgetUpdaterImpl implements BookmarksWidgetUpdater {

    private final Context context;

    @Inject
    public BookmarksWidgetUpdaterImpl(Context context) {
        this.context = context;
    }

    @Override
    public boolean checkForAnyBookmarksWidgets() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager != null ?
                appWidgetManager.getAppWidgetIds(new ComponentName(context, BookmarksWidget.class)) : new int[0];
        return appWidgetIds != null && appWidgetIds.length > 0;
    }

    @Override
    public void updateBookmarksWidget() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        if (appWidgetManager != null) {
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, BookmarksWidget.class));
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.list_view_widget);
        }
    }
}
