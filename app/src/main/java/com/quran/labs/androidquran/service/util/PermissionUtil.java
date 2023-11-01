package com.quran.labs.androidquran.service.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.quran.labs.androidquran.R;
import com.quran.labs.androidquran.util.QuranSettings;

public class PermissionUtil {

    public static boolean haveWriteExternalStoragePermission(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                        PackageManager.PERMISSION_GRANTED;
    }

    public static boolean canRequestWriteExternalStoragePermission(Activity activity) {
        return !QuranSettings.getInstance(activity).didPresentSdcardPermissionsDialog() ||
                ActivityCompat.shouldShowRequestPermissionRationale(
                        activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    public static boolean havePostNotificationPermission(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED;
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    public static boolean canRequestPostNotificationPermission(Activity activity) {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS);
    }

    public static AlertDialog buildPostPermissionDialog(
            Context context,
            final Runnable onAccept,
            final Runnable onDecline
    ) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(R.string.post_notification_permission)
                .setPositiveButton(R.string.downloadPrompt_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        onAccept.run();
                    }
                })
                .setNegativeButton(R.string.downloadPrompt_no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        onDecline.run();
                    }
                });
        return builder.create();
    }
}
