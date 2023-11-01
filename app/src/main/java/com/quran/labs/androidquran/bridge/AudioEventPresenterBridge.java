package com.quran.labs.androidquran.bridge;

import com.quran.data.model.SuraAyah;
import com.quran.reading.common.AudioEventPresenter;
import kotlinx.coroutines.flow.FlowCollector;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.MainScope;
import kotlinx.coroutines.flow.collect;
import kotlinx.coroutines.flow.flow;
import kotlinx.coroutines.flow.onEach;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.flow.FlowKt__TransformKt;

public class AudioEventPresenterBridge {

    private final MainScope scope = new MainScope();
    private final Flow<SuraAyah> audioPlaybackAyahFlow;

    public AudioEventPresenterBridge(AudioEventPresenter audioEventPresenter, FlowCollector<? super SuraAyah> flowCollector) {
        audioPlaybackAyahFlow = audioEventPresenter.getAudioPlaybackAyahFlow();
        FlowKt__TransformKt.onEach(audioPlaybackAyahFlow, (SuraAyah suraAyah) -> {
            flowCollector.emit(suraAyah);
        }, null, 2, null);
        CoroutineScope coroutineScope = scope;
        audioPlaybackAyahFlow.collect((FlowCollector)flowCollector);
    }

    public void dispose() {
        scope.cancel();
    }
}
