package com.quran.labs.androidquran.worker;

import android.content.Context;
import androidx.work.CoroutineWorker;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import com.quran.data.core.QuranInfo;
import com.quran.labs.androidquran.core.worker.WorkerTaskFactory;
import com.quran.labs.androidquran.util.QuranFileUtils;
import com.quran.labs.androidquran.util.QuranPartialPageChecker;
import com.quran.labs.androidquran.util.QuranScreenInfo;
import com.quran.labs.androidquran.util.QuranSettings;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import kotlinx.coroutines.coroutineScope;
import timber.log.Timber;

public class PartialPageCheckingWorker extends CoroutineWorker {

    private final Context context;
    private final QuranInfo quranInfo;
    private final QuranFileUtils quranFileUtils;
    private final QuranScreenInfo quranScreenInfo;
    private final QuranSettings quranSettings;
    private final QuranPartialPageChecker quranPartialPageChecker;

    @Inject
    public PartialPageCheckingWorker(
            Context context,
            WorkerParameters params,
            QuranInfo quranInfo,
            QuranFileUtils quranFileUtils,
            QuranScreenInfo quranScreenInfo,
            QuranSettings quranSettings,
            QuranPartialPageChecker quranPartialPageChecker
    ) {
        super(context, params);
        this.context = context;
        this.quranInfo = quranInfo;
        this.quranFileUtils = quranFileUtils;
        this.quranScreenInfo = quranScreenInfo;
        this.quranSettings = quranSettings;
        this.quranPartialPageChecker = quranPartialPageChecker;
    }

    @Override
    public Result doWork() {
        return coroutineScope(() -> {
            Timber.d("PartialPageCheckingWorker");
            String requestedPageType = getInputData().getString(WorkerConstants.PAGE_TYPE);
            if (!requestedPageType.equals(quranSettings.getPageType())) {
                Timber.e(new IllegalStateException(
                        "PageType different than expected: " + requestedPageType +
                                ", found " + quranSettings.getPageType()));
                return Result.success();
            } else if (!quranSettings.didCheckPartialImages(requestedPageType)) {
                int numberOfPages = quranInfo.getNumberOfPages();

                // prepare page widths and paths
                String width = quranScreenInfo.getWidthParam();
                File pagesDirectory = quranFileUtils.getQuranImagesDirectory(context, width);
                if (pagesDirectory == null) {
                    return Result.success();
                }

                // compute the partial page sets
                List<PartialPage> partialPages =
                        quranPartialPageChecker.checkPages(pagesDirectory, numberOfPages, width);
                Timber.d("Found %d partial images for width %s", partialPages.size(), width);

                String tabletWidth = quranScreenInfo.getTabletWidthParam();
                List<PartialPage> tabletPartialPages = new ArrayList<>();
                if (!width.equals(tabletWidth)) {
                    File tabletPagesDirectory = quranFileUtils.getQuranImagesDirectory(context, tabletWidth);
                    if (tabletPagesDirectory != null) {
                        tabletPartialPages = quranPartialPageChecker.checkPages(tabletPagesDirectory, numberOfPages, tabletWidth);
                    }
                }
                Timber.d("Found %d partial images for tablet width %s", tabletPartialPages.size(), tabletWidth);

                List<PartialPage> allPartialPages = new ArrayList<>(partialPages);
                allPartialPages.addAll(tabletPartialPages);
                if (allPartialPages.size() > PARTIAL_PAGE_LIMIT) {
                    Timber.e(new IllegalStateException("Too many partial pages found"),
                            "found " + allPartialPages.size() + " partial images");
                    // still delete the partial images just because ¯\_(ツ)_/¯
                }

                boolean deletionSucceeded = true;
                try {
                    // iterate through each one and delete the partial pages
                    for (PartialPage partialPage : allPartialPages) {
                        String path = (partialPage.width.equals(width)) ? pagesDirectory.getPath() : tabletPagesDirectory.getPath();
                        if (!deletePage(path, partialPage.page)) {
                            deletionSucceeded = false;
                            break;
                        }
                    }
                } catch (IOException ioException) {
                    Timber.e(ioException);
                    deletionSucceeded = false;
                }

                if (!deletionSucceeded) {
                    Timber.d("PartialPageCheckingWorker - partial deletion failure, retrying..");
                    return Result.retry();
                } else {
                    Timber.d("PartialPageCheckingWorker - partial success!");
                    quranSettings.setCheckedPartialImages(requestedPageType);
                    return Result.success();
                }
            } else {
                return Result.success();
            }
        });
    }

    private boolean deletePage(String directory, int page) {
        String pageName = QuranFileUtils.getPageFileName(page);
        File file = new File(directory, pageName);
        if (file.exists()) {
            return file.delete();
        } else {
            return true;
        }
    }

    public static class Factory implements WorkerTaskFactory {

        private final QuranInfo quranInfo;
        private final QuranFileUtils quranFileUtils;
        private final QuranScreenInfo quranScreenInfo;
        private final QuranSettings quranSettings;
        private final QuranPartialPageChecker quranPartialPageChecker;

        @Inject
        public Factory(
                QuranInfo quranInfo,
                QuranFileUtils quranFileUtils,
                QuranScreenInfo quranScreenInfo,
                QuranSettings quranSettings,
                QuranPartialPageChecker quranPartialPageChecker
        ) {
            this.quranInfo = quranInfo;
            this.quranFileUtils = quranFileUtils;
            this.quranScreenInfo = quranScreenInfo;
            this.quranSettings = quranSettings;
            this.quranPartialPageChecker = quranPartialPageChecker;
        }

        @Override
        public ListenableWorker makeWorker(Context appContext, WorkerParameters workerParameters) {
            return new PartialPageCheckingWorker(
                    appContext, workerParameters, quranInfo, quranFileUtils, quranScreenInfo, quranSettings,
                    quranPartialPageChecker
            );
        }
    }

    private static class PartialPage {
        private final String width;
        private final int page;

        public PartialPage(String width, int page) {
            this.width = width;
            this.page = page;
        }
    }

    private static final int PARTIAL_PAGE_LIMIT = 50;
}
