package com.quran.labs.androidquran.di.module.application;

import com.quran.data.dao.BookmarksDao;
import com.quran.data.dao.TranslationsDao;
import com.quran.labs.androidquran.database.BookmarksDaoImpl;
import com.quran.labs.androidquran.database.TranslationsDaoImpl;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

@Module
public class DatabaseModule {

    @Provides
    @Singleton
    public BookmarksDao provideBookmarksDao(BookmarksDaoImpl daoImpl) {
        return daoImpl;
    }

    @Provides
    @Singleton
    public TranslationsDao provideTranslationsDao(TranslationsDaoImpl daoImpl) {
        return daoImpl;
    }
}
