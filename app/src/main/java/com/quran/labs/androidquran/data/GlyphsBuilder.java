package com.quran.labs.androidquran.data;

import android.graphics.RectF;
import com.quran.data.model.AyahGlyph.AyahEndGlyph;
import com.quran.data.model.AyahGlyph.HizbGlyph;
import com.quran.data.model.AyahGlyph.PauseGlyph;
import com.quran.data.model.AyahGlyph.SajdahGlyph;
import com.quran.data.model.AyahGlyph.WordGlyph;
import com.quran.data.model.SuraAyah;
import com.quran.page.common.data.coordinates.GlyphCoords;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper to convert the ordered glyph rows in the database to structured [GlyphCoords] classes.
 *
 * Usage: add glyphs to the builder in sequence so that word positions can be calculated correctly.
 *
 * Note: It is important that the glyphs are appended in sequence with no gaps,
 * otherwise the `wordPosition` for [WordGlyph] may be incorrect.
 */
public class GlyphsBuilder {
    private final List<GlyphCoords> glyphs = new ArrayList<>();
    private SuraAyah curAyah;
    private int nextWordPos;

    public void append(int sura, int ayah, int glyphPosition, int line, String type, RectF bounds) {
        SuraAyah suraAyah = new SuraAyah(sura, ayah);

        // If we're on a different ayah, reset word position to 1
        if (curAyah == null || !curAyah.equals(suraAyah)) {
            curAyah = suraAyah;
            nextWordPos = 1;
        }

        GlyphCoords glyph;
        if ("hizb".equals(type)) {
            glyph = new GlyphCoords(new HizbGlyph(suraAyah, glyphPosition), line, bounds);
        } else if ("sajdah".equals(type)) {
            glyph = new GlyphCoords(new SajdahGlyph(suraAyah, glyphPosition), line, bounds);
        } else if ("pause".equals(type)) {
            glyph = new GlyphCoords(new PauseGlyph(suraAyah, glyphPosition), line, bounds);
        } else if ("end".equals(type)) {
            glyph = new GlyphCoords(new AyahEndGlyph(suraAyah, glyphPosition), line, bounds);
        } else if ("word".equals(type)) {
            glyph = new GlyphCoords(new WordGlyph(suraAyah, glyphPosition, nextWordPos++), line, bounds);
        } else {
            throw new IllegalArgumentException("Unknown glyph type " + type);
        }

        glyphs.add(glyph);
    }

    public List<GlyphCoords> build() {
        return new ArrayList<>(glyphs);
    }

    // Glyph Type keys
    // Note: it's important these types match what is defined in the ayahinfo db
    private static final String HIZB = "hizb";
    private static final String SAJDAH = "sajdah";
    private static final String PAUSE = "pause";
    private static final String END = "end";
    private static final String WORD = "word";
}
