package com.quran.labs.androidquran.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import com.quran.labs.androidquran.QuranApplication;
import com.quran.labs.androidquran.QuranDataActivity;
import com.quran.labs.androidquran.R;
import com.quran.labs.androidquran.SearchActivity;
import com.quran.labs.androidquran.ShortcutsActivity;
import com.quran.labs.androidquran.ui.PagerActivity;
import com.quran.labs.androidquran.ui.QuranActivity;

import javax.inject.Inject;

/**
 * Widget that displays a list of bookmarks and some buttons for jumping into the app
 */
public class BookmarksWidget extends AppWidgetProvider {

    @Inject
    BookmarksWidgetSubscriber bookmarksWidgetSubscriber;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            Intent serviceIntent = new Intent(context, BookmarksWidgetService.class);
            serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME)));

            RemoteViews widget = new RemoteViews(context.getPackageName(), R.layout.bookmarks_widget);

            PendingIntent intent = PendingIntent.getActivity(context, 0,
                    new Intent(context, QuranDataActivity.class),
                    PendingIntent.FLAG_IMMUTABLE);
            widget.setOnClickPendingIntent(R.id.widget_icon_button, intent);

            intent = PendingIntent.getActivity(context, 0,
                    new Intent(context, SearchActivity.class),
                    PendingIntent.FLAG_IMMUTABLE);
            widget.setOnClickPendingIntent(R.id.widget_btn_search, intent);

            intent = PendingIntent.getActivity(context, 0,
                    new Intent(context, QuranActivity.class),
                    PendingIntent.FLAG_IMMUTABLE);
            widget.setOnClickPendingIntent(R.id.widget_btn_go_to_quran, intent);

            intent = PendingIntent.getActivity(context, 0,
                    new Intent(context, ShowJumpFragmentActivity.class),
                    PendingIntent.FLAG_IMMUTABLE);
            widget.setOnClickPendingIntent(R.id.search_widget_btn_jump, intent);

            widget.setRemoteAdapter(R.id.list_view_widget, serviceIntent);

            Intent clickIntent = new Intent(context, PagerActivity.class);
            PendingIntent clickPendingIntent = PendingIntent.getActivity(context, 0,
                    clickIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
            widget.setPendingIntentTemplate(R.id.list_view_widget, clickPendingIntent);
            widget.setEmptyView(R.id.list_view_widget, R.id.empty_view);

            appWidgetManager.updateAppWidget(appWidgetId, widget);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onEnabled(Context context) {
        ((QuranApplication) context.getApplicationContext()).getApplicationComponent().inject(this);
        bookmarksWidgetSubscriber.onEnabledBookmarksWidget();
        super.onEnabled(context);
    }

    @Override
    public void onDisabled(Context context) {
        ((QuranApplication) context.getApplicationContext()).getApplicationComponent().inject(this);
        bookmarksWidgetSubscriber.onDisabledBookmarksWidget();
        super.onDisabled(context);
    }
}
