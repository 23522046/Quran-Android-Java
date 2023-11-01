package com.quran.labs.androidquran.dao.translation;

public class TranslationHeader implements TranslationRowData {
    private final String name;

    public TranslationHeader(String name) {
        this.name = name;
    }

    @Override
    public boolean isSeparator() {
        return true;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean needsUpgrade() {
        return false;
    }
}
