package com.quran.labs.androidquran.pageselect;

import com.quran.data.core.QuranInfo;
import com.quran.data.dao.BookmarksDao;
import com.quran.data.source.PageProvider;
import com.quran.labs.androidquran.model.bookmark.BookmarkModel;
import com.quran.labs.androidquran.presenter.Presenter;
import com.quran.labs.androidquran.util.ImageUtil;
import com.quran.labs.androidquran.util.QuranFileUtils;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import timber.log.Timber;

import java.io.File;
import java.util.Map;
import java.util.Set;

public class PageSelectPresenter implements Presenter<PageSelectActivity> {
    private final ImageUtil imageUtil;
    private final QuranFileUtils quranFileUtils;
    private final Scheduler mainThreadScheduler;
    private final BookmarksDao bookmarksDao;
    private final BookmarkModel bookmarkModel;
    private final Map<String, PageProvider> pageTypes;

    private final String baseUrl = "https://quran.app/data/pagetypes/snips";
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final Set<String> downloadingSet = new java.util.HashSet<>();
    private PageSelectActivity currentView;

    @Inject
    public PageSelectPresenter(
            ImageUtil imageUtil,
            QuranFileUtils quranFileUtils,
            Scheduler mainThreadScheduler,
            BookmarksDao bookmarksDao,
            BookmarkModel bookmarkModel,
            Map<String, PageProvider> pageTypes) {
        this.imageUtil = imageUtil;
        this.quranFileUtils = quranFileUtils;
        this.mainThreadScheduler = mainThreadScheduler;
        this.bookmarksDao = bookmarksDao;
        this.bookmarkModel = bookmarkModel;
        this.pageTypes = pageTypes;
    }

    private void generateData() {
        String base = quranFileUtils.getQuranBaseDirectory();
        if (base != null) {
            File outputPath = new File(new File(base, "pagetypes"), "snips");
            if (!outputPath.exists()) {
                outputPath.mkdirs();
                new File(outputPath, ".nomedia").createNewFile();
            }

            PageTypeItem[] data = pageTypes.entrySet().stream().map(entry -> {
                String key = entry.getKey();
                PageProvider provider = entry.getValue();
                File previewImage = new File(outputPath, key + ".png");
                File downloadedImage = previewImage.exists() ? previewImage :
                        (!downloadingSet.contains(key) ? downloadImage(provider, key, previewImage) : null);

                return new PageTypeItem(
                        key,
                        downloadedImage,
                        provider.getPreviewTitle(),
                        provider.getPreviewDescription()
                );
            }).toArray(PageTypeItem[]::new);

            currentView.onUpdatedData(data);
        }
    }

    private File downloadImage(PageProvider provider, String key, File previewImage) {
        downloadingSet.add(key);
        compositeDisposable.add(
                imageUtil.downloadImage(baseUrl + "/" + key + ".png", previewImage)
                        .onErrorResumeNext(imageUtil.downloadImage(baseUrl + "/" + key + ".png", previewImage))
                        .subscribeOn(Schedulers.io())
                        .observeOn(mainThreadScheduler)
                        .subscribe(
                                () -> generateData(),
                                e -> Timber.e(e)
                        )
        );
        return null;
    }

    public void migrateBookmarksData(String sourcePageType, String destinationPageType) {
        PageProvider source = pageTypes.get(sourcePageType);
        PageProvider destination = pageTypes.get(destinationPageType);

        if (source != null && destination != null && source.getNumberOfPages() != destination.getNumberOfPages()) {
            int[] sourcePageSuraStart = source.getSuraForPageArray();
            int[] sourcePageAyahStart = source.getAyahForPageArray();

            int sourceNumberOfPages = source.getNumberOfPages();
            int destinationNumberOfPages = destination.getNumberOfPages();
            int[] destinationPageSuraStart = destination.getSuraForPageArray();
            int[] destinationPageAyahStart = destination.getAyahForPageArray();

            for (int pageIndex = 0; pageIndex < sourceNumberOfPages; pageIndex++) {
                int sourcePage = pageIndex + 1;
                int sourcePageSura = sourcePageSuraStart[pageIndex];
                int sourcePageAyah = sourcePageAyahStart[pageIndex];

                int destinationPage;
                int destinationPageSura;
                int destinationPageAyah;

                if (sourcePageSura == 0) {
                    // Non-madani pages (e.g., Kufi pages) don't need migration as they don't
                    // follow the Hafs numbering.
                    destinationPage = sourcePage;
                    destinationPageSura = sourcePageSura;
                    destinationPageAyah = sourcePageAyah;
                } else {
                    int destinationPageIndex = destination.getPageFromSuraAyah(sourcePageSura, sourcePageAyah);
                    destinationPage = destinationPageIndex + 1;
                    destinationPageSura = destinationPageSuraStart[destinationPageIndex];
                    destinationPageAyah = destinationPageAyahStart[destinationPageIndex];
                }

                // Update bookmarks for the source page to the destination page.
                bookmarksDao.migrateBookmarksPage(sourcePage, destinationPage);
                // Update recent pages for the source page to the destination page.
                bookmarksDao.migrateRecentPagesPage(sourcePage, destinationPage);
            }

            // Notify bookmark and recent page updates.
            bookmarkModel.notifyBookmarksUpdated();
            bookmarkModel.notifyRecentPagesUpdated(destinationPageSuraStart[0]);
        }
    }

    @Override
    public void bind(PageSelectActivity what) {
        currentView = what;
        generateData();
    }

    @Override
    public void unbind(PageSelectActivity what) {
        if (currentView == what) {
            currentView = null;
            compositeDisposable.clear();
            downloadingSet.clear();
        }
    }
}
