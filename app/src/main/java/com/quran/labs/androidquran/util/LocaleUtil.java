package com.quran.labs.androidquran.util;

import android.content.Context;
import android.content.res.Resources;

import java.util.Locale;

public class LocaleUtil {

    public static Locale getLocale(Context context) {
        boolean isArabic = QuranSettings.getInstance(context.getApplicationContext()).isArabicNames();
        return isArabic ? new Locale("ar") : Resources.getSystem().getConfiguration().locale;
    }
}
