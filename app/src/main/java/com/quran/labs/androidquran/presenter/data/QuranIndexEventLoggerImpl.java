package com.quran.labs.androidquran.presenter.data;

import com.quran.analytics.AnalyticsProvider;
import com.quran.labs.androidquran.util.QuranSettings;

import javax.inject.Inject;

import dagger.Reusable;

@Reusable
public class QuranIndexEventLoggerImpl implements QuranIndexEventLogger {

    private final AnalyticsProvider analyticsProvider;
    private final QuranSettings quranSettings;

    @Inject
    public QuranIndexEventLoggerImpl(AnalyticsProvider analyticsProvider, QuranSettings quranSettings) {
        this.analyticsProvider = analyticsProvider;
        this.quranSettings = quranSettings;
    }

    @Override
    public void logAnalytics() {
        String appLocation = quranSettings.getAppCustomLocation();
        String pathType;
        if (appLocation == null) {
            pathType = "unknown";
        } else if (appLocation.contains("com.quran")) {
            pathType = "external";
        } else {
            pathType = "sdcard";
        }

        Map<String, Object> params = new HashMap<>();
        params.put("pathType", pathType);
        params.put("sortOrder", quranSettings.getBookmarksSortOrder());
        params.put("groupByTags", quranSettings.getBookmarksGroupedByTags());
        params.put("showRecents", quranSettings.getShowRecents());
        params.put("showDate", quranSettings.getShowDate());

        analyticsProvider.logEvent("quran_index_view", params);
    }
}
