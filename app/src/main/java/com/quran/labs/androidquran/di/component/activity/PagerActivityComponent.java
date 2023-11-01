package com.quran.labs.androidquran.di.component.activity;

import com.quran.data.di.QuranReadingScope;
import com.quran.data.di.ActivityScope;
import com.quran.labs.androidquran.di.component.fragment.QuranPageComponent;
import com.quran.labs.androidquran.di.module.activity.PagerActivityModule;
import com.quran.labs.androidquran.ui.PagerActivity;
import com.quran.labs.androidquran.ui.fragment.AyahPlaybackFragment;
import com.quran.labs.androidquran.ui.fragment.AyahTranslationFragment;
import com.quran.labs.androidquran.ui.fragment.TagBookmarkFragment;
import com.quran.page.common.toolbar.AyahToolBar;
import com.quran.mobile.di.QuranReadingActivityComponent;
import com.quran.mobile.feature.qarilist.QariListWrapper;
import com.squareup.anvil.annotations.MergeSubcomponent;
import dagger.Subcomponent;

@ActivityScope
@MergeSubcomponent(QuranReadingScope.class)
public interface PagerActivityComponent extends QuranReadingActivityComponent {
    // subcomponents
    QuranPageComponent.Builder quranPageComponentBuilder();

    void inject(PagerActivity pagerActivity);
    void inject(AyahToolBar ayahToolBar);

    void inject(TagBookmarkFragment tagBookmarkFragment);
    void inject(AyahPlaybackFragment ayahPlaybackFragment);
    void inject(AyahTranslationFragment ayahTranslationFragment);

    void inject(QariListWrapper qariListWrapper);

    @Subcomponent.Builder
    interface Builder {
        Builder withPagerActivityModule(PagerActivityModule pagerModule);
        PagerActivityComponent build();
    }
}
