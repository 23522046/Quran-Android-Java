package com.quran.labs.androidquran.data;

import android.content.Context;
import androidx.annotation.StringRes;
import com.quran.data.core.QuranInfo;
import com.quran.data.di.AppScope;
import com.quran.data.model.SuraAyah;
import com.quran.data.model.SuraAyahIterator;
import com.quran.labs.androidquran.R;
import com.quran.labs.androidquran.util.QuranUtils;
import com.quran.page.common.data.QuranNaming;
import com.squareup.anvil.annotations.ContributesBinding;
import timber.log.Timber;

import javax.inject.Inject;
import java.util.LinkedHashSet;
import java.util.Set;

@ContributesBinding(AppScope.class)
public class QuranDisplayData implements QuranNaming {

    private final QuranInfo quranInfo;

    @Inject
    public QuranDisplayData(QuranInfo quranInfo) {
        this.quranInfo = quranInfo;
    }

    @Override
    public String getSuraName(Context context, int sura, boolean wantPrefix) {
        return getSuraName(context, sura, wantPrefix, false);
    }

    public String getSuraName(Context context, int sura, boolean wantPrefix, boolean wantTranslation) {
        if (sura < Constants.SURA_FIRST || sura > Constants.SURA_LAST) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        String[] suraNames = context.getResources().getStringArray(R.array.sura_names);
        if (wantPrefix) {
            builder.append(context.getString(R.string.quran_sura_title, suraNames[sura - 1]));
        } else {
            builder.append(suraNames[sura - 1]);
        }
        if (wantTranslation) {
            String translation = context.getResources().getStringArray(R.array.sura_names_translation)[sura - 1];
            if (translation != null && !translation.isEmpty()) {
                builder.append(" (").append(translation).append(")");
            }
        }

        return builder.toString();
    }

    public String getSuraNameWithNumber(Context context, int sura, boolean wantPrefix) {
        String name = getSuraName(context, sura, wantPrefix);
        String prefix = QuranUtils.getLocalizedNumber(context, sura);
        return prefix + ". " + name;
    }

    public String getSuraNameFromPage(Context context, int page, boolean wantTitle) {
        int sura = quranInfo.getSuraNumberFromPage(page);
        return (sura > 0) ? getSuraName(context, sura, wantTitle, false) : "";
    }

    public String getPageSubtitle(Context context, int page) {
        String description = context.getString(R.string.page_description);
        return String.format(description,
                QuranUtils.getLocalizedNumber(context, page),
                QuranUtils.getLocalizedNumber(context, quranInfo.getJuzForDisplayFromPage(page)));
    }

    public String getJuzDisplayStringForPage(Context context, int page) {
        String description = context.getString(R.string.juz2_description);
        return String.format(description,
                QuranUtils.getLocalizedNumber(context, quranInfo.getJuzForDisplayFromPage(page)));
    }

    public String getManzilForPage(Context context, int page) {
        int manzil = quranInfo.manzilForPage(page);
        if (manzil > 0) {
            return context.getString(R.string.comma) + ' ' +
                    context.getString(R.string.manzil_description,
                            QuranUtils.getLocalizedNumber(context, manzil));
        } else {
            return "";
        }
    }

    public String getSuraAyahString(Context context, int sura, int ayah) {
        return getSuraAyahString(context, sura, ayah, R.string.sura_ayah_notification_str);
    }

    public String getSuraAyahString(Context context, int sura, int ayah, @StringRes int resource) {
        String suraName = getSuraName(context, sura, false, false);
        return context.getString(resource, suraName, ayah);
    }

    public String getNotificationTitle(Context context, SuraAyah minVerse, SuraAyah maxVerse, boolean isGapless) {
        int minSura = minVerse.sura;
        int maxSura = maxVerse.sura;
        String notificationTitle = getSuraName(context, minSura, true, false);
        if (isGapless) {
            return (minSura == maxSura) ? notificationTitle :
                    notificationTitle + " - " + getSuraName(context, maxSura, true, false);
        }

        int maxAyah = maxVerse.ayah;
        if (maxAyah == 0) {
            maxSura--;
            maxAyah = quranInfo.getNumberOfAyahs(maxSura);
        }

        return notificationTitle + ((minSura == maxSura) ?
                ((minVerse.ayah == maxAyah) ? " (" + maxAyah + ")" :
                        " (" + minVerse.ayah + "-" + maxAyah + ")") :
                (" (" + minVerse.ayah + ") - " +
                        getSuraName(context, maxSura, true, false) + " (" + maxAyah + ")"));
    }

    public String getSuraListMetaString(Context context, int sura) {
        String info = context.getString(quranInfo.isMakki(sura) ? R.string.makki : R.string.madani) + " - ";
        int ayahs = quranInfo.getNumberOfAyahs(sura);
        return info + context.getResources().getQuantityString(R.plurals.verses, ayahs,
                QuranUtils.getLocalizedNumber(context, ayahs));
    }

    public int safelyGetSuraOnPage(int page) {
        if (!quranInfo.isValidPage(page)) {
            Timber.e(new IllegalArgumentException("safelyGetSuraOnPage with page: " + page));
            return quranInfo.getSuraOnPage(1);
        } else {
            return quranInfo.getSuraOnPage(page);
        }
    }

    private String getSuraNameString(Context context, int page) {
        return context.getString(R.string.quran_sura_title, getSuraNameFromPage(context, page, false));
    }

    public Set<String> getAyahKeysOnPage(int page) {
        Set<String> ayahKeys = new LinkedHashSet<>();
        int[] bounds = quranInfo.getPageBounds(page);
        SuraAyah start = new SuraAyah(bounds[0], bounds[1]);
        SuraAyah end = new SuraAyah(bounds[2], bounds[3]);
        SuraAyahIterator iterator = new SuraAyahIterator(quranInfo, start, end);
        while (iterator.next()) {
            ayahKeys.add(iterator.sura + ":" + iterator.ayah);
        }
        return ayahKeys;
    }

    public String getAyahString(int sura, int ayah, Context context) {
        return getSuraName(context, sura, true) + " - " + context.getString(R.string.quran_ayah, ayah);
    }

    public String getAyahMetadata(int sura, int ayah, int page, Context context) {
        int juz = quranInfo.getJuzForDisplayFromPage(page);
        return context.getString(R.string.quran_ayah_details,
                getSuraName(context, sura, true),
                QuranUtils.getLocalizedNumber(context, ayah),
                QuranUtils.getLocalizedNumber(context, quranInfo.getJuzFromSuraAyah(sura, ayah, juz)));
    }
}
