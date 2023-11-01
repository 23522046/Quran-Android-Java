package com.quran.labs.androidquran.presenter.quran.ayahtracker;

import android.view.View;
import com.quran.data.model.AyahGlyph;
import com.quran.data.model.AyahWord;
import com.quran.data.model.SuraAyah;
import com.quran.data.model.highlight.HighlightInfo;
import com.quran.data.model.highlight.HighlightType;
import com.quran.data.model.selection.SelectionIndicator;
import com.quran.labs.androidquran.common.LocalTranslation;
import com.quran.labs.androidquran.common.QuranAyahInfo;
import com.quran.page.common.data.AyahCoordinates;
import com.quran.page.common.data.PageCoordinates;
import java.util.Set;

public class AyahTrackerItem {

    private final int page;

    public AyahTrackerItem(int page) {
        this.page = page;
    }

    public void onSetPageBounds(PageCoordinates pageCoordinates) {
    }

    public void onSetAyahCoordinates(AyahCoordinates ayahCoordinates) {
    }

    public boolean onHighlight(int page, HighlightInfo highlightInfo) {
        return onHighlightAyah(page, highlightInfo.getSura(), highlightInfo.getAyah(),
                highlightInfo.getWord(), highlightInfo.getHighlightType(), highlightInfo.isScrollToAyah());
    }

    public void onUnhighlight(int page, HighlightInfo highlightInfo) {
        onUnHighlightAyah(page, highlightInfo.getSura(), highlightInfo.getAyah(),
                highlightInfo.getWord(), highlightInfo.getHighlightType());
    }

    public boolean onHighlightAyah(int page, int sura, int ayah, HighlightType type, boolean scrollToAyah) {
        return onHighlightAyah(page, sura, ayah, -1, type, scrollToAyah);
    }

    public boolean onHighlightAyah(int page, int sura, int ayah, int word, HighlightType type, boolean scrollToAyah) {
        return false;
    }

    public void onHighlightAyat(int page, Set<String> ayahKeys, HighlightType type) {
    }

    public void onUnHighlightAyah(int page, int sura, int ayah, HighlightType type) {
        onUnHighlightAyah(page, sura, ayah, -1, type);
    }

    public void onUnHighlightAyah(int page, int sura, int ayah, int word, HighlightType type) {
    }

    public void onUnHighlightAyahType(HighlightType type) {
    }

    public SelectionIndicator getToolBarPosition(int page, int sura, int ayah) {
        return SelectionIndicator.None;
    }

    public SelectionIndicator getToolBarPosition(int page, AyahWord word) {
        return getToolBarPosition(page, word.getAyah().getSura(), word.getAyah().getAyah());
    }

    public SelectionIndicator getToolBarPosition(int page, AyahGlyph glyph) {
        return getToolBarPosition(page, glyph.getAyah().getSura(), glyph.getAyah().getAyah());
    }

    public SuraAyah getAyahForPosition(int page, float x, float y) {
        return null;
    }

    public AyahGlyph getGlyphForPosition(int page, float x, float y) {
        return null;
    }

    public QuranAyahInfo getQuranAyahInfo(int sura, int ayah) {
        return null;
    }

    public LocalTranslation[] getLocalTranslations() {
        return null;
    }

    public View getAyahView() {
        return null;
    }
}
