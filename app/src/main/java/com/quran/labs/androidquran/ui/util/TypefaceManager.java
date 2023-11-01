package com.quran.labs.androidquran.ui.util;

import android.content.Context;
import android.graphics.Typeface;
import com.quran.labs.androidquran.data.QuranFileConstants;

public class TypefaceManager {

    public static final int TYPE_UTHMANI_HAFS = 1;
    public static final int TYPE_NOOR_HAYAH = 2;
    public static final int TYPE_UTHMANIC_WARSH = 3;
    public static final int TYPE_UTHMANIC_QALOON = 4;

    private static Typeface typeface = null;
    private static Typeface arabicTafseerTypeface = null;
    private static Typeface arabicHeaderFooterTypeface = null;
    private static Typeface dyslexicTypeface = null;

    public static Typeface getUthmaniTypeface(Context context) {
        if (typeface == null) {
            String fontName;
            switch (QuranFileConstants.FONT_TYPE) {
                case TYPE_NOOR_HAYAH:
                    fontName = "noorehira.ttf";
                    break;
                case TYPE_UTHMANIC_WARSH:
                    fontName = "uthmanic_warsh_ver09.ttf";
                    break;
                case TYPE_UTHMANIC_QALOON:
                    fontName = "uthmanic_qaloon_ver21.ttf";
                    break;
                default:
                    fontName = "uthmanic_hafs_ver12.otf";
            }
            typeface = Typeface.createFromAsset(context.getAssets(), fontName);
        }
        return typeface;
    }

    public static Typeface getTafseerTypeface(Context context) {
        if (arabicTafseerTypeface == null) {
            arabicTafseerTypeface = Typeface.createFromAsset(context.getAssets(), "kitab.ttf");
        }
        return arabicTafseerTypeface;
    }

    public static Typeface getDyslexicTypeface(Context context) {
        if (dyslexicTypeface == null) {
            dyslexicTypeface = Typeface.createFromAsset(context.getAssets(), "OpenDyslexic.otf");
        }
        return dyslexicTypeface;
    }

    public static Typeface getHeaderFooterTypeface(Context context) {
        if (arabicHeaderFooterTypeface == null) {
            arabicHeaderFooterTypeface = Typeface.createFromAsset(context.getAssets(), "UthmanTN1Ver10.otf");
        }
        return arabicHeaderFooterTypeface;
    }
}
