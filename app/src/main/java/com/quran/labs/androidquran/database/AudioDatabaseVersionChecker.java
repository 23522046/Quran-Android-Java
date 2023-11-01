package com.quran.labs.androidquran.database;

import com.quran.labs.androidquran.feature.audio.VersionableDatabaseChecker;

import javax.inject.Inject;

public class AudioDatabaseVersionChecker implements VersionableDatabaseChecker {

    @Inject
    public AudioDatabaseVersionChecker() {
    }

    @Override
    public int getVersionForDatabase(String path) {
        return SuraTimingDatabaseHandler.getDatabaseHandler(path).getVersion();
    }
}
