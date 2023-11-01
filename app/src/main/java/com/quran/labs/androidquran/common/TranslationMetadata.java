package com.quran.labs.androidquran.common;

import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import com.quran.data.model.SuraAyah;
import com.quran.labs.androidquran.ui.helpers.TranslationFootnoteHelper;
import java.util.List;

public class TranslationMetadata {
    private final int sura;
    private final int ayah;
    private final String text;
    private final Integer localTranslationId;
    private final SuraAyah link;
    private final Integer linkPageNumber;
    private final List<IntRange> ayat;
    private final List<IntRange> footnotes;

    public TranslationMetadata(
            int sura,
            int ayah,
            String text,
            Integer localTranslationId,
            SuraAyah link,
            Integer linkPageNumber,
            List<IntRange> ayat,
            List<IntRange> footnotes) {
        this.sura = sura;
        this.ayah = ayah;
        this.text = text;
        this.localTranslationId = localTranslationId;
        this.link = link;
        this.linkPageNumber = linkPageNumber;
        this.ayat = ayat;
        this.footnotes = footnotes;
    }

    public CharSequence footnoteCognizantText(
            SpannableStringBuilder spannableStringBuilder,
            List<Integer> expandedFootnotes,
            TranslationFootnoteHelper.CollapsedFootnoteSpannableStyler collapsedFootnoteSpannableStyler,
            TranslationFootnoteHelper.ExpandedFootnoteSpannableStyler expandedFootnoteSpannableStyler) {
        return TranslationFootnoteHelper.footnoteCognizantText(
                text,
                footnotes,
                spannableStringBuilder,
                expandedFootnotes,
                collapsedFootnoteSpannableStyler,
                expandedFootnoteSpannableStyler
        );
    }
}
