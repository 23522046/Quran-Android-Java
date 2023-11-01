package com.quran.labs.androidquran.widget;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.RemoteViews;

import com.quran.data.core.QuranInfo;
import com.quran.labs.androidquran.QuranApplication;
import com.quran.labs.androidquran.R;
import com.quran.labs.androidquran.database.BookmarksDBAdapter;
import com.quran.labs.androidquran.ui.PagerActivity;
import com.quran.labs.androidquran.ui.helpers.QuranRow;
import com.quran.labs.androidquran.ui.helpers.QuranRowFactory;

import java.util.List;

import javax.inject.Inject;

/**
 * {@link RemoteViewsFactory} implementation responsible for providing a list of bookmark views to be
 * displayed in {@link BookmarksWidget}.
 */
public class BookmarksWidgetListProvider implements RemoteViewsFactory {

    private Context context;
    private List<QuranRow> quranRowList;

    @Inject
    QuranInfo quranInfo;

    @Inject
    QuranRowFactory quranRowFactory;

    @Inject
    BookmarksDBAdapter bookmarksDbAdapter;

    public BookmarksWidgetListProvider(Context context) {
        this.context = context;
        ((QuranApplication) context.getApplicationContext()).getApplicationComponent().inject(this);
        populateListItem();
    }

    private void populateListItem() {
        List<BookmarksDBAdapter.Bookmark> bookmarksList = bookmarksDbAdapter.getBookmarks(BookmarksDBAdapter.SORT_LOCATION);
        quranRowList = quranRowFactory.fromBookmarkList(context, bookmarksList);
    }

    @Override
    public void onCreate() {
        // Not needed in this implementation
    }

    @Override
    public void onDestroy() {
        // Not needed in this implementation
    }

    @Override
    public void onDataSetChanged() {
        populateListItem();
    }

    @Override
    public int getCount() {
        return quranRowList.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews remoteView = new RemoteViews(context.getPackageName(), R.layout.bookmarks_widget_list_row);
        QuranRow item = quranRowList.get(position);
        remoteView.setTextViewText(R.id.sura_title, item.getText());
        remoteView.setTextViewText(R.id.sura_meta_data, item.getMetadata());
        remoteView.setImageViewResource(R.id.widget_favorite_icon, item.getImageResource());
        if (item.getImageFilterColor() == null) {
            // If a color filter isn't set, then sometimes the color filter of bookmarks can crossover into each other
            remoteView.setInt(R.id.widget_favorite_icon, "setColorFilter", Color.WHITE);
        } else {
            remoteView.setInt(R.id.widget_favorite_icon, "setColorFilter", item.getImageFilterColor());
        }
        Intent fillInIntent = new Intent();
        fillInIntent.putExtras(new Bundle());
        fillInIntent.putExtra("page", item.getBookmark().getPage());
        fillInIntent.putExtra(PagerActivity.EXTRA_HIGHLIGHT_SURA, item.getBookmark().getSura());
        fillInIntent.putExtra(PagerActivity.EXTRA_HIGHLIGHT_AYAH, item.getBookmark().getAyah());
        remoteView.setOnClickFillInIntent(R.id.widget_item, fillInIntent);
        return remoteView;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }
}
