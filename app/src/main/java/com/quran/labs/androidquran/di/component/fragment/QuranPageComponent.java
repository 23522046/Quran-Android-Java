package com.quran.labs.androidquran.di.component.fragment;

import com.quran.data.di.QuranPageScope;
import com.quran.data.di.QuranReadingPageScope;
import com.quran.labs.androidquran.di.module.fragment.QuranPageModule;
import com.quran.labs.androidquran.ui.fragment.QuranPageFragment;
import com.quran.labs.androidquran.ui.fragment.TabletFragment;
import com.quran.labs.androidquran.ui.fragment.TranslationFragment;
import com.quran.mobile.di.QuranReadingPageComponent;
import com.squareup.anvil.annotations.MergeSubcomponent;

import dagger.Subcomponent;

@QuranPageScope
@MergeSubcomponent(QuranReadingPageScope.class, modules = {QuranPageModule.class})
public interface QuranPageComponent extends QuranReadingPageComponent {
    void inject(QuranPageFragment quranPageFragment);
    void inject(TabletFragment tabletFragment);
    void inject(TranslationFragment translationFragment);

    @Subcomponent.Builder
    interface Builder {
        Builder withQuranPageModule(QuranPageModule quranPageModule);
        QuranPageComponent build();
    }
}
