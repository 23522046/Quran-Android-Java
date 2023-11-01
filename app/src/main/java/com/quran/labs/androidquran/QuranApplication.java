package com.quran.labs.androidquran;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import androidx.work.Configuration;
import androidx.work.WorkManager;
import com.quran.labs.androidquran.core.worker.QuranWorkerFactory;
import com.quran.labs.androidquran.di.component.application.ApplicationComponent;
import com.quran.labs.androidquran.di.component.application.DaggerApplicationComponent;
import com.quran.labs.androidquran.di.module.application.ApplicationModule;
import com.quran.labs.androidquran.util.QuranSettings;
import com.quran.labs.androidquran.util.RecordingLogTree;
import com.quran.labs.androidquran.widget.BookmarksWidgetSubscriber;
import com.quran.mobile.di.QuranApplicationComponent;
import com.quran.mobile.di.QuranApplicationComponentProvider;
import timber.log.Timber;
import java.util.Locale;
import javax.inject.Inject;

public class QuranApplication extends Application implements QuranApplicationComponentProvider {

    protected ApplicationComponent applicationComponent;

    @Inject
    protected QuranWorkerFactory quranWorkerFactory;

    @Inject
    protected BookmarksWidgetSubscriber bookmarksWidgetSubscriber;

    @Override
    public QuranApplicationComponent provideQuranApplicationComponent() {
        return applicationComponent;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setupTimber();
        applicationComponent = initializeInjector();
        applicationComponent.inject(this);
        initializeWorkManager();
        bookmarksWidgetSubscriber.subscribeBookmarksWidgetIfNecessary();
    }

    public void setupTimber() {
        Timber.plant(new RecordingLogTree());
    }

    public ApplicationComponent initializeInjector() {
        return DaggerApplicationComponent.builder()
                .applicationModule(new ApplicationModule(this))
                .build();
    }

    public void initializeWorkManager() {
        WorkManager.initialize(
                this,
                new Configuration.Builder()
                        .setWorkerFactory(quranWorkerFactory)
                        .build()
        );
    }

    public void refreshLocale(Context context, boolean force) {
        String language = QuranSettings.getInstance(this).isArabicNames() ? "ar" : null;
        Locale locale;
        if ("ar".equals(language)) {
            locale = new Locale("ar");
        } else if (force) {
            locale = Resources.getSystem().getConfiguration().locale;
        } else {
            return; // Nothing to do
        }
        updateLocale(context, locale);
        Context appContext = context.getApplicationContext();
        if (context != appContext) {
            updateLocale(appContext, locale);
        }
    }

    private void updateLocale(Context context, Locale locale) {
        Resources resources = context.getResources();
        android.content.res.Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        config.setLayoutDirection(config.locale);
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }
}
