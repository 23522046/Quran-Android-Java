package com.quran.labs.androidquran.dao.audio;

import android.os.Parcelable;
import com.quran.labs.androidquran.common.audio.model.QariItem;
import com.quran.data.model.SuraAyah;
import kotlinx.parcelize.Parcelize;

@Parcelize
public class AudioRequest implements Parcelable {
    private final SuraAyah start;
    private final SuraAyah end;
    private final QariItem qari;
    private final int repeatInfo;
    private final int rangeRepeatInfo;
    private final boolean enforceBounds;
    private final boolean shouldStream;
    private final AudioPathInfo audioPathInfo;

    public AudioRequest(SuraAyah start, SuraAyah end, QariItem qari, int repeatInfo, int rangeRepeatInfo, boolean enforceBounds, boolean shouldStream, AudioPathInfo audioPathInfo) {
        this.start = start;
        this.end = end;
        this.qari = qari;
        this.repeatInfo = repeatInfo;
        this.rangeRepeatInfo = rangeRepeatInfo;
        this.enforceBounds = enforceBounds;
        this.shouldStream = shouldStream;
        this.audioPathInfo = audioPathInfo;
    }

    public SuraAyah getStart() {
        return start;
    }

    public SuraAyah getEnd() {
        return end;
    }

    public QariItem getQari() {
        return qari;
    }

    public int getRepeatInfo() {
        return repeatInfo;
    }

    public int getRangeRepeatInfo() {
        return rangeRepeatInfo;
    }

    public boolean isEnforceBounds() {
        return enforceBounds;
    }

    public boolean isShouldStream() {
        return shouldStream;
    }

    public AudioPathInfo getAudioPathInfo() {
        return audioPathInfo;
    }

    public boolean isGapless() {
        return qari.isGapless();
    }

    public boolean needsIsti3athaAudio() {
        return !isGapless() || (audioPathInfo.getGaplessDatabase() != null && audioPathInfo.getGaplessDatabase().contains("minshawi_murattal"));
    }
}
