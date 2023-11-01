package com.quran.labs.androidquran.presenter.quran.ayahtracker;

import com.quran.data.core.QuranInfo;
import com.quran.data.model.SuraAyah;
import com.quran.data.model.highlight.HighlightType;
import com.quran.data.model.selection.SelectionIndicator;
import com.quran.labs.androidquran.common.LocalTranslation;
import com.quran.labs.androidquran.common.QuranAyahInfo;
import com.quran.labs.androidquran.ui.translation.TranslationView;

public class AyahTranslationTrackerItem extends AyahTrackerItem {
    private final QuranInfo quranInfo;
    private final TranslationView ayahView;

    public AyahTranslationTrackerItem(int page, QuranInfo quranInfo, TranslationView ayahView) {
        super(page);
        this.quranInfo = quranInfo;
        this.ayahView = ayahView;
    }

    @Override
    public boolean onHighlightAyah(int page, int sura, int ayah, int word, HighlightType type, boolean scrollToAyah) {
        if (this.page == page) {
            ayahView.highlightAyah(new SuraAyah(sura, ayah), quranInfo.getAyahId(sura, ayah), type);
            return true;
        }
        ayahView.unhighlightAyah(type);
        return false;
    }

    @Override
    public void onUnHighlightAyah(int page, int sura, int ayah, HighlightType type) {
        if (this.page == page) {
            ayahView.unhighlightAyah(type);
        }
    }

    @Override
    public void onUnHighlightAyahType(HighlightType type) {
        ayahView.unhighlightAyat(type);
    }

    @Override
    public SelectionIndicator getToolBarPosition(int page, int sura, int ayah) {
        return ayahView.getToolbarPosition(sura, ayah);
    }

    @Override
    public QuranAyahInfo getQuranAyahInfo(int sura, int ayah) {
        QuranAyahInfo quranAyahInfo = ayahView.getQuranAyahInfo(sura, ayah);
        return quranAyahInfo != null ? quranAyahInfo : super.getQuranAyahInfo(sura, ayah);
    }

    @Override
    public LocalTranslation[] getLocalTranslations() {
        LocalTranslation[] translations = ayahView.getLocalTranslations();
        return translations != null ? translations : super.getLocalTranslations();
    }
}
