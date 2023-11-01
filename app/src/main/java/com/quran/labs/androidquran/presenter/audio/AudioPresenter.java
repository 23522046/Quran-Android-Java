package com.quran.labs.androidquran.presenter.audio;

import android.content.Context;
import android.content.Intent;

import com.quran.data.model.SuraAyah;
import com.quran.labs.androidquran.R;
import com.quran.labs.androidquran.common.audio.model.QariItem;
import com.quran.labs.androidquran.dao.audio.AudioPathInfo;
import com.quran.labs.androidquran.dao.audio.AudioRequest;
import com.quran.labs.androidquran.data.QuranDisplayData;
import com.quran.labs.androidquran.presenter.Presenter;
import com.quran.labs.androidquran.service.QuranDownloadService;
import com.quran.labs.androidquran.service.util.ServiceIntentHelper;
import com.quran.labs.androidquran.ui.PagerActivity;
import com.quran.labs.androidquran.util.AudioUtils;
import com.quran.labs.androidquran.util.QuranFileUtils;

import java.io.File;

import timber.log.Timber;

public class AudioPresenter implements Presenter<PagerActivity> {
    private final QuranDisplayData quranDisplayData;
    private final AudioUtils audioUtil;
    private final QuranFileUtils quranFileUtils;
    private PagerActivity pagerActivity;
    private AudioRequest lastAudioRequest;

    public AudioPresenter(QuranDisplayData quranDisplayData, AudioUtils audioUtil, QuranFileUtils quranFileUtils) {
        this.quranDisplayData = quranDisplayData;
        this.audioUtil = audioUtil;
        this.quranFileUtils = quranFileUtils;
    }

    public void play(SuraAyah start, SuraAyah end, QariItem qari, int verseRepeat, int rangeRepeat, boolean enforceRange, boolean shouldStream) {
        AudioPathInfo audioPathInfo = getLocalAudioPathInfo(qari);
        if (audioPathInfo != null) {
            boolean stream = shouldStream && !haveAllFiles(audioPathInfo, start, end);

            String audioPath = stream ? audioUtil.getQariUrl(qari) : audioPathInfo.getUrlFormat();

            SuraAyah actualStart, actualEnd;
            if (start.compareTo(end) <= 0) {
                actualStart = start;
                actualEnd = end;
            } else {
                Timber.e(new IllegalStateException("End isn't larger than the start: " + start + " to " + end));
                actualStart = end;
                actualEnd = start;
            }

            AudioRequest audioRequest = new AudioRequest(actualStart, actualEnd, qari, verseRepeat, rangeRepeat, enforceRange, stream, audioPath);
            play(audioRequest);
        }
    }

    private void play(AudioRequest audioRequest) {
        lastAudioRequest = audioRequest;
        proceedWithAudioRequest(audioRequest);
    }

    private void proceedWithAudioRequest(AudioRequest audioRequest) {
        if (pagerActivity != null) {
            Intent downloadIntent = getDownloadIntent(pagerActivity, audioRequest);
            if (downloadIntent != null) {
                pagerActivity.proceedWithDownload(downloadIntent);
            } else {
                pagerActivity.handlePlayback(audioRequest);
            }
        }
    }

    public void onDownloadPermissionGranted() {
        if (lastAudioRequest != null) {
            play(lastAudioRequest);
        }
    }

    public void onPostNotificationsPermissionResponse(boolean granted) {
        if (lastAudioRequest != null) {
            proceedWithAudioRequest(lastAudioRequest, true);
        }
    }

    public void onDownloadSuccess() {
        if (lastAudioRequest != null) {
            play(lastAudioRequest);
        }
    }

    private Intent getDownloadIntent(Context context, AudioRequest request) {
        QariItem qari = request.getQari();
        AudioPathInfo audioPathInfo = request.getAudioPathInfo();
        String path = audioPathInfo.getLocalDirectory();
        String gaplessDb = audioPathInfo.getGaplessDatabase();

        if (!quranFileUtils.haveAyaPositionFile(context)) {
            return getDownloadIntent(context, quranFileUtils.getAyaPositionFileUrl(), quranFileUtils.getQuranAyahDatabaseDirectory(context),
                    context.getString(R.string.highlighting_database));
        } else if (gaplessDb != null && !new File(gaplessDb).exists()) {
            return getDownloadIntent(context, getGaplessDatabaseUrl(qari), path, context.getString(R.string.timing_database));
        } else if (!request.shouldStream() && audioUtil.shouldDownloadBasmallah(path, request.getStart(), request.getEnd(), qari.isGapless())) {
            String title = quranDisplayData.getNotificationTitle(context, request.getStart(), request.getStart(), qari.isGapless());
            Intent intent = getDownloadIntent(context, audioUtil.getQariUrl(qari), path, title);
            intent.putExtra(QuranDownloadService.EXTRA_START_VERSE, request.getStart());
            intent.putExtra(QuranDownloadService.EXTRA_END_VERSE, request.getStart());
            return intent;
        } else if (!request.shouldStream() && !haveAllFiles(audioPathInfo, request.getStart(), request.getEnd())) {
            String title = quranDisplayData.getNotificationTitle(context, request.getStart(), request.getEnd(), qari.isGapless());
            Intent intent = getDownloadIntent(context, audioUtil.getQariUrl(qari), path, title);
            intent.putExtra(QuranDownloadService.EXTRA_START_VERSE, request.getStart());
            intent.putExtra(QuranDownloadService.EXTRA_END_VERSE, request.getEnd());
            intent.putExtra(QuranDownloadService.EXTRA_IS_GAPLESS, qari.isGapless());
            intent.putExtra(QuranDownloadService.EXTRA_METADATA, new AudioDownloadMetadata(qari.getId()));
            return intent;
        } else {
            return null;
        }
    }

    private Intent getDownloadIntent(Context context, String url, String destination, String title) {
        return ServiceIntentHelper.getAudioDownloadIntent(context, url, destination, title);
    }

    private AudioPathInfo getLocalAudioPathInfo(QariItem qari) {
        if (pagerActivity != null) {
            String localPath = audioUtil.getLocalQariUrl(qari);
            if (localPath != null) {
                String databasePath = audioUtil.getQariDatabasePathIfGapless(qari);
                String urlFormat;
                if (databasePath == null) {
                    urlFormat = localPath + File.separator + "%d" + File.separator + "%d" + AudioUtils.AUDIO_EXTENSION;
                } else {
                    urlFormat = localPath + File.separator + "%03d" + AudioUtils.AUDIO_EXTENSION;
                }
                return new AudioPathInfo(urlFormat, localPath, databasePath);
            }
        }
        return null;
    }

    private boolean haveAllFiles(AudioPathInfo audioPathInfo, SuraAyah start, SuraAyah end) {
        return audioUtil.haveAllFiles(audioPathInfo.getUrlFormat(), audioPathInfo.getLocalDirectory(), start, end, audioPathInfo.getGaplessDatabase() != null);
    }

    private String getGaplessDatabaseUrl(QariItem qari) {
        if (!qari.isGapless() || qari.getDatabaseName() == null) {
            return null;
        }

        String dbName = qari.getDatabaseName() + AudioUtils.ZIP_EXTENSION;
        return quranFileUtils.getGaplessDatabaseRootUrl() + "/" + dbName;
    }

    @Override
    public void bind(PagerActivity what) {
        pagerActivity = what;
    }

    @Override
    public void unbind(PagerActivity what) {
        if (pagerActivity == what) {
            pagerActivity = null;
        }
    }
}
