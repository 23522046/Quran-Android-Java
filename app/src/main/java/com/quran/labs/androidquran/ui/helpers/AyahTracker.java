package com.quran.labs.androidquran.ui.helpers;

import com.quran.data.model.selection.SelectionIndicator;
import com.quran.labs.androidquran.common.LocalTranslation;
import com.quran.labs.androidquran.common.QuranAyahInfo;

public interface AyahTracker {
    SelectionIndicator getToolBarPosition(int sura, int ayah);
    QuranAyahInfo getQuranAyahInfo(int sura, int ayah);
    LocalTranslation[] getLocalTranslations();
}
