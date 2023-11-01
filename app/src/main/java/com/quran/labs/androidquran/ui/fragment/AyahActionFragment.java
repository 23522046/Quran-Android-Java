package com.quran.labs.androidquran.ui.fragment;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import com.quran.data.model.SuraAyah;
import com.quran.data.model.selection.AyahSelection;
import com.quran.data.model.selection.SelectionUtils;
import com.quran.reading.common.AudioEventPresenter;
import com.quran.reading.common.ReadingEventPresenter;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.MainScope;
import kotlinx.coroutines.cancel;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.collect;
import kotlinx.coroutines.flow.combine;
import kotlinx.coroutines.flow.map;
import javax.inject.Inject;

public abstract class AyahActionFragment extends Fragment {

    private CoroutineScope scope = new MainScope();

    @Inject
    ReadingEventPresenter readingEventPresenter;

    @Inject
    AudioEventPresenter audioEventPresenter;

    protected SuraAyah start;
    protected SuraAyah end;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        scope = new MainScope();
        Flow<AyahSelection> ayahSelectionFlow = readingEventPresenter.getAyahSelectionFlow();
        Flow<SuraAyah> audioPlaybackAyahFlow = audioEventPresenter.getAudioPlaybackAyahFlow();

        Flow.combine(ayahSelectionFlow, audioPlaybackAyahFlow,
                (selectedAyah, playbackAyah) -> {
                    if (selectedAyah != AyahSelection.NONE) {
                        start = SelectionUtils.getStartSuraAyah(selectedAyah);
                        end = SelectionUtils.getEndSuraAyah(selectedAyah);
                    } else if (playbackAyah != null) {
                        start = playbackAyah;
                        end = playbackAyah;
                    }
                    refreshView();
                }).collect();

        readingEventPresenter.getDetailsPanelFlow()
                .map(isVisible -> onToggleDetailsPanel(isVisible))
                .collect();
    }

    @Override
    public void onDestroy() {
        scope.cancel();
        super.onDestroy();
    }

    public abstract void onToggleDetailsPanel(boolean isVisible);

    protected abstract void refreshView();
}
