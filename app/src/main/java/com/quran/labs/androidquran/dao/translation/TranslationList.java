package com.quran.labs.androidquran.dao.translation;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

import java.util.List;

@JsonClass(generateAdapter = true)
public class TranslationList {
    @Json(name = "data")
    private List<Translation> translations;

    public List<Translation> getTranslations() {
        return translations;
    }
}
