package com.quran.labs.androidquran.common;

import java.util.Comparator;

public class LocalTranslationDisplaySort implements Comparator<LocalTranslation> {
    @Override
    public int compare(LocalTranslation first, LocalTranslation second) {
        return Integer.compare(first.getDisplayOrder(), second.getDisplayOrder());
    }
}
