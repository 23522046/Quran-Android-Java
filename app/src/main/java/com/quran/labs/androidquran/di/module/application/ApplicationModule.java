package com.quran.labs.androidquran.di.module.application;

import android.app.Application;
import android.content.Context;
import android.graphics.Point;
import android.view.Display;
import android.view.WindowManager;
import com.quran.data.constant.DependencyInjectionConstants;
import com.quran.data.core.QuranFileManager;
import com.quran.data.dao.Settings;
import com.quran.data.source.DisplaySize;
import com.quran.data.source.PageProvider;
import com.quran.data.source.PageSizeCalculator;
import com.quran.labs.androidquran.data.QuranFileConstants;
import com.quran.labs.androidquran.util.QuranFileUtils;
import com.quran.labs.androidquran.util.QuranSettings;
import com.quran.labs.androidquran.util.SettingsImpl;
import com.quran.mobile.di.ExtraPreferencesProvider;
import com.quran.mobile.di.ExtraScreenProvider;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.ElementsIntoSet;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Scheduler;
import okio.FileSystem;

import java.io.File;

import javax.inject.Named;
import javax.inject.Singleton;

@Module
public class ApplicationModule {

    private final Application application;

    public ApplicationModule(Application application) {
        this.application = application;
    }

    @Provides
    public Context provideApplicationContext() {
        return application;
    }

    @Provides
    public Display provideDisplay(Context appContext) {
        WindowManager w = (WindowManager) appContext.getSystemService(Context.WINDOW_SERVICE);
        return w.getDefaultDisplay();
    }

    @Provides
    public DisplaySize provideDisplaySize(Display display) {
        Point point = new Point();
        display.getRealSize(point);
        return new DisplaySize(point.x, point.y);
    }

    @Provides
    public PageSizeCalculator provideQuranPageSizeCalculator(
            PageProvider pageProvider, DisplaySize displaySize) {
        return pageProvider.getPageSizeCalculator(displaySize);
    }

    @Provides
    @Singleton
    public QuranSettings provideQuranSettings() {
        return QuranSettings.getInstance(application);
    }

    @Provides
    public Settings provideSettings(SettingsImpl settingsImpl) {
        return settingsImpl;
    }

    @Named(DependencyInjectionConstants.CURRENT_PAGE_TYPE)
    @Provides
    public String provideCurrentPageType(QuranSettings quranSettings) {
        String currentKey = quranSettings.getPageType();
        String result = currentKey != null ? currentKey : QuranFileConstants.FALLBACK_PAGE_TYPE;
        if (currentKey == null) {
            quranSettings.setPageType(result);
        }
        return result;
    }

    @Provides
    public QuranFileManager provideQuranFileManager(QuranFileUtils quranFileUtils) {
        return quranFileUtils;
    }

    @Provides
    public FileSystem provideFileSystem() {
        return FileSystem.SYSTEM;
    }

    @Provides
    public Scheduler provideMainThreadScheduler() {
        return AndroidSchedulers.mainThread();
    }

    @Provides
    public File provideCacheDirectory() {
        return application.getCacheDir();
    }

    @Provides
    @ElementsIntoSet
    public Set<ExtraPreferencesProvider> provideExtraPreferences() {
        return Collections.emptySet();
    }

    @Provides
    @ElementsIntoSet
    public Set<ExtraScreenProvider> provideExtraScreens() {
        return Collections.emptySet();
    }
}
