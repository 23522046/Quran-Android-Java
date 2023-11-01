package com.quran.labs.androidquran.presenter.quran.ayahtracker;

import com.quran.data.core.QuranInfo;
import com.quran.data.model.AyahGlyph;
import com.quran.data.model.AyahWord;
import com.quran.data.model.highlight.HighlightType;
import com.quran.data.model.selection.SelectionIndicator;
import com.quran.labs.androidquran.data.QuranDisplayData;
import com.quran.labs.androidquran.ui.util.ImageAyahUtils;
import com.quran.labs.androidquran.view.HighlightingImageView;
import com.quran.labs.androidquran.view.QuranPageLayout;
import com.quran.page.common.draw.ImageDrawHelper;

import java.util.Set;

public class AyahScrollableImageTrackerItem extends AyahImageTrackerItem {

    private final int screenHeight;
    private final QuranPageLayout quranPageLayout;

    public AyahScrollableImageTrackerItem(
            int page,
            int screenHeight,
            QuranInfo quranInfo,
            QuranDisplayData quranDisplayData,
            QuranPageLayout quranPageLayout,
            Set<ImageDrawHelper> imageDrawHelpers,
            HighlightingImageView highlightingImageView) {
        super(page, quranInfo, quranDisplayData, false, imageDrawHelpers, highlightingImageView);
        this.screenHeight = screenHeight;
        this.quranPageLayout = quranPageLayout;
    }

    @Override
    public boolean onHighlightAyah(int page, int sura, int ayah, int word, HighlightType type, boolean scrollToAyah) {
        if (this.page == page && scrollToAyah && coordinates != null) {
            HighlightType coordinateType = HighlightType.fromType(type);
            coordinateType.setLastHighlightedVerse(sura, ayah);
            SelectionIndicator position = ImageAyahUtils.getYBoundsForHighlight(coordinates, coordinateType);

            if (position != null) {
                position.withYScroll(-quranPageLayout.getCurrentScrollY());
                quranPageLayout.smoothScrollLayoutTo((int) (position.getY()), false);
            }
        }
        return super.onHighlightAyah(page, sura, ayah, word, type, scrollToAyah);
    }

    @Override
    public SelectionIndicator getToolBarPosition(int page, int sura, int ayah) {
        SelectionIndicator position = super.getToolBarPosition(page, sura, ayah);
        position.withYScroll(-quranPageLayout.getCurrentScrollY());
        return position;
    }

    @Override
    public SelectionIndicator getToolBarPosition(int page, AyahWord word) {
        SelectionIndicator position = super.getToolBarPosition(page, word);
        position.withYScroll(-quranPageLayout.getCurrentScrollY());
        return position;
    }

    @Override
    public SelectionIndicator getToolBarPosition(int page, AyahGlyph glyph) {
        SelectionIndicator position = super.getToolBarPosition(page, glyph);
        position.withYScroll(-quranPageLayout.getCurrentScrollY());
        return position;
    }
}
