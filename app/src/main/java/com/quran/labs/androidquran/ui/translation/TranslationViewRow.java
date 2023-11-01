package com.quran.labs.androidquran.ui.translation;

import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import androidx.annotation.IntDef;
import com.quran.data.model.SuraAyah;
import com.quran.labs.androidquran.common.QuranAyahInfo;
import com.quran.labs.androidquran.ui.helpers.TranslationFootnoteHelper;
import java.util.List;

public class TranslationViewRow {
    public static final int BASMALLAH = 0;
    public static final int SURA_HEADER = 1;
    public static final int QURAN_TEXT = 2;
    public static final int TRANSLATOR = 3;
    public static final int TRANSLATION_TEXT = 4;
    public static final int VERSE_NUMBER = 5;
    public static final int SPACER = 6;

    @IntDef({BASMALLAH, SURA_HEADER, QURAN_TEXT, TRANSLATOR, TRANSLATION_TEXT, VERSE_NUMBER, SPACER})
    public @interface Type {
    }

    private final int type;
    private final QuranAyahInfo ayahInfo;
    private final CharSequence data;
    private final int translationIndex;
    private final SuraAyah link;
    private final Integer linkPage;
    private final boolean isArabic;
    private final List<IntRange> ayat;
    private final List<IntRange> footnotes;

    public TranslationViewRow(
        int type,
        QuranAyahInfo ayahInfo,
        CharSequence data,
        int translationIndex,
        SuraAyah link,
        Integer linkPage,
        boolean isArabic,
        List<IntRange> ayat,
        List<IntRange> footnotes
    ) {
        this.type = type;
        this.ayahInfo = ayahInfo;
        this.data = data;
        this.translationIndex = translationIndex;
        this.link = link;
        this.linkPage = linkPage;
        this.isArabic = isArabic;
        this.ayat = ayat;
        this.footnotes = footnotes;
    }

    public CharSequence footnoteCognizantText(
        SpannableStringBuilder spannableStringBuilder,
        List<Integer> expandedFootnotes,
        CollapsedFootnoteSpannableStyler collapsedFootnoteSpannableStyler,
        ExpandedFootnoteSpannableStyler expandedFootnoteSpannableStyler
    ) {
        return TranslationFootnoteHelper.footnoteCognizantText(
            data,
            footnotes,
            spannableStringBuilder,
            expandedFootnotes,
            collapsedFootnoteSpannableStyler,
            expandedFootnoteSpannableStyler
        );
    }

    public interface CollapsedFootnoteSpannableStyler {
        SpannableString collapsedFootnoteSpannableStyle(int footnoteIndex);
    }

    public interface ExpandedFootnoteSpannableStyler {
        SpannableStringBuilder expandedFootnoteSpannableStyle(
            SpannableStringBuilder originalText,
            int footnoteStart,
            int footnoteEnd
        );
    }
}
