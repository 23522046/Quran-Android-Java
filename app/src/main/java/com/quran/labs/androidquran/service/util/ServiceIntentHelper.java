package com.quran.labs.androidquran.service.util;

import android.content.Context;
import android.content.Intent;
import com.quran.labs.androidquran.R;
import com.quran.labs.androidquran.service.QuranDownloadService;
import com.quran.labs.androidquran.service.download.QuranDownloadNotifier;

public class ServiceIntentHelper {
    private static final String AUDIO_DOWNLOAD_KEY = "AUDIO_DOWNLOAD_KEY";

    public static Intent getAudioDownloadIntent(
            Context context,
            String url,
            String destination,
            String notificationTitle
    ) {
        return getDownloadIntent(
                context, url, destination, notificationTitle,
                AUDIO_DOWNLOAD_KEY, QuranDownloadService.DOWNLOAD_TYPE_AUDIO
        );
    }

    public static Intent getDownloadIntent(
            Context context,
            String url,
            String destination,
            String notificationTitle,
            String key,
            int type
    ) {
        Intent intent = new Intent(context, QuranDownloadService.class);
        intent.putExtra(QuranDownloadService.EXTRA_URL, url);
        intent.putExtra(QuranDownloadService.EXTRA_DESTINATION, destination);
        intent.putExtra(QuranDownloadService.EXTRA_NOTIFICATION_NAME, notificationTitle);
        intent.putExtra(QuranDownloadService.EXTRA_DOWNLOAD_KEY, key);
        intent.putExtra(QuranDownloadService.EXTRA_DOWNLOAD_TYPE, type);
        intent.setAction(QuranDownloadService.ACTION_DOWNLOAD_URL);
        return intent;
    }

    public static int getErrorResourceFromDownloadIntent(
            Intent intent,
            boolean willRetry
    ) {
        int errorCode = intent.getIntExtra(QuranDownloadNotifier.ProgressIntent.ERROR_CODE, 0);
        return getErrorResourceFromErrorCode(errorCode, willRetry);
    }

    public static int getErrorResourceFromErrorCode(
            int errorCode,
            boolean willRetry
    ) {
        switch (errorCode) {
            case QuranDownloadNotifier.ERROR_DISK_SPACE:
                return R.string.download_error_disk;
            case QuranDownloadNotifier.ERROR_NETWORK:
                return willRetry ? R.string.download_error_network_retry : R.string.download_error_network;
            case QuranDownloadNotifier.ERROR_PERMISSIONS:
                return R.string.download_error_perms;
            case QuranDownloadNotifier.ERROR_INVALID_DOWNLOAD:
                return willRetry ? R.string.download_error_invalid_download_retry : R.string.download_error_invalid_download;
            case QuranDownloadNotifier.ERROR_CANCELLED:
                return R.string.notification_download_canceled;
            case QuranDownloadNotifier.ERROR_GENERAL:
            default:
                return R.string.download_error_general;
        }
    }
}
