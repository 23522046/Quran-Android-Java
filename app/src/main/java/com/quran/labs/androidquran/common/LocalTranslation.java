package com.quran.labs.androidquran.common;

public class LocalTranslation {
    private final int id;
    private final String filename;
    private final String name;
    private final String translator;
    private final String translatorForeign;
    private final String url;
    private final String languageCode;
    private final int version;
    private final int minimumVersion;
    private final int displayOrder;

    public LocalTranslation(int id, String filename, String name, String translator, String translatorForeign,
                            String url, String languageCode, int version, int minimumVersion, int displayOrder) {
        this.id = id;
        this.filename = filename;
        this.name = name;
        this.translator = translator;
        this.translatorForeign = translatorForeign;
        this.url = url;
        this.languageCode = languageCode;
        this.version = version;
        this.minimumVersion = minimumVersion;
        this.displayOrder = displayOrder;
    }

    public int getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public String getName() {
        return name;
    }

    public String getTranslator() {
        return translator;
    }

    public String getTranslatorForeign() {
        return translatorForeign;
    }

    public String getUrl() {
        return url;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public int getVersion() {
        return version;
    }

    public int getMinimumVersion() {
        return minimumVersion;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public String getTranslatorName() {
        if (translatorForeign != null && !translatorForeign.isEmpty()) {
            return translatorForeign;
        } else if (translator != null && !translator.isEmpty()) {
            return translator;
        } else if (!name.isEmpty()) {
            return name;
        } else {
            return filename;
        }
    }
}
