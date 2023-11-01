package com.quran.labs.androidquran.model.translation;

import android.content.Context;
import com.quran.data.model.QuranText;
import com.quran.data.model.VerseRange;
import com.quran.data.pageinfo.mapper.AyahMapper;
import com.quran.labs.androidquran.data.QuranDataProvider;
import com.quran.labs.androidquran.database.DatabaseHandler;
import com.quran.labs.androidquran.database.DatabaseHandler.TextType;
import com.quran.data.di.ActivityScope;
import com.quran.labs.androidquran.util.QuranFileUtils;
import io.reactivex.rxjava3.core.Single;

import javax.inject.Inject;
import java.util.List;

@ActivityScope
public class TranslationModel {

    private final Context appContext;
    private final QuranFileUtils quranFileUtils;
    private final AyahMapper ayahMapper;

    @Inject
    public TranslationModel(
            Context appContext,
            QuranFileUtils quranFileUtils,
            AyahMapper ayahMapper
    ) {
        this.appContext = appContext;
        this.quranFileUtils = quranFileUtils;
        this.ayahMapper = ayahMapper;
    }

    public Single<List<QuranText>> getArabicFromDatabase(VerseRange verses) {
        return getVersesFromDatabase(
                verses,
                QuranDataProvider.QURAN_ARABIC_DATABASE,
                TextType.ARABIC,
                false
        );
    }

    public Single<List<QuranText>> getTranslationFromDatabase(VerseRange verses, String db) {
        return getVersesFromDatabase(verses, db, TextType.TRANSLATION, true);
    }

    private Single<List<QuranText>> getVersesFromDatabase(
            VerseRange verses,
            String database,
            @TextType int type,
            boolean shouldMap
    ) {
        return Single.fromCallable(() -> {
            DatabaseHandler databaseHandler = DatabaseHandler.getDatabaseHandler(
                    appContext,
                    database,
                    quranFileUtils
            );

            if (shouldMap) {
                VerseRange mappedRange = ayahMapper.mapRange(verses);
                List<QuranText> data = databaseHandler.getVerses(mappedRange, type);
                return ayahMapper.mapKufiData(verses, data);
            } else {
                return databaseHandler.getVerses(verses, type);
            }
        });
    }
}
