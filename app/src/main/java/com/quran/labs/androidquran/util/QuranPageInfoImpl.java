package com.quran.labs.androidquran.util;

import android.content.Context;
import com.quran.data.core.QuranInfo;
import com.quran.data.core.QuranPageInfo;
import com.quran.labs.androidquran.data.QuranDisplayData;
import com.quran.labs.androidquran.ui.helpers.QuranDisplayHelper;

public class QuranPageInfoImpl implements QuranPageInfo {

    private final Context context;
    private final QuranInfo quranInfo;
    private final QuranDisplayData quranDisplayData;

    public QuranPageInfoImpl(Context context, QuranInfo quranInfo, QuranDisplayData quranDisplayData) {
        this.context = context;
        this.quranInfo = quranInfo;
        this.quranDisplayData = quranDisplayData;
    }

    @Override
    public String juz(int page) {
        return quranDisplayData.getJuzDisplayStringForPage(context, page);
    }

    @Override
    public String suraName(int page) {
        return quranDisplayData.getSuraNameFromPage(context, page, true);
    }

    @Override
    public String displayRub3(int page) {
        return QuranDisplayHelper.displayRub3(context, quranInfo, page);
    }

    @Override
    public String localizedPage(int page) {
        return QuranUtils.getLocalizedNumber(context, page);
    }

    @Override
    public int pageForSuraAyah(int sura, int ayah) {
        return quranInfo.getPageFromSuraAyah(sura, ayah);
    }

    @Override
    public String manzilForPage(int page) {
        return quranDisplayData.getManzilForPage(context, page);
    }

    @Override
    public int skippedPagesCount() {
        return quranInfo.getSkip();
    }
}
