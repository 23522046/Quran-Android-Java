package com.quran.labs.androidquran.di.module.fragment;

import dagger.Module;
import dagger.Provides;

@Module
public class QuranPageModule {

    private final int[] pages;

    public QuranPageModule(int... pages) {
        this.pages = pages;
    }

    @Provides
    public int[] providePages() {
        return pages;
    }
}
