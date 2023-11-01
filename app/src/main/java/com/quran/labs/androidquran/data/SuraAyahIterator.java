package com.quran.labs.androidquran.data;

import com.quran.data.core.QuranInfo;
import com.quran.data.model.SuraAyah;

import java.util.HashSet;
import java.util.Set;

public class SuraAyahIterator {
    private final QuranInfo quranInfo;
    private final SuraAyah start;
    private final SuraAyah end;
    private boolean started = false;
    private int sura = 0;
    private int ayah = 0;

    public SuraAyahIterator(QuranInfo quranInfo, SuraAyah start, SuraAyah end) {
        this.quranInfo = quranInfo;
        if (start.compareTo(end) <= 0) {
            this.start = start;
            this.end = end;
        } else {
            this.start = end;
            this.end = start;
        }
        reset();
    }

    private void reset() {
        sura = start.getSura();
        ayah = start.getAyah();
        started = false;
    }

    public boolean hasNext() {
        return !started || sura < end.getSura() || ayah < end.getAyah();
    }

    public boolean next() {
        if (!started) {
            started = true;
            return true;
        } else if (!hasNext()) {
            return false;
        }
        if (ayah < quranInfo.getNumberOfAyahs(sura)) {
            ayah++;
        } else {
            ayah = 1;
            sura++;
        }
        return true;
    }

    public Set<String> asSet() {
        Set<String> suraAyahSet = new HashSet<>();
        while (next()) {
            suraAyahSet.add(sura + ":" + ayah);
        }
        return suraAyahSet;
    }
}
