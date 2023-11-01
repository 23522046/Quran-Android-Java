package com.quran.labs.androidquran.service.util;

import android.content.Context;
import android.content.Intent;

import com.quran.data.core.QuranFileManager;
import com.quran.data.core.QuranInfo;
import com.quran.data.di.AppScope;
import com.quran.data.model.SuraAyah;
import com.quran.data.model.audio.Qari;
import com.quran.labs.androidquran.common.audio.model.AudioDownloadMetadata;
import com.quran.labs.androidquran.service.QuranDownloadService;
import com.quran.labs.androidquran.util.AudioUtils;
import com.quran.mobile.common.download.Downloader;
import com.squareup.anvil.annotations.ContributesBinding;

import javax.inject.Inject;

@ContributesBinding(AppScope.class)
public class DownloadStarter implements Downloader {
    private final Context context;
    private final QuranInfo quranInfo;
    private final QuranFileManager fileManager;
    private final AudioUtils audioUtils;

    @Inject
    public DownloadStarter(
            Context context,
            QuranInfo quranInfo,
            QuranFileManager fileManager,
            AudioUtils audioUtils
    ) {
        this.context = context;
        this.quranInfo = quranInfo;
        this.fileManager = fileManager;
        this.audioUtils = audioUtils;
    }

    @Override
    public void downloadSura(Qari qari, int sura) {
        downloadSuras(qari, sura, sura);
    }

    @Override
    public void downloadSuras(Qari qari, int startSura, int endSura) {
        String basePath = fileManager.audioFileDirectory();
        String baseUri = basePath + qari.getPath();
        boolean isGapless = qari.isGapless();
        String sheikhName = context.getString(qari.getNameResource());

        Intent intent = ServiceIntentHelper.getDownloadIntent(
                context,
                audioUtils.getQariUrl(qari),
                baseUri,
                sheikhName,
                AUDIO_DOWNLOAD_KEY + qari.getId() + startSura,
                QuranDownloadService.DOWNLOAD_TYPE_AUDIO
        );

        intent.putExtra(QuranDownloadService.EXTRA_START_VERSE, new SuraAyah(startSura, 1));
        intent.putExtra(QuranDownloadService.EXTRA_END_VERSE, new SuraAyah(endSura, quranInfo.getNumberOfAyahs(endSura)));
        intent.putExtra(QuranDownloadService.EXTRA_IS_GAPLESS, isGapless);
        intent.putExtra(QuranDownloadService.EXTRA_METADATA, new AudioDownloadMetadata(qari.getId()));

        context.startService(intent);
    }

    @Override
    public void cancelDownloads() {
        Intent intent = new Intent(context, QuranDownloadService.class);
        intent.setAction(QuranDownloadService.ACTION_CANCEL_DOWNLOADS);
        context.startService(intent);
    }

    private static final String AUDIO_DOWNLOAD_KEY = "AudioDownload.DownloadKey.";
}
