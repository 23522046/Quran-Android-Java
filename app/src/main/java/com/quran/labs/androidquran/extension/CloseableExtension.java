package com.quran.labs.androidquran.extension;

import java.io.Closeable;

public class CloseableExtension {
    public static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (Exception e) {
            // no op
        }
    }
}
