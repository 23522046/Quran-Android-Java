import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.WorkManager;
import com.example.quranapp.R;
import com.example.quranapp.util.PermissionUtil;
import com.example.quranapp.util.ServiceIntentHelper;
import com.example.quranapp.util.ToastCompat;
import com.example.quranapp.util.WorkerConstants;
import timber.log.Timber;

import java.io.File;
import java.io.IOException;

public class QuranDataActivity extends AppCompatActivity {

    private static final int REQUEST_WRITE_TO_SDCARD_PERMISSIONS = 1;
    private static final int REQUEST_POST_NOTIFICATION_PERMISSIONS = 2;
    private static final String QURAN_DIRECTORY_MARKER_FILE = "q4a";
    private static final String QURAN_HIDDEN_DIRECTORY_MARKER_FILE = ".q4a";

    private int lastForceValue = 0;
    private AlertDialog permissionsDialog;
    private AlertDialog errorDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quran_data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_WRITE_TO_SDCARD_PERMISSIONS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (!canWriteSdcardAfterPermissions()) {
                    ToastCompat.makeText(
                            this,
                            R.string.storage_permission_please_restart, Toast.LENGTH_LONG
                    ).show();
                }
                checkPages();
            } else {
                File fallbackFile = getExternalFilesDir(null);
                if (fallbackFile != null) {
                    quranSettings.appCustomLocation = fallbackFile.getAbsolutePath();
                    checkPages();
                } else {
                    quranSettings.appCustomLocation = null;
                    runListViewWithoutPages();
                }
            }
        } else if (requestCode == REQUEST_POST_NOTIFICATION_PERMISSIONS) {
            actuallyDownloadQuranImages(lastForceValue);
        }
    }

    private boolean canWriteSdcardAfterPermissions() {
        String location = quranFileUtils.getQuranBaseDirectory(this);
        if (location != null) {
            try {
                if (new File(location).exists() || quranFileUtils.makeQuranDirectory(this, quranScreenInfo.widthParam)) {
                    File f = new File(location, "" + System.currentTimeMillis());
                    if (f.createNewFile()) {
                        f.delete();
                        return true;
                    }
                }
            } catch (Exception e) {
                // no op
            }
        }
        return false;
    }

    public void handleDownloadSuccess() {
        if (quranDataStatus != null && !quranDataStatus.havePages()) {
            quranSettings.setCheckedPartialImages(quranSettings.pageType);
            WorkManager.getInstance(applicationContext).cancelUniqueWork(WorkerConstants.CLEANUP_PREFIX + pageType);
        }
        quranSettings.removeShouldFetchPages();
        runListView();
    }

    public void handleDownloadFailure(int errId) {
        if (errorDialog != null && errorDialog.isShowing()) {
            return;
        }
        showFatalErrorDialog(errId);
    }

    private void showFatalErrorDialog(int errorId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(errorId);
        builder.setCancelable(false);
        builder.setPositiveButton(
                R.string.download_retry,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        errorDialog = null;
                        removeErrorPreferences();
                        downloadQuranImages(true);
                    }
                }
        );
        builder.setNegativeButton(
                R.string.download_cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        errorDialog = null;
                        removeErrorPreferences();
                        quranSettings.setShouldFetchPages(false);
                        runListViewWithoutPages();
                    }
                }
        );
        errorDialog = builder.create();
        errorDialog.show();
    }

    private void removeErrorPreferences() {
        quranSettings.clearLastDownloadError();
    }

    public void onStorageNotAvailable() {
        hideMigrationDialog();
        runListViewWithoutPages();
    }

    public void onPagesChecked(QuranDataStatus quranDataStatus) {
        hideMigrationDialog();
        this.quranDataStatus = quranDataStatus;
        if (!quranDataStatus.havePages()) {
            if (quranSettings.didDownloadPages()) {
                try {
                    onPagesLost();
                } catch (Exception e) {
                    Timber.e(e);
                }
                String path = quranSettings.appCustomLocation;
                String internalDirectory = getFilesDir().getAbsolutePath();
                if (!TextUtils.isEmpty(path) && !path.equals(internalDirectory)) {
                    quranSettings.appCustomLocation = internalDirectory;
                }
                quranSettings.removeDidDownloadPages();
            }
            String lastErrorItem = quranSettings.lastDownloadItemWithError;
            Timber.d("checkPages: need to download pages... lastError: %s", lastErrorItem);
            switch (lastErrorItem) {
                case PAGES_DOWNLOAD_KEY:
                    int lastError = quranSettings.lastDownloadErrorCode;
                    int errorId = ServiceIntentHelper.getErrorResourceFromErrorCode(lastError, false);
                    showFatalErrorDialog(errorId);
                    break;
                default:
                    if (quranSettings.shouldFetchPages()) {
                        downloadQuranImages(false);
                    } else {
                        promptForDownload();
                    }
                    break;
            }
        } else {
            String appLocation = quranSettings.appCustomLocation;
            String baseDirectory = quranFileUtils.quranBaseDirectory;
            try {
                new File(baseDirectory, QURAN_DIRECTORY_MARKER_FILE).createNewFile();
                new File(baseDirectory, QURAN_HIDDEN_DIRECTORY_MARKER_FILE).createNewFile();
                new File(noBackupFilesDir, QURAN_HIDDEN_DIRECTORY_MARKER_FILE).createNewFile();
                quranSettings.setDownloadedPages(
                        System.currentTimeMillis(), appLocation,
                        quranDataStatus.portraitWidth + "_" + quranDataStatus.landscapeWidth
                );
            } catch (IOException ioe) {
                Timber.e(ioe);
            }
            String patchParam = quranDataStatus.patchParam;
            if (!TextUtils.isEmpty(patchParam)) {
                Timber.d("checkPages: have pages, but need patch %s", patchParam);
                promptForDownload();
            } else {
                runListView();
            }
        }
    }

    private void onPagesLost() {
        String appLocation = quranSettings.appCustomLocation;
        File appDir = getExternalFilesDir(null);
        File sdcard = Environment.getExternalStorageDirectory();
        String lastDownloadedPagePath = quranSettings.previouslyDownloadedPath;
        boolean isPagePathTheSame = appLocation.equals(lastDownloadedPagePath);
        String lastDownloadedPages = quranSettings.previouslyDownloadedPageTypes;
        String currentPagesToDownload = quranDataStatus.portraitWidth + "_" + quranDataStatus.landscapeWidth;
        boolean arePageSetsEquivalent = lastDownloadedPages.equals(currentPagesToDownload);
        boolean didHiddenFileSurvive = false;
        String baseDirectory = quranFileUtils.quranBaseDirectory;
        if (baseDirectory != null) {
            try {
                didHiddenFileSurvive = new File(baseDirectory, QURAN_HIDDEN_DIRECTORY_MARKER_FILE).exists();
            } catch (Exception e) {
                Timber.e(e);
            }
        }
        boolean didNormalFileSurvive = false;
        if (baseDirectory != null) {
            try {
                didNormalFileSurvive = new File(baseDirectory, QURAN_DIRECTORY_MARKER_FILE).exists();
            } catch (Exception e) {
                Timber.e(e);
            }
        }
        boolean didInternalFileSurvive = false;
        try {
            didInternalFileSurvive = new File(noBackupFilesDir, QURAN_HIDDEN_DIRECTORY_MARKER_FILE).exists();
        } catch (Exception e) {
            Timber.e(e);
        }
        long downloadTime = quranSettings.previouslyDownloadedTime;
        String recencyOfRemoval = "";
        if (downloadTime != 0) {
            long deltaInSeconds = (System.currentTimeMillis() - downloadTime) / 1000;
            if (deltaInSeconds < 5 * 60) {
                recencyOfRemoval = "within 5 minutes";
            } else if (deltaInSeconds < 10 * 60) {
                recencyOfRemoval = "within 10 minutes";
            } else if (deltaInSeconds < 60 * 60) {
                recencyOfRemoval = "within an hour";
            } else if (deltaInSeconds < 24 * 60 * 60) {
                recencyOfRemoval = "within a day";
            } else {
                recencyOfRemoval = "more than a day";
            }
        }
        Timber.w(quranDataStatus.toString());
        Timber.w(quranDataPresenter.getDebugLog());
        Timber.w("appLocation: %s", appLocation);
        Timber.w("sdcard: %s, app dir: %s", sdcard, appDir);
        Timber.w("didNormalFileSurvive: %s", didNormalFileSurvive);
        Timber.w("didInternalFileSurvive: %s", didInternalFileSurvive);
        Timber.w("didHiddenFileSurvive: %s", didHiddenFileSurvive);
        Timber.w("seconds passed: %d, recency: %s", (System.currentTimeMillis() - downloadTime) / 1000, recencyOfRemoval);
        Timber.w("isPagePathTheSame: %s", isPagePathTheSame);
        Timber.w("arePagesToDownloadTheSame: %s", arePageSetsEquivalent);
        Timber.e(new IllegalStateException("Deleted Data"), "Unable to Download Pages");
        if (appLocation.contains("com.quran")) {
            String internalDir = getFilesDir().getAbsolutePath();
            boolean isInternal = appLocation.equals(internalDir);
            File[] externalDirs = ContextCompat.getExternalFilesDirs(this, null);
            boolean isExternal = !isInternal && false;
            for (File dir : externalDirs) {
                if (dir != null && dir.getAbsolutePath().equals(appLocation)) {
                    isExternal = true;
                    break;
                }
            }
            if (!isInternal && !isExternal) {
                Timber.w("appLocation: %s", appLocation);
                Timber.w("internal: %s", internalDir);
                for (File dir : externalDirs) {
                    Timber.w("external: %s", dir);
                }
                Timber.e(new IllegalStateException("data deleted from unknown directory"));
            }
        }
    }

    private void downloadQuranImages(boolean force) {
        if (PermissionUtil.havePostNotificationPermission(this)) {
            actuallyDownloadQuranImages(force);
        } else if (PermissionUtil.canRequestPostNotificationPermission(this)) {
            AlertDialog dialog = PermissionUtil.buildPostPermissionDialog(
                    this,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            lastForceValue = force;
                            permissionsDialog = null;
                            requestPostNotificationPermission();
                        }
                    },
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            permissionsDialog = null;
                            actuallyDownloadQuranImages(force);
                        }
                    }
            );
            permissionsDialog = dialog;
            dialog.show();
        } else {
            lastForceValue = force;
            requestPostNotificationPermission();
        }
    }

    private void actuallyDownloadQuranImages(boolean force) {
        if (downloadReceiver != null && downloadReceiver.didReceiveBroadcast() && !force) {
            return;
        }
        QuranDataStatus dataStatus = quranDataStatus;
        String url;
        if (dataStatus.needPortrait() && !dataStatus.needLandscape()) {
            url = quranFileUtils.zipFileUrl;
        } else if (dataStatus.needLandscape() && !dataStatus.needPortrait()) {
            url = quranFileUtils.getZipFileUrl(quranScreenInfo.tabletWidthParam);
        } else {
            if (quranScreenInfo.tabletWidthParam == quranScreenInfo.widthParam) {
                url = quranFileUtils.zipFileUrl;
            } else {
                int widthParam = quranScreenInfo.widthParam + quranScreenInfo.tabletWidthParam;
                url = quranFileUtils.getZipFileUrl(widthParam);
            }
        }
        String patchParam = dataStatus.patchParam;
        if (!TextUtils.isEmpty(patchParam)) {
            url = quranFileUtils.getPatchFileUrl(patchParam, quranDataPresenter.imagesVersion());
        }
        String destination = quranFileUtils.getQuranImagesBaseDirectory(this);
        Intent intent = ServiceIntentHelper.getDownloadIntent(
                this, url,
                destination, getString(R.string.app_name), PAGES_DOWNLOAD_KEY,
                QuranDownloadService.DOWNLOAD_TYPE_PAGES
        );
        if (!force) {
            intent.putExtra(QuranDownloadService.EXTRA_REPEAT_LAST_ERROR, true);
        }
        startService(intent);
    }

    private void promptForDownload() {
        QuranDataStatus dataStatus = quranDataStatus;
        int message = R.string.downloadPrompt;
        if (quranScreenInfo.isDualPageMode && dataStatus.needLandscape()) {
            message = R.string.downloadTabletPrompt;
        }
        if (!TextUtils.isEmpty(dataStatus.patchParam)) {
            message = R.string.downloadImportantPrompt;
        }
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setMessage(message);
        dialog.setCancelable(false);
        dialog.setPositiveButton(
                R.string.downloadPrompt_ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        promptForDownloadDialog = null;
                        quranSettings.setShouldFetchPages(true);
                        downloadQuranImages(true);
                    }
                }
        );
        dialog.setNegativeButton(
                R.string.downloadPrompt_no,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        promptForDownloadDialog = null;
                        boolean isPatch = dataStatus.patchParam != null && !dataStatus.patchParam.isEmpty();
                        if (isPatch) {
                            runListView();
                        } else {
                            runListViewWithoutPages();
                        }
                    }
                }
        );
        AlertDialog promptForDownloadDialog = dialog.create();
        promptForDownloadDialog.setTitle(R.string.downloadPrompt_title);
        promptForDownloadDialog.show();
        this.promptForDownloadDialog = promptForDownloadDialog;
    }

    private void runListViewWithoutPages() {
        if (!quranDataPresenter.canProceedWithoutDownload()) {
            quranDataPresenter.fallbackToImageType();
        }
        runListView();
    }

    private void runListView() {
        Intent intent = new Intent(this, QuranActivity.class);
        intent.putExtra(QuranActivity.EXTRA_SHOW_TRANSLATION_UPGRADE, quranSettings.haveUpdatedTranslations());
        startActivity(intent);
        finish();
    }
}