package com.quran.labs.androidquran.extension;

import com.quran.data.model.SuraAyah;

public class SuraAyahExtension {
    public static boolean requiresBasmallah(SuraAyah suraAyah) {
        return suraAyah.getAyah() == 1 && suraAyah.getSura() != 1 && suraAyah.getSura() != 9;
    }
}
