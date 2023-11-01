package com.quran.labs.androidquran.util;

import android.content.SharedPreferences;
import com.quran.data.dao.Settings;
import kotlinx.coroutines.channels.Channel;
import kotlinx.coroutines.channels.ReceiveChannel;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.receiveAsFlow;
import javax.inject.Inject;

public class SettingsImpl implements Settings {

    private final QuranSettings quranSettings;

    private final Channel<String> preferencesChannel = new Channel<>(Channel.CONFLATED);

    @Inject
    public SettingsImpl(QuranSettings quranSettings) {
        this.quranSettings = quranSettings;
        setupPreferencesFlow();
    }

    private void setupPreferencesFlow() {
        SharedPreferences.OnSharedPreferenceChangeListener listener =
                (sharedPreferences, pref) -> {
                    if (pref != null) {
                        preferencesChannel.offer(pref);
                    }
                };
        quranSettings.registerPreferencesListener(listener);
    }

    @Override
    public void setVersion(int version) {
        quranSettings.setVersion(version);
    }

    @Override
    public void removeDidDownloadPages() {
        quranSettings.removeDidDownloadPages();
    }

    @Override
    public void setShouldOverlayPageInfo(boolean shouldOverlay) {
        quranSettings.setShouldOverlayPageInfo(shouldOverlay);
    }

    @Override
    public int lastPage() {
        return quranSettings.getLastPage();
    }

    @Override
    public boolean isNightMode() {
        return quranSettings.isNightMode();
    }

    @Override
    public int nightModeTextBrightness() {
        return quranSettings.getNightModeTextBrightness();
    }

    @Override
    public int nightModeBackgroundBrightness() {
        return quranSettings.getNightModeBackgroundBrightness();
    }

    @Override
    public boolean shouldShowHeaderFooter() {
        return quranSettings.shouldOverlayPageInfo();
    }

    @Override
    public boolean shouldShowBookmarks() {
        return quranSettings.shouldHighlightBookmarks();
    }

    @Override
    public String pageType() {
        return quranSettings.getPageType();
    }

    @Override
    public boolean showSidelines() {
        return quranSettings.isSidelines();
    }

    @Override
    public Flow<String> preferencesFlow() {
        return preferencesChannel.receiveAsFlow();
    }
}
