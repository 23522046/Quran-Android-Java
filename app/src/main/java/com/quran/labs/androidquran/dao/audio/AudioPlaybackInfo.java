package com.quran.labs.androidquran.dao.audio;

import com.quran.data.model.SuraAyah;

public class AudioPlaybackInfo {
    private final SuraAyah currentAyah;
    private final int timesPlayed;
    private final int rangePlayedTimes;
    private final boolean shouldPlayBasmallah;

    public AudioPlaybackInfo(SuraAyah currentAyah, int timesPlayed, int rangePlayedTimes, boolean shouldPlayBasmallah) {
        this.currentAyah = currentAyah;
        this.timesPlayed = timesPlayed;
        this.rangePlayedTimes = rangePlayedTimes;
        this.shouldPlayBasmallah = shouldPlayBasmallah;
    }

    public SuraAyah getCurrentAyah() {
        return currentAyah;
    }

    public int getTimesPlayed() {
        return timesPlayed;
    }

    public int getRangePlayedTimes() {
        return rangePlayedTimes;
    }

    public boolean isShouldPlayBasmallah() {
        return shouldPlayBasmallah;
    }
}
