package com.quran.labs.androidquran.util;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UrlUtil {

    @Inject
    public UrlUtil() {
        // Constructor
    }

    public String fallbackUrl(String url) {
        return url.replace(".quranicaudio.com", ".quranicaudio.org");
    }
}
