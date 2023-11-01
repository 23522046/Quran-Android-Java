package com.quran.labs.androidquran.util;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.VisibleForTesting;
import com.quran.data.core.QuranInfo;
import com.quran.data.model.SuraAyah;
import com.quran.data.model.audio.Qari;
import com.quran.labs.androidquran.common.audio.model.QariItem;
import com.quran.labs.androidquran.common.audio.util.QariUtil;
import com.quran.labs.androidquran.service.AudioService;
import timber.log.Timber;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AudioUtils {

    @Inject
    private final QuranInfo quranInfo;
    private final QuranFileUtils quranFileUtils;
    private final QariUtil qariUtil;
    private final int totalPages = quranInfo.numberOfPages;

    public static final String ZIP_EXTENSION = ".zip";
    public static final String AUDIO_EXTENSION = ".mp3";
    private static final String DB_EXTENSION = ".db";

    @Inject
    public AudioUtils(QuranInfo quranInfo, QuranFileUtils quranFileUtils, QariUtil qariUtil) {
        this.quranInfo = quranInfo;
        this.quranFileUtils = quranFileUtils;
        this.qariUtil = qariUtil;
    }

    public List<QariItem> getQariList(Context context) {
        List<QariItem> qariItemList = qariUtil.getQariList(context);
        List<QariItem> filteredList = new ArrayList<QariItem>();
        for (QariItem item : qariItemList) {
            if (item.isGapless() || (item.hasGaplessAlternative() && !haveAnyFiles(item.getPath()))) {
                filteredList.add(item);
            }
        }
        Collections.sort(filteredList, new Comparator<QariItem>() {
            @Override
            public int compare(QariItem lhs, QariItem rhs) {
                if (lhs.isGapless() != rhs.isGapless()) {
                    return lhs.isGapless() ? -1 : 1;
                } else {
                    return lhs.getName().compareTo(rhs.getName());
                }
            }
        });
        return filteredList;
    }

    private boolean haveAnyFiles(String path) {
        File basePath = quranFileUtils.audioFileDirectory();
        File file = new File(basePath, path);
        return file.isDirectory() && file.list() != null && file.list().length > 0;
    }

    public String getQariUrl(Qari qari) {
        return qari.getUrl() + (qari.isGapless() ? "%03d" + AUDIO_EXTENSION : "%03d%03d" + AUDIO_EXTENSION);
    }

    public String getQariUrl(QariItem item) {
        return item.getUrl() + (item.isGapless() ? "%03d" + AUDIO_EXTENSION : "%03d%03d" + AUDIO_EXTENSION);
    }

    public String getLocalQariUrl(QariItem item) {
        String rootDirectory = quranFileUtils.audioFileDirectory();
        return rootDirectory == null ? null : rootDirectory + item.getPath();
    }

    public String getLocalQariUri(QariItem item) {
        String rootDirectory = quranFileUtils.audioFileDirectory();
        if (rootDirectory == null) return null;
        return rootDirectory + item.getPath() + File.separator + (item.isGapless() ?
                "%03d" + AUDIO_EXTENSION :
                "%d" + File.separator + "%d" + AUDIO_EXTENSION);
    }

    public String getQariDatabasePathIfGapless(QariItem item) {
        String databaseName = item.getDatabaseName();
        if (databaseName != null) {
            String path = getLocalQariUrl(item);
            if (path != null) {
                databaseName = path + File.separator + databaseName + DB_EXTENSION;
            }
        }
        return databaseName;
    }

    public SuraAyah getLastAyahToPlay(SuraAyah startAyah, int currentPage, int mode, boolean isDualPageVisible) {
        int potentialPage = isDualPageVisible && mode == LookAheadAmount.PAGE && currentPage % 2 == 1 ?
                currentPage + 1 : currentPage;
        int page = isDualPageVisible && potentialPage == (totalPages + 1) ? totalPages : potentialPage;

        int pageLastSura = 114;
        int pageLastAyah = 6;

        if (page > totalPages || page < 0) {
            return null;
        }

        if (mode == LookAheadAmount.SURA) {
            int sura = startAyah.getSura();
            int lastAyah = quranInfo.getNumberOfAyahs(sura);
            if (lastAyah == -1) {
                return null;
            }
            return new SuraAyah(sura, lastAyah);
        } else if (mode == LookAheadAmount.JUZ) {
            int juz = quranInfo.getJuzFromPage(page);
            if (juz == 30) {
                return new SuraAyah(114, 6);
            } else if (juz >= 1 && juz <= 29) {
                Quarter endJuz = quranInfo.getQuarterByIndex(juz * 8);
                if (pageLastSura > endJuz.getSura() ||
                        (pageLastSura == endJuz.getSura() && pageLastAyah > endJuz.getAyah())) {
                    return getQuarterForNextJuz(juz);
                }
                return new SuraAyah(endJuz.getSura(), endJuz.getAyah());
            }
        } else {
            VerseRange range = quranInfo.getVerseRangeForPage(page);
            pageLastSura = range.getEndingSura();
            pageLastAyah = range.getEndingAyah();
        }

        return new SuraAyah(pageLastSura, pageLastAyah);
    }

    private SuraAyah getQuarterForNextJuz(int currentJuz) {
        if (currentJuz < 29) {
            Quarter juz = quranInfo.getQuarterByIndex((currentJuz + 1) * 8);
            return new SuraAyah(juz.getSura(), juz.getAyah());
        } else {
            return new SuraAyah(114, 6);
        }
    }

    public boolean shouldDownloadBasmallah(String baseDirectory, SuraAyah start, SuraAyah end, boolean isGapless) {
        if (isGapless) {
            return false;
        }

        if (!baseDirectory.isEmpty()) {
            File f = new File(baseDirectory);
            if (f.exists()) {
                String filename = "1" + File.separator + "1" + AUDIO_EXTENSION;
                f = new File(baseDirectory + File.separator + filename);
                if (f.exists()) {
                    Timber.d("already have basmalla...");
                    return false;
                }
            } else {
                f.mkdirs();
            }
        }

        return doesRequireBasmallah(start, end);
    }

    @VisibleForTesting
    public boolean doesRequireBasmallah(SuraAyah minAyah, SuraAyah maxAyah) {
        Timber.d("seeing if need basmalla...");

        for (int i = minAyah.getSura(); i <= maxAyah.getSura(); i++) {
            int firstAyah = (i == minAyah.getSura()) ? minAyah.getAyah() : 1;
            if (firstAyah == 1 && i != 1 && i != 9) {
                return true;
            }
        }

        return false;
    }

    public boolean haveAllFiles(String baseUrl, String path, SuraAyah start, SuraAyah end, boolean isGapless) {
        if (path.isEmpty()) {
            return false;
        }

        File f = new File(path);
        if (!f.exists()) {
            f.mkdirs();
            return false;
        }

        int startSura = start.getSura();
        int startAyah = start.getAyah();

        int endSura = end.getSura();
        int endAyah = end.getAyah();

        if (endSura < startSura || (endSura == startSura && endAyah < startAyah)) {
            throw new IllegalStateException("End isn't larger than the start: " +
                    startSura + ":" + startAyah + " to " + endSura + ":" + endAyah);
        }

        for (int i = startSura; i <= endSura; i++) {
            int lastAyah = (i == endSura) ? endAyah : quranInfo.getNumberOfAyahs(i);
            int firstAyah = (i == startSura) ? startAyah : 1;

            if (isGapless) {
                if (i == endSura && endAyah == 0) {
                    continue;
                }
                String fileName = String.format(Locale.US, baseUrl, i);
                Timber.d("gapless, checking if we have %s", fileName);
                f = new File(fileName);
                if (!f.exists()) {
                    return false;
                }
                continue;
            }

            Timber.d("not gapless, checking each ayah...");
            for (int j = firstAyah; j <= lastAyah; j++) {
                String filename = i + File.separator + j + AUDIO_EXTENSION;
                f = new File(path + File.separator + filename);
                if (!f.exists()) {
                    return false;
                }
            }
        }

        return true;
    }

    public Intent getAudioIntent(Context context, String action) {
        Intent intent = new Intent(context, AudioService.class);
        intent.setAction(action);
        return intent;
    }

    private static class LookAheadAmount {
        public static final int PAGE = 1;
        public static final int SURA = 2;
        public static final int JUZ = 3;

        // make sure to update these when a lookup type is added
        public static final int MIN = 1;
        public static final int MAX = 3;
    }

    private static class Quarter {
        private final int sura;
        private final int ayah;

        public Quarter(int sura, int ayah) {
            this.sura = sura;
            this.ayah = ayah;
        }

        public int getSura() {
            return sura;
        }

        public int getAyah() {
            return ayah;
        }
    }

    private static class VerseRange {
        private final int startingSura;
        private final int startingAyah;
        private final int endingSura;
        private final int endingAyah;

        public VerseRange(int startingSura, int startingAyah, int endingSura, int endingAyah) {
            this.startingSura = startingSura;
            this.startingAyah = startingAyah;
            this.endingSura = endingSura;
            this.endingAyah = endingAyah;
        }

        public int getStartingSura() {
            return startingSura;
        }

        public int getStartingAyah() {
            return startingAyah;
        }

        public int getEndingSura() {
            return endingSura;
        }

        public int getEndingAyah() {
            return endingAyah;
        }
    }
}
