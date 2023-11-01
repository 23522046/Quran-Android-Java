package com.quran.labs.androidquran.presenter.data;

import com.quran.analytics.AnalyticsProvider;
import com.quran.labs.androidquran.common.audio.model.QariItem;
import com.quran.labs.androidquran.util.QuranSettings;
import dagger.Reusable;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@Reusable
public class QuranEventLogger {

    private final AnalyticsProvider analyticsProvider;
    private final QuranSettings quranSettings;

    @Inject
    public QuranEventLogger(AnalyticsProvider analyticsProvider, QuranSettings quranSettings) {
        this.analyticsProvider = analyticsProvider;
        this.quranSettings = quranSettings;
    }

    public void logAnalytics(boolean isDualPages, boolean showingTranslations, boolean isSplitScreen) {
        boolean isLockOrientation = quranSettings.isLockOrientation();
        String lockingOrientation = isLockOrientation ? (quranSettings.isLandscapeOrientation() ? "landscape" : "portrait") : "no";

        Map<String, Object> params = new HashMap<>();
        params.put("mode", getScreenMode(isDualPages, showingTranslations, isSplitScreen));
        params.put("pageType", quranSettings.getPageType());
        params.put("isNightMode", quranSettings.isNightMode());
        params.put("isArabic", quranSettings.isArabicNames());
        params.put("background", quranSettings.useNewBackground() ? "default" : "legacy");
        params.put("isLockingOrientation", lockingOrientation);
        params.put("overlayInfo", quranSettings.shouldOverlayPageInfo());
        params.put("markerPopups", quranSettings.shouldDisplayMarkerPopup());
        params.put("navigation", quranSettings.navigateWithVolumeKeys() ? "with_volume" : "default");
        params.put("shouldHighlightBookmarks", quranSettings.shouldHighlightBookmarks());

        analyticsProvider.logEvent("quran_view", params);
    }

    public void logAudioPlayback(
            AudioPlaybackSource source,
            QariItem qari,
            boolean isDualPages,
            boolean showingTranslations,
            boolean isSplitScreen
    ) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", qari.getId());
        params.put("path", qari.getPath());
        params.put("isGapless", qari.isGapless());
        params.put("source", source == AudioPlaybackSource.PAGE ? "page" : "ayah");
        params.put("mode", getScreenMode(isDualPages, showingTranslations, isSplitScreen));

        analyticsProvider.logEvent("audio_playback", params);
    }

    public void switchToTranslationMode(int translations) {
        Map<String, Object> params = new HashMap<>();
        params.put("translations", translations);

        analyticsProvider.logEvent("switch_to_translations", params);
    }

    private String getScreenMode(
            boolean isDualPages,
            boolean showingTranslations,
            boolean isSplitScreen
    ) {
        if (isDualPages && showingTranslations && isSplitScreen) {
            return "split_quran_translation";
        } else if (isDualPages && showingTranslations) {
            return "dual_translations";
        } else if (isDualPages) {
            return "dual_quran";
        } else if (showingTranslations) {
            return "translation";
        } else {
            return "quran";
        }
    }

    public enum AudioPlaybackSource {
        PAGE, AYAH
    }
}
