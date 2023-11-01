package com.quran.labs.androidquran.presenter.data;

import com.quran.data.core.QuranInfo;
import com.quran.labs.androidquran.model.translation.ArabicDatabaseUtils;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.withContext;

import javax.inject.Inject;
import java.util.List;

public class JuzListPresenter {

    private final QuranInfo quranInfo;
    private final ArabicDatabaseUtils arabicDatabaseUtils;

    @Inject
    public JuzListPresenter(QuranInfo quranInfo, ArabicDatabaseUtils arabicDatabaseUtils) {
        this.quranInfo = quranInfo;
        this.arabicDatabaseUtils = arabicDatabaseUtils;
    }

    public List<String> quarters() {
        return withContext(Dispatchers.IO, () -> {
            List<Integer> ayahIds = quranInfo.getQuarters();
            List<String> results = arabicDatabaseUtils.getAyahTextForAyat(ayahIds);
            for (int i = 0; i < ayahIds.size(); i++) {
                int id = ayahIds.get(i);
                results.set(i, results.get(id));
            }
            return results;
        });
    }
}
