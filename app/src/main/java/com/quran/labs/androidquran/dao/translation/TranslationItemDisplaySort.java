package com.quran.labs.androidquran.dao.translation;

import java.util.Comparator;

public class TranslationItemDisplaySort implements Comparator<TranslationItem> {
    @Override
    public int compare(TranslationItem first, TranslationItem second) {
        return Integer.compare(first.getDisplayOrder(), second.getDisplayOrder());
    }
}
