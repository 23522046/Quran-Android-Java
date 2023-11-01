package com.quran.labs.androidquran.common;

import com.quran.labs.androidquran.ui.helpers.HighlightType;

public class HighlightInfo {
    private final int sura;
    private final int ayah;
    private final int word;
    private final HighlightType highlightType;
    private final boolean scrollToAyah;

    public HighlightInfo(int sura, int ayah, int word, HighlightType highlightType, boolean scrollToAyah) {
        this.sura = sura;
        this.ayah = ayah;
        this.word = word;
        this.highlightType = highlightType;
        this.scrollToAyah = scrollToAyah;
    }

    public int getSura() {
        return sura;
    }

    public int getAyah() {
        return ayah;
    }

    public int getWord() {
        return word;
    }

    public HighlightType getHighlightType() {
        return highlightType;
    }

    public boolean isScrollToAyah() {
        return scrollToAyah;
    }
}
