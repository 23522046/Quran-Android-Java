package com.quran.labs.androidquran.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import com.quran.data.core.QuranInfo;
import com.quran.data.model.selection.AyahSelection;
import com.quran.data.model.selection.startSuraAyah;
import com.quran.labs.androidquran.common.toolbar.R;
import com.quran.labs.androidquran.ui.PagerActivity;
import com.quran.labs.androidquran.ui.helpers.SlidingPagerAdapter;
import com.quran.mobile.di.AyahActionFragmentProvider;
import com.quran.reading.common.AudioEventPresenter;
import com.quran.reading.common.ReadingEventPresenter;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.MainScope;
import kotlinx.coroutines.cancel;
import kotlinx.coroutines.flow.combine;
import kotlinx.coroutines.flow.launchIn;
import javax.inject.Inject;

public class TagBookmarkFragment extends TagBookmarkDialog {
    private CoroutineScope scope = new MainScope();

    @Inject
    ReadingEventPresenter readingEventPresenter;

    @Inject
    AudioEventPresenter audioEventPresenter;

    @Inject
    QuranInfo quranInfo;

    public static class Provider implements AyahActionFragmentProvider {
        @Override
        public int getOrder() {
            return SlidingPagerAdapter.TAG_PAGE;
        }

        @Override
        public int getIconResId() {
            return R.drawable.ic_tag;
        }

        @Override
        public TagBookmarkDialog newAyahActionFragment() {
            return new TagBookmarkFragment();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        PagerActivity pagerActivity = (PagerActivity) getActivity();
        if (pagerActivity != null) {
            pagerActivity.pagerActivityComponent.inject(this);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        scope = new MainScope();
        readingEventPresenter.getAyahSelectionFlow()
                .combine(audioEventPresenter.getAudioPlaybackAyahFlow(), (selectedAyah, playbackAyah) -> {
                    AyahSelection.None none = AyahSelection.None.INSTANCE;
                    AyahSelection start = (selectedAyah instanceof AyahSelection.None) ? none : selectedAyah.startSuraAyah();
                    AyahSelection playback = (playbackAyah != null) ? playbackAyah : null;

                    if (start != null) {
                        int page = quranInfo.getPageFromSuraAyah(start.getSura(), start.getAyah());
                        tagBookmarkPresenter.setAyahBookmarkMode(start.getSura(), start.getAyah(), page);
                    }
                    return null;
                })
                .launchIn(scope);
    }

    @Override
    public void onDestroy() {
        scope.cancel();
        super.onDestroy();
    }

    @Override
    public boolean shouldInject() {
        return false;
    }
}
