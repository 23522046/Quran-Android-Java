package com.quran.labs.androidquran.presenter.data;

import android.content.Context;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.work.*;
import com.quran.data.core.QuranInfo;
import com.quran.data.model.QuranDataStatus;
import com.quran.data.source.PageProvider;
import com.quran.common.upgrade.LocalDataUpgrade;
import com.quran.data.source.PageContentType;
import com.quran.labs.androidquran.QuranDataActivity;
import com.quran.labs.androidquran.data.Constants;
import com.quran.labs.androidquran.presenter.Presenter;
import com.quran.labs.androidquran.util.QuranFileUtils;
import com.quran.labs.androidquran.util.QuranScreenInfo;
import com.quran.labs.androidquran.util.QuranSettings;
import com.quran.labs.androidquran.worker.AudioUpdateWorker;
import com.quran.labs.androidquran.worker.MissingPageDownloadWorker;
import com.quran.labs.androidquran.worker.PartialPageCheckingWorker;
import com.quran.labs.androidquran.worker.WorkerConstants;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;
import timber.log.Timber;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class QuranDataPresenter implements Presenter<QuranDataActivity> {

    private Context appContext;
    private QuranInfo quranInfo;
    private QuranScreenInfo quranScreenInfo;
    private PageProvider quranPageProvider;
    private QuranFileUtils quranFileUtils;
    private LocalDataUpgrade localDataUpgrade;
    private QuranSettings quranSettings;
    private QuranDataActivity activity;
    private Disposable checkPagesDisposable;
    private String cachedPageType;
    private QuranDataStatus lastCachedResult;
    private String debugLog;

    public QuranDataPresenter(
            Context appContext,
            QuranInfo quranInfo,
            QuranScreenInfo quranScreenInfo,
            PageProvider quranPageProvider,
            QuranFileUtils quranFileUtils,
            LocalDataUpgrade localDataUpgrade
    ) {
        this.appContext = appContext;
        this.quranInfo = quranInfo;
        this.quranScreenInfo = quranScreenInfo;
        this.quranPageProvider = quranPageProvider;
        this.quranFileUtils = quranFileUtils;
        this.localDataUpgrade = localDataUpgrade;
        this.quranSettings = QuranSettings.getInstance(appContext);
    }

    @UiThread
    public void checkPages() {
        QuranDataStatus lastCachedResult = this.lastCachedResult;
        if (quranFileUtils.getQuranBaseDirectory(appContext) == null) {
            activity.onStorageNotAvailable();
        } else if (lastCachedResult != null && cachedPageType.equals(quranSettings.getPageType())) {
            activity.onPagesChecked(lastCachedResult);
        } else if (checkPagesDisposable == null) {
            int pages = quranInfo.getNumberOfPages();
            String pageType = quranSettings.getPageType();
            checkPagesDisposable = supportLegacyPages(pages)
                    .andThen(actuallyCheckPages(pages))
                    .flatMap((Function<QuranDataStatus, SingleSource<QuranDataStatus>>) localDataUpgrade::processData)
                    .map(this::checkPatchStatus)
                    .flatMap((Function<QuranDataStatus, SingleSource<QuranDataStatus>>) localDataUpgrade::processPatch)
                    .doOnSuccess(it -> {
                        if (!it.havePages()) {
                            try {
                                generateDebugLog();
                            } catch (Exception e) {
                                Timber.e(e);
                            }
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(it -> {
                        if (it.havePages() && it.getPatchParam() == null) {
                            cachedPageType = pageType;
                            lastCachedResult = it;
                        }
                        activity.onPagesChecked(it);
                        checkPagesDisposable = null;
                    });
            scheduleAudioUpdater();
        }
    }

    private void scheduleAudioUpdater() {
        Constraints audioUpdaterTaskConstraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest updateAudioTask = new PeriodicWorkRequest.Builder<>(
                AudioUpdateWorker.class,
                7, TimeUnit.DAYS)
                .setConstraints(audioUpdaterTaskConstraints)
                .build();

        WorkManager.getInstance(appContext)
                .enqueueUniquePeriodicWork(
                        Constants.AUDIO_UPDATE_UNIQUE_WORK,
                        ExistingPeriodicWorkPolicy.KEEP,
                        updateAudioTask
                );
    }

    public int imagesVersion() {
        return quranPageProvider.getImageVersion();
    }

    public boolean canProceedWithoutDownload() {
        return quranPageProvider.getPageContentType() == PageContentType.Image;
    }

    public void fallbackToImageType() {
        String fallbackType = quranPageProvider.getFallbackPageType();
        if (fallbackType != null) {
            quranSettings.setPageType(fallbackType);
        }
    }

    public String getDebugLog() {
        return debugLog != null ? debugLog : "";
    }

    private void generateDebugLog() {
        File directory = quranFileUtils.getQuranBaseDirectory();
        if (directory != null) {
            StringBuilder log = new StringBuilder();

            String quranImagesDirectoryName = quranFileUtils.getQuranImagesBaseDirectory(appContext);
            File quranImagesDirectory = new File(quranImagesDirectoryName);
            File[] quranImagesDirectoryFiles = quranImagesDirectory.listFiles();
            if (quranImagesDirectoryFiles != null) {
                for (File file : quranImagesDirectoryFiles) {
                    if (file.getName().contains("width_")) {
                        log.append("image directory: ")
                                .append(file.getName())
                                .append(" - ");
                        File[] imageFiles = file.listFiles();
                        if (imageFiles != null) {
                            int fileCount = imageFiles.length;
                            log.append(fileCount);
                            if (fileCount == 1) {
                                log.append(" [")
                                        .append(imageFiles[0].getName())
                                        .append("]");
                            }
                        }
                        log.append("\n");
                        if (imageFiles == null) {
                            log.append("null image file list, ")
                                    .append(file)
                                    .append(" - ")
                                    .append(file.isDirectory());
                        }
                    }
                }
            }

            if (quranImagesDirectoryFiles == null) {
                log.append("null list of files in images directory: ")
                        .append(quranImagesDirectoryName)
                        .append(" - ")
                        .append(quranImagesDirectory.isDirectory());
            }

            String audioDirectory = quranFileUtils.getQuranAudioDirectory(appContext);
            if (audioDirectory != null) {
                log.append("audio files in audio root: ")
                        .append(new File(audioDirectory).listFiles() != null ?
                                new File(audioDirectory).listFiles().length :
                                "null");
            } else {
                log.append("audio directory is null");
            }
            debugLog = log.toString();
        }

        if (directory == null) {
            debugLog = "can't find quranBaseDirectory";
        }
    }

    private Completable supportLegacyPages(int totalPages) {
        return Completable.fromCallable(() -> {
            if (!quranSettings.haveDefaultImagesDirectory() && "madani".equals(quranSettings.getPageType())) {
                String fallback = quranFileUtils.getPotentialFallbackDirectory(appContext, totalPages);
                if (fallback != null) {
                    Timber.d("setting fallback pages to %s", fallback);
                    quranSettings.setDefaultImagesDirectory(fallback);
                } else {
                    quranSettings.setDefaultImagesDirectory("");
                }
            }

            String pageType = quranSettings.getPageType();
            if (!quranSettings.didCheckPartialImages(pageType) && !pageType.endsWith("lines")) {
                Timber.d("enqueuing work for %s...", pageType);

                Data checkTasksInputData = new Data.Builder()
                        .putString(WorkerConstants.PAGE_TYPE, pageType)
                        .build();

                OneTimeWorkRequest checkPartialPagesTask = new OneTimeWorkRequest.Builder<>(
                        PartialPageCheckingWorker.class)
                        .setInputData(checkTasksInputData)
                        .build();

                Constraints missingPageTaskConstraints = new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build();

                OneTimeWorkRequest missingPageDownloadTask = new OneTimeWorkRequest.Builder<>(
                        MissingPageDownloadWorker.class)
                        .setConstraints(missingPageTaskConstraints)
                        .build();

                WorkManager.getInstance(appContext)
                        .beginUniqueWork(
                                WorkerConstants.CLEANUP_PREFIX + pageType,
                                ExistingWorkPolicy.KEEP,
                                checkPartialPagesTask)
                        .then(missingPageDownloadTask)
                        .enqueue();
            }
        });
    }

    private Single<QuranDataStatus> actuallyCheckPages(int totalPages) {
        return Single.fromCallable(() -> {
            int width = quranScreenInfo.getWidthParam();
            boolean havePortrait = quranFileUtils.haveAllImages(appContext, width, totalPages, true);

            int tabletWidth = quranScreenInfo.getTabletWidthParam();
            boolean needLandscapeImages = quranScreenInfo.isDualPageMode() && width != tabletWidth &&
                    !quranFileUtils.haveAllImages(appContext, tabletWidth, totalPages, true);

            Timber.d("checkPages: have portrait images: %s, have landscape images: %s",
                    havePortrait ? "yes" : "no",
                    needLandscapeImages ? "yes" : "no");

            return new QuranDataStatus(width, tabletWidth, havePortrait, !needLandscapeImages, null);
        });
    }

    @WorkerThread
    private QuranDataStatus checkPatchStatus(QuranDataStatus quranDataStatus) {
        if (quranDataStatus.havePages()) {
            int latestImagesVersion = quranPageProvider.getImageVersion();

            int width = quranDataStatus.getPortraitWidth();
            boolean needPortraitPatch = !quranFileUtils.isVersion(width, latestImagesVersion);

            int tabletWidth = quranDataStatus.getLandscapeWidth();
            if (width != tabletWidth) {
                boolean needLandscapePatch = !quranFileUtils.isVersion(tabletWidth, latestImagesVersion);
                if (needLandscapePatch) {
                    return quranDataStatus.copy(patchParam = width + tabletWidth);
                }
            }

            if (needPortraitPatch) {
                return quranDataStatus.copy(patchParam = width);
            }
        }
        return quranDataStatus;
    }

    @Override
    public void bind(QuranDataActivity activity) {
        this.activity = activity;
    }

    @Override
    public void unbind(QuranDataActivity activity) {
        if (this.activity == activity) {
            this.activity = null;
        }
    }
}
