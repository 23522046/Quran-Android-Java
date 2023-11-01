package com.quran.labs.androidquran.di.module.activity;

import com.quran.data.core.QuranInfo;
import com.quran.data.core.QuranPageInfo;
import com.quran.data.di.ActivityScope;
import com.quran.labs.androidquran.data.QuranDisplayData;
import com.quran.labs.androidquran.ui.PagerActivity;
import com.quran.labs.androidquran.ui.helpers.AyahSelectedListener;
import com.quran.labs.androidquran.util.QuranPageInfoImpl;
import com.quran.labs.androidquran.util.QuranScreenInfo;
import com.quran.labs.androidquran.util.QuranUtils;
import com.quran.labs.androidquran.util.TranslationUtil;
import com.quran.mobile.di.AyahActionFragmentProvider;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.ElementsIntoSet;

import java.util.Set;

@Module
public class PagerActivityModule {
    private final PagerActivity pagerActivity;

    public PagerActivityModule(PagerActivity pagerActivity) {
        this.pagerActivity = pagerActivity;
    }

    @Provides
    AyahSelectedListener provideAyahSelectedListener() {
        return pagerActivity;
    }

    @Provides
    QuranPageInfo provideQuranPageInfo(QuranInfo quranInfo, QuranDisplayData quranDisplayData) {
        return new QuranPageInfoImpl(pagerActivity, quranInfo, quranDisplayData);
    }

    @Provides
    @ActivityScope
    String provideImageWidth(QuranScreenInfo screenInfo) {
        return QuranUtils.isDualPages(pagerActivity, screenInfo) ? screenInfo.getTabletWidthParam() : screenInfo.getWidthParam();
    }

    @Provides
    @ActivityScope
    TranslationUtil provideTranslationUtil(QuranInfo quranInfo) {
        return new TranslationUtil(quranInfo);
    }

    @Provides
    @ElementsIntoSet
    Set<AyahActionFragmentProvider> provideAdditionalAyahPanels() {
        return Collections.emptySet();
    }
}
