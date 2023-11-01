package com.quran.labs.androidquran.dao.translation;

public class TranslationItem implements TranslationRowData {
    private final Translation translation;
    private final int localVersion;
    private final int displayOrder;

    public TranslationItem(Translation translation, int localVersion, int displayOrder) {
        this.translation = translation;
        this.localVersion = localVersion;
        this.displayOrder = displayOrder;
    }

    public TranslationItem(Translation translation, int localVersion) {
        this(translation, localVersion, -1);
    }

    @Override
    public boolean isSeparator() {
        return false;
    }

    public boolean exists() {
        return localVersion > 0;
    }

    @Override
    public String name() {
        return translation.getDisplayName();
    }

    @Override
    public boolean needsUpgrade() {
        return localVersion > 0 && translation.getCurrentVersion() > localVersion;
    }

    public TranslationItem withTranslationRemoved() {
        return new TranslationItem(translation, 0, displayOrder);
    }

    public TranslationItem withTranslationVersion(int version) {
        return new TranslationItem(translation, version, displayOrder);
    }

    public TranslationItem withDisplayOrder(int newDisplayOrder) {
        return new TranslationItem(translation, localVersion, newDisplayOrder);
    }
}
