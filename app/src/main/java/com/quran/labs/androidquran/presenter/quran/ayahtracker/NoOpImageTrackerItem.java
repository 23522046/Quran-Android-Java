package com.quran.labs.androidquran.presenter.quran.ayahtracker;

import com.quran.data.model.AyahGlyph;
import com.quran.data.model.AyahWord;
import com.quran.data.model.selection.SelectionIndicator;

public class NoOpImageTrackerItem extends AyahTrackerItem {
    public NoOpImageTrackerItem(int pageNumber) {
        super(pageNumber);
    }

    @Override
    public SelectionIndicator getToolBarPosition(int page, int sura, int ayah) {
        return SelectionIndicator.None;
    }

    @Override
    public SelectionIndicator getToolBarPosition(int page, AyahWord word) {
        return SelectionIndicator.None;
    }

    @Override
    public SelectionIndicator getToolBarPosition(int page, AyahGlyph glyph) {
        return SelectionIndicator.None;
    }
}
