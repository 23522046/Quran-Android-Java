package com.quran.labs.androidquran.presenter.quran.ayahtracker;

import android.view.View;

import com.quran.data.core.QuranInfo;
import com.quran.data.model.AyahGlyph;
import com.quran.data.model.AyahWord;
import com.quran.data.model.SuraAyah;
import com.quran.data.model.highlight.HighlightType;
import com.quran.data.model.selection.SelectionIndicator;
import com.quran.labs.androidquran.data.QuranDisplayData;
import com.quran.labs.androidquran.ui.helpers.QuranDisplayHelper;
import com.quran.labs.androidquran.ui.util.ImageAyahUtils;
import com.quran.labs.androidquran.util.QuranUtils;
import com.quran.page.common.data.AyahBounds;
import com.quran.page.common.data.AyahCoordinates;
import com.quran.page.common.data.PageCoordinates;
import com.quran.page.common.data.coordinates.PageGlyphsCoords;
import com.quran.page.common.draw.ImageDrawHelper;
import com.quran.labs.androidquran.view.HighlightingImageView;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class AyahImageTrackerItem extends AyahTrackerItem {

    private Map<String, List<AyahBounds>> coordinates;
    private PageGlyphsCoords pageGlyphsCoords;
    private final QuranInfo quranInfo;
    private final QuranDisplayData quranDisplayData;
    private final boolean isPageOnRightSide;
    private final Set<ImageDrawHelper> imageDrawHelpers;
    private final HighlightingImageView ayahView;

    public AyahImageTrackerItem(
            int page,
            QuranInfo quranInfo,
            QuranDisplayData quranDisplayData,
            boolean isPageOnRightSide,
            Set<ImageDrawHelper> imageDrawHelpers,
            HighlightingImageView ayahView) {
        super(page);
        this.quranInfo = quranInfo;
        this.quranDisplayData = quranDisplayData;
        this.isPageOnRightSide = isPageOnRightSide;
        this.imageDrawHelpers = imageDrawHelpers;
        this.ayahView = ayahView;
    }

    @Override
    public void onSetPageBounds(PageCoordinates pageCoordinates) {
        if (this.page == pageCoordinates.page) {
            ayahView.setPageBounds(pageCoordinates.pageBounds);
            ayahView.setPageData(pageCoordinates, imageDrawHelpers);
            ayahView.invalidate();
        }
    }

    @Override
    public void onSetAyahCoordinates(AyahCoordinates ayahCoordinates) {
        if (this.page == ayahCoordinates.page) {
            coordinates = ayahCoordinates.ayahCoordinates;
            pageGlyphsCoords = ayahCoordinates.glyphCoordinates;
            if (!coordinates.isEmpty()) {
                ayahView.setAyahData(ayahCoordinates);
                ayahView.invalidate();
            }
        }
    }

    @Override
    public boolean onHighlightAyah(int page, int sura, int ayah, int word, HighlightType type, boolean scrollToAyah) {
        if (this.page == page && coordinates != null) {
            ayahView.highlightAyah(sura, ayah, word, type);
            ayahView.invalidate();
            return true;
        } else if (coordinates != null) {
            ayahView.unHighlight(type);
        }
        return false;
    }

    @Override
    public void onHighlightAyat(int page, Set<String> ayahKeys, HighlightType type) {
        if (this.page == page) {
            ayahView.highlightAyat(ayahKeys, type);
            ayahView.invalidate();
        }
    }

    @Override
    public void onUnHighlightAyah(int page, int sura, int ayah, HighlightType type) {
        if (this.page == page) {
            ayahView.unHighlight(sura, ayah, type);
        }
    }

    @Override
    public void onUnHighlightAyah(int page, int sura, int ayah, int word, HighlightType type) {
        if (this.page == page) {
            ayahView.unHighlight(sura, ayah, word, type);
        }
    }

    @Override
    public void onUnHighlightAyahType(HighlightType type) {
        ayahView.unHighlight(type);
    }

    @Override
    public SelectionIndicator getToolBarPosition(int page, int sura, int ayah) {
        if (this.page == page) {
            List<AyahBounds> bounds = coordinates != null ? coordinates.get(sura + ":" + ayah) : null;
            return getToolBarPosition(page, bounds);
        }
        return SelectionIndicator.None;
    }

    @Override
    public SelectionIndicator getToolBarPosition(int page, AyahWord word) {
        if (this.page == page) {
            List<AyahBounds> bounds = pageGlyphsCoords != null ? pageGlyphsCoords.getBoundsForWord(word) : null;
            return getToolBarPosition(page, bounds);
        }
        return SelectionIndicator.None;
    }

    @Override
    public SelectionIndicator getToolBarPosition(int page, AyahGlyph glyph) {
        if (this.page == page) {
            List<AyahBounds> bounds = pageGlyphsCoords != null ? pageGlyphsCoords.getBoundsForGlyph(glyph) : null;
            return getToolBarPosition(page, bounds);
        }
        return SelectionIndicator.None;
    }

    private SelectionIndicator getToolBarPosition(int page, List<AyahBounds> bounds) {
        if (this.page == page) {
            int screenWidth = ayahView.getWidth();
            if (bounds != null && screenWidth > 0) {
                int yPadding = ayahView.getPaddingTop();
                int xPadding = isPageOnRightSide ? ayahView.getWidth() : 0;
                return ImageAyahUtils.getToolBarPosition(bounds, ayahView.getImageMatrix(), xPadding, yPadding);
            }
        }
        return SelectionIndicator.None;
    }

    @Override
    public SuraAyah getAyahForPosition(int page, float x, float y) {
        if (this.page == page) {
            return ImageAyahUtils.getAyahFromCoordinates(coordinates, ayahView, x, y);
        }
        return null;
    }

    @Override
    public AyahGlyph getGlyphForPosition(int page, float x, float y) {
        if (this.page == page) {
            float[] pageXY = ImageAyahUtils.getPageXY(x, y, ayahView);
            if (pageXY != null) {
                float pgX = pageXY[0];
                float pgY = pageXY[1];
                return pageGlyphsCoords != null ? pageGlyphsCoords.getGlyphAtPoint(pgX, pgY) : null;
            }
        }
        return null;
    }

    @Override
    public View getAyahView() {
        return ayahView;
    }
}
