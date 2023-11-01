package com.quran.labs.androidquran.worker;

import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.work.CoroutineWorker;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import com.quran.labs.androidquran.R;
import com.quran.labs.androidquran.core.worker.WorkerTaskFactory;
import com.quran.labs.androidquran.data.Constants;
import com.quran.labs.androidquran.database.AudioDatabaseVersionChecker;
import com.quran.labs.androidquran.database.SuraTimingDatabaseHandler;
import com.quran.labs.androidquran.feature.audio.AudioUpdater;
import com.quran.labs.androidquran.feature.audio.api.AudioUpdateService;
import com.quran.labs.androidquran.feature.audio.util.AudioFileCheckerImpl;
import com.quran.labs.androidquran.feature.audio.util.MD5Calculator;
import com.quran.labs.androidquran.util.AudioUtils;
import com.quran.labs.androidquran.util.NotificationChannelUtil;
import com.quran.labs.androidquran.util.QuranFileUtils;
import com.quran.labs.androidquran.util.QuranSettings;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import kotlinx.coroutines.coroutineScope;
import timber.log.Timber;

public class AudioUpdateWorker extends CoroutineWorker {

    private final Context context;
    private final AudioUpdateService audioUpdateService;
    private final AudioUtils audioUtils;
    private final QuranFileUtils quranFileUtils;
    private final QuranSettings quranSettings;

    @Inject
    public AudioUpdateWorker(
            Context context,
            WorkerParameters params,
            AudioUpdateService audioUpdateService,
            AudioUtils audioUtils,
            QuranFileUtils quranFileUtils,
            QuranSettings quranSettings
    ) {
        super(context, params);
        this.context = context;
        this.audioUpdateService = audioUpdateService;
        this.audioUtils = audioUtils;
        this.quranFileUtils = quranFileUtils;
        this.quranSettings = quranSettings;
    }

    @Override
    public ListenableWorker.Result doWork() {
        return coroutineScope(() -> {
            String audioPathRoot = quranFileUtils.getQuranAudioDirectory(context);
            if (audioPathRoot != null) {
                int currentVersion = quranSettings.getCurrentAudioRevision();
                AudioUpdateService.AudioUpdates updates = audioUpdateService.getUpdates(currentVersion);

                Timber.d("local version: %d - server version: %d",
                        currentVersion, updates.currentRevision);
                if (currentVersion != updates.currentRevision) {
                    List<AudioUpdater.LocalUpdate> localFilesToDelete = AudioUpdater.computeUpdates(
                            updates.updates,
                            audioUtils.getQariList(context),
                            new AudioFileCheckerImpl(MD5Calculator.INSTANCE, audioPathRoot),
                            new AudioDatabaseVersionChecker()
                    );

                    Timber.d("update count: %d", localFilesToDelete.size);
                    if (!localFilesToDelete.isEmpty()) {
                        for (AudioUpdater.LocalUpdate localUpdate : localFilesToDelete) {
                            if (localUpdate.needsDatabaseUpgrade) {
                                // delete the database
                                String dbPath = audioUtils.getQariDatabasePathIfGapless(localUpdate.qari);
                                if (dbPath != null) {
                                    SuraTimingDatabaseHandler.clearDatabaseHandlerIfExists(dbPath);
                                }
                                Timber.d("would remove %s", dbPath);
                                new File(dbPath).delete();
                            }

                            String qari = localUpdate.qari;
                            String path = audioUtils.getLocalQariUrl(qari);
                            for (String file : localUpdate.files) {
                                // delete the file
                                String filePath = qari.isGapless ?
                                        path + File.separator + file :
                                        path + File.separator + file.substring(0, 3) +
                                                File.separator + file.substring(3, 6) + ".mp3";
                                Timber.d("would remove %s", filePath);
                                new File(filePath).delete();
                            }
                        }

                        // push a notification to inform the person that some files
                        // have been deleted.
                        sendNotification(context);
                    }
                    Timber.d("updating audio to revision: %d", updates.currentRevision);
                    quranSettings.setCurrentAudioRevision(updates.currentRevision);
                }
            }
            return ListenableWorker.Result.success();
        });
    }

    private void sendNotification(Context context) {
        int notificationColor = ContextCompat.getColor(context, R.color.notification_color);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context.getApplicationContext(), Constants.DOWNLOAD_CHANNEL)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setColor(notificationColor)
                        .setContentTitle(context.getString(R.string.audio_updated_title))
                        .setContentText(context.getString(R.string.audio_updated_text))
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(context.getString(R.string.audio_updated_text)))
                        .build();

        NotificationManager notificationManager =
                (NotificationManager) context.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || notificationManager.areNotificationsEnabled()) {
            NotificationChannelUtil.setupNotificationChannel(
                    notificationManager,
                    Constants.DOWNLOAD_CHANNEL,
                    context.getString(R.string.notification_channel_download)
            );
            notificationManager.notify(Constants.NOTIFICATION_ID_AUDIO_UPDATE, notificationBuilder);
        }
    }

    public static class Factory implements WorkerTaskFactory {

        private final AudioUpdateService audioUpdateService;
        private final AudioUtils audioUtils;
        private final QuranFileUtils quranFileUtils;
        private final QuranSettings quranSettings;

        @Inject
        public Factory(
                AudioUpdateService audioUpdateService,
                AudioUtils audioUtils,
                QuranFileUtils quranFileUtils,
                QuranSettings quranSettings
        ) {
            this.audioUpdateService = audioUpdateService;
            this.audioUtils = audioUtils;
            this.quranFileUtils = quranFileUtils;
            this.quranSettings = quranSettings;
        }

        @Override
        public ListenableWorker makeWorker(Context appContext, WorkerParameters workerParameters) {
            return new AudioUpdateWorker(
                    appContext, workerParameters, audioUpdateService, audioUtils, quranFileUtils,
                    quranSettings
            );
        }
    }
}
