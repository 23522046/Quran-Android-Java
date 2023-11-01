package com.quran.labs.androidquran.presenter.quran.ayahtracker;

import android.view.View;
import com.quran.data.model.AyahGlyph;
import com.quran.data.model.SuraAyah;
import com.quran.data.model.highlight.HighlightType;
import com.quran.data.model.selection.SelectionIndicator;
import com.quran.page.common.data.AyahCoordinates;
import com.quran.page.common.data.PageCoordinates;

public class AyahSplitConsolidationTrackerItem extends AyahTrackerItem {

    private final AyahImageTrackerItem imageTrackerItem;
    private final AyahTranslationTrackerItem translationTrackerItem;

    public AyahSplitConsolidationTrackerItem(
            int page,
            AyahImageTrackerItem imageTrackerItem,
            AyahTranslationTrackerItem translationTrackerItem) {
        super(page);
        this.imageTrackerItem = imageTrackerItem;
        this.translationTrackerItem = translationTrackerItem;
    }

    @Override
    public void onSetPageBounds(PageCoordinates pageCoordinates) {
        imageTrackerItem.onSetPageBounds(pageCoordinates);
        translationTrackerItem.onSetPageBounds(pageCoordinates);
    }

    @Override
    public void onSetAyahCoordinates(AyahCoordinates ayahCoordinates) {
        imageTrackerItem.onSetAyahCoordinates(ayahCoordinates);
        translationTrackerItem.onSetAyahCoordinates(ayahCoordinates);
    }

    @Override
    public boolean onHighlightAyah(
            int page, int sura, int ayah, int word, HighlightType type, boolean scrollToAyah) {
        boolean firstResult = imageTrackerItem.onHighlightAyah(page, sura, ayah, word, type, scrollToAyah);
        boolean secondResult = translationTrackerItem.onHighlightAyah(page, sura, ayah, word, type, scrollToAyah);
        return firstResult && secondResult;
    }

    @Override
    public void onHighlightAyat(int page, Set<String> ayahKeys, HighlightType type) {
        imageTrackerItem.onHighlightAyat(page, ayahKeys, type);
        translationTrackerItem.onHighlightAyat(page, ayahKeys, type);
    }

    @Override
    public void onUnHighlightAyah(int page, int sura, int ayah, HighlightType type) {
        imageTrackerItem.onUnHighlightAyah(page, sura, ayah, type);
        translationTrackerItem.onUnHighlightAyah(page, sura, ayah, type);
    }

    @Override
    public void onUnHighlightAyahType(HighlightType type) {
        imageTrackerItem.onUnHighlightAyahType(type);
        translationTrackerItem.onUnHighlightAyahType(type);
    }

    @Override
    public SelectionIndicator getToolBarPosition(int page, int sura, int ayah) {
        return imageTrackerItem.getToolBarPosition(page, sura, ayah);
    }

    @Override
    public SuraAyah getAyahForPosition(int page, float x, float y) {
        return imageTrackerItem.getAyahForPosition(page, x, y);
    }

    @Override
    public AyahGlyph getGlyphForPosition(int page, float x, float y) {
        return imageTrackerItem.getGlyphForPosition(page, x, y);
    }

    @Override
    public View getAyahView() {
        return imageTrackerItem.getAyahView();
    }
}
