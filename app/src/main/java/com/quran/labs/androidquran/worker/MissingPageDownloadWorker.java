package com.quran.labs.androidquran.worker;

import android.content.Context;

import androidx.work.CoroutineWorker;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import com.quran.data.core.QuranInfo;
import com.quran.labs.androidquran.core.worker.WorkerTaskFactory;
import com.quran.labs.androidquran.util.QuranFileUtils;
import com.quran.labs.androidquran.util.QuranScreenInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.coroutineScope;
import kotlinx.coroutines.flow.asFlow;
import kotlinx.coroutines.flow.flowOn;
import kotlinx.coroutines.flow.map;
import kotlinx.coroutines.flow.toList;
import okhttp3.OkHttpClient;
import timber.log.Timber;

public class MissingPageDownloadWorker extends CoroutineWorker {

    private final Context context;
    private final OkHttpClient okHttpClient;
    private final QuranInfo quranInfo;
    private final QuranScreenInfo quranScreenInfo;
    private final QuranFileUtils quranFileUtils;

    @Inject
    public MissingPageDownloadWorker(
            Context context,
            WorkerParameters params,
            OkHttpClient okHttpClient,
            QuranInfo quranInfo,
            QuranScreenInfo quranScreenInfo,
            QuranFileUtils quranFileUtils
    ) {
        super(context, params);
        this.context = context;
        this.okHttpClient = okHttpClient;
        this.quranInfo = quranInfo;
        this.quranScreenInfo = quranScreenInfo;
        this.quranFileUtils = quranFileUtils;
    }

    @Override
    public Result doWork() {
        return coroutineScope(() -> {
            Timber.d("MissingPageDownloadWorker");
            List<PageToDownload> pagesToDownload = findMissingPagesToDownload();
            Timber.d("MissingPageDownloadWorker found %d missing pages", pagesToDownload.size);
            if (pagesToDownload.size() < MISSING_PAGE_LIMIT) {
                // attempt to download missing pages
                List<Boolean> results = pagesToDownload.stream()
                        .map(this::downloadPage)
                        .collect(toList());

                long failures = results.stream().filter(result -> !result).count();
                if (failures > 0) {
                    Timber.d("MissingPageWorker failed with %d from %d", failures, pagesToDownload.size());
                } else {
                    Timber.d("MissingPageWorker success with %d", pagesToDownload.size());
                }
            }
            return Result.success();
        });
    }

    private List<PageToDownload> findMissingPagesToDownload() {
        String width = quranScreenInfo.getWidthParam();
        List<PageToDownload> result = findMissingPagesForWidth(width);

        String tabletWidth = quranScreenInfo.getTabletWidthParam();
        if (width.equals(tabletWidth)) {
            return result;
        } else {
            result.addAll(findMissingPagesForWidth(tabletWidth));
            return result;
        }
    }

    private List<PageToDownload> findMissingPagesForWidth(String width) {
        List<PageToDownload> result = new ArrayList<>();
        File pagesDirectory = new File(quranFileUtils.getQuranImagesDirectory(context, width));
        for (int page = 1; page <= quranInfo.getNumberOfPages(); page++) {
            String pageFile = QuranFileUtils.getPageFileName(page);
            if (!new File(pagesDirectory, pageFile).exists()) {
                result.add(new PageToDownload(width, page));
            }
        }
        return result;
    }

    private boolean downloadPage(PageToDownload pageToDownload) {
        Timber.d("downloading %d for %s - thread: %s",
                pageToDownload.page, pageToDownload.width, Thread.currentThread().getName());
        String pageName = QuranFileUtils.getPageFileName(pageToDownload.page);

        try {
            return quranFileUtils.getImageFromWeb(okHttpClient, context, pageToDownload.width, pageName)
                    .isSuccessful();
        } catch (Throwable throwable) {
            return false;
        }
    }

    public static class Factory implements WorkerTaskFactory {

        private final QuranInfo quranInfo;
        private final QuranFileUtils quranFileUtils;
        private final QuranScreenInfo quranScreenInfo;
        private final OkHttpClient okHttpClient;

        @Inject
        public Factory(
                QuranInfo quranInfo,
                QuranFileUtils quranFileUtils,
                QuranScreenInfo quranScreenInfo,
                OkHttpClient okHttpClient
        ) {
            this.quranInfo = quranInfo;
            this.quranFileUtils = quranFileUtils;
            this.quranScreenInfo = quranScreenInfo;
            this.okHttpClient = okHttpClient;
        }

        @Override
        public ListenableWorker makeWorker(Context appContext, WorkerParameters workerParameters) {
            return new MissingPageDownloadWorker(
                    appContext, workerParameters, okHttpClient, quranInfo, quranScreenInfo, quranFileUtils
            );
        }
    }

    private static class PageToDownload {
        private final String width;
        private final int page;

        public PageToDownload(String width, int page) {
            this.width = width;
            this.page = page;
        }
    }

    private static final int MISSING_PAGE_LIMIT = 50;
}
