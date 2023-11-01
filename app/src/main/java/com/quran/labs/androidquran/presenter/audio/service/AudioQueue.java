package com.quran.labs.androidquran.presenter.audio.service;

import com.quran.data.core.QuranInfo;
import com.quran.labs.androidquran.dao.audio.AudioPlaybackInfo;
import com.quran.labs.androidquran.dao.audio.AudioRequest;
import com.quran.data.model.SuraAyah;
import com.quran.labs.androidquran.extension.requiresBasmallah;

import java.util.Locale;

/**
 * This class maintains a virtual audio queue for playback.
 * Given an [AudioRequest], this class maintains the queue for this request,
 * supporting operations such as switching to the next or previous ayahs or
 * jumping to an ayah. This class doesn't do the actual playback, but it
 * dictates what to play (while respecting repeat settings) and where to
 * find it.
 */
public class AudioQueue {
    private final QuranInfo quranInfo;
    private final AudioRequest audioRequest;
    private AudioPlaybackInfo playbackInfo;

    public AudioQueue(QuranInfo quranInfo, AudioRequest audioRequest, AudioPlaybackInfo initialPlaybackInfo) {
        this.quranInfo = quranInfo;
        this.audioRequest = audioRequest;
        this.playbackInfo = initialPlaybackInfo;
    }

    public boolean playAt(int sura, int ayah, boolean skipAyahRepeat) {
        AudioPlaybackInfo updatedPlaybackInfo = skipAyahRepeat || shouldRepeat(audioRequest.getRepeatInfo(), playbackInfo.getTimesPlayed())
                ? playbackInfo.copy(
                        playbackInfo.getTimesPlayed() + 1,
                        false
                )
                : (audioRequest.isGapless() && audioRequest.getEnforceBounds() && isOutOfBounds(audioRequest, sura, ayah))
                        ? shouldRepeat(audioRequest.getRangeRepeatInfo(), playbackInfo.getRangePlayedTimes())
                                ? playbackInfo.copy(
                                        audioRequest.getStart(),
                                        1,
                                        playbackInfo.getRangePlayedTimes() + 1,
                                        !audioRequest.isGapless() && playbackInfo.getCurrentAyah().requiresBasmallah()
                                )
                                : playbackInfo
                        : playbackInfo.copy(
                                new SuraAyah(sura, ayah),
                                1,
                                !audioRequest.isGapless() && !playbackInfo.getCurrentAyah().equals(new SuraAyah(sura, ayah)) && new SuraAyah(sura, ayah).requiresBasmallah()
                        );

        boolean result = updatedPlaybackInfo != playbackInfo;
        playbackInfo = updatedPlaybackInfo;
        return result;
    }

    public int getCurrentSura() {
        return playbackInfo.getCurrentAyah().getSura();
    }

    public int getCurrentAyah() {
        return playbackInfo.getCurrentAyah().getAyah();
    }

    public boolean playNextAyah(boolean skipAyahRepeat) {
        if (playbackInfo.shouldPlayBasmallah()) {
            playbackInfo = playbackInfo.copy(false);
            return true;
        }
        SuraAyah next = playbackInfo.getCurrentAyah().nextAyah();
        return playAt(next.getSura(), next.getAyah(), skipAyahRepeat);
    }

    public boolean playPreviousAyah(boolean skipAyahRepeat) {
        SuraAyah previous = playbackInfo.getCurrentAyah().previousAyah();
        boolean result = playAt(previous.getSura(), previous.getAyah(), skipAyahRepeat);
        if (playbackInfo.shouldPlayBasmallah()) {
            playbackInfo = playbackInfo.copy(false);
        }
        return result;
    }

    public String getUrl() {
        SuraAyah current = playbackInfo.getCurrentAyah();
        int currentSura = current.getSura();
        int currentAyah = current.getAyah();
        if (audioRequest.getEnforceBounds() && isOutOfBounds(audioRequest, currentSura, currentAyah)) {
            return null;
        }

        int sura = playbackInfo.shouldPlayBasmallah() ? 1 : currentSura;
        int ayah = playbackInfo.shouldPlayBasmallah() ? 1 : currentAyah;

        return String.format(Locale.US, audioRequest.getAudioPathInfo().getUrlFormat(), sura, ayah);
    }

    public AudioQueue withUpdatedAudioRequest(AudioRequest updatedAudioRequest) {
        return new AudioQueue(quranInfo, updatedAudioRequest, playbackInfo);
    }

    private boolean shouldRepeat(int repeatValue, int currentPlaybacks) {
        // Subtract 1 from currentPlaybacks because currentPlaybacks starts at 1
        // So repeating once requires having played twice.
        return repeatValue == -1 || repeatValue > currentPlaybacks - 1;
    }

    private boolean isOutOfBounds(AudioRequest audioRequest, int sura, int ayah) {
        SuraAyah start = audioRequest.getStart();
        SuraAyah end = audioRequest.getEnd();
        return sura > end.getSura() || (end.getSura() == sura && ayah > end.getAyah())
                || sura < start.getSura() || (start.getSura() == sura && ayah < start.getAyah());
    }
}
