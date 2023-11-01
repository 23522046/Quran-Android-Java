package com.quran.labs.androidquran.dao.translation;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true)
public class Translation {
    private final int id;
    private final int minimumVersion;
    private final int currentVersion;
    private final String displayName;
    private final String downloadType;
    private final String fileName;
    private final String fileUrl;
    private final String saveTo;
    private final String languageCode;
    private final String translator;
    private final String translatorNameLocalized;
    private final int displayOrder;

    public Translation(
        int id,
        int minimumVersion,
        int currentVersion,
        String displayName,
        String downloadType,
        String fileName,
        String fileUrl,
        String saveTo,
        String languageCode,
        String translator,
        String translatorNameLocalized,
        int displayOrder
    ) {
        this.id = id;
        this.minimumVersion = minimumVersion;
        this.currentVersion = currentVersion;
        this.displayName = displayName;
        this.downloadType = downloadType;
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.saveTo = saveTo;
        this.languageCode = languageCode;
        this.translator = translator;
        this.translatorNameLocalized = translatorNameLocalized;
        this.displayOrder = displayOrder;
    }

    public int getId() {
        return id;
    }

    public int getMinimumVersion() {
        return minimumVersion;
    }

    public int getCurrentVersion() {
        return currentVersion;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDownloadType() {
        return downloadType;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public String getSaveTo() {
        return saveTo;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public String getTranslator() {
        return translator;
    }

    public String getTranslatorNameLocalized() {
        return translatorNameLocalized;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public Translation withSchema(int schema) {
        return new Translation(
            id,
            schema,
            currentVersion,
            displayName,
            downloadType,
            fileName,
            fileUrl,
            saveTo,
            languageCode,
            translator,
            translatorNameLocalized,
            displayOrder
        );
    }
}
