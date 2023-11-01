package com.quran.labs.androidquran.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class NotificationChannelUtil {

    public static void setupNotificationChannel(NotificationManager notificationManager, String channelId, String channelName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW);
            if (notificationManager.getNotificationChannel(channelId) == null) {
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }
    }
}
