package com.quran.labs.androidquran.util;

import com.quran.data.core.QuranInfo;
import com.quran.data.model.QuranText;
import com.quran.labs.androidquran.common.TranslationMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dagger.Reusable;

@Reusable
public class TranslationUtil {

    private final QuranInfo quranInfo;

    public TranslationUtil(QuranInfo quranInfo) {
        this.quranInfo = quranInfo;
    }

    public TranslationMetadata parseTranslationText(QuranText quranText, int translationId) {
        String text = quranText.getText();
        Integer hyperlinkId = getHyperlinkAyahId(quranText);

        QuranInfo.SuraAyah suraAyah = null;
        if (hyperlinkId != null) {
            suraAyah = quranInfo.getSuraAyahFromAyahId(hyperlinkId);
        }

        Integer linkPage = null;
        if (suraAyah != null) {
            linkPage = quranInfo.getPageFromSuraAyah(suraAyah.getSura(), suraAyah.getAyah());
        }

        String extraData = quranText.getExtraData();
        String textToParse = (suraAyah != null && extraData != null) ? extraData : text;

        List<TranslationMetadata.Range> ayat = getRanges(ayahRegex, textToParse);
        List<TranslationMetadata.Range> footnotes = getRanges(footerRegex, textToParse);

        return new TranslationMetadata(
                quranText.getSura(),
                quranText.getAyah(),
                textToParse,
                translationId,
                suraAyah,
                linkPage,
                ayat,
                footnotes
        );
    }

    private List<TranslationMetadata.Range> getRanges(Pattern pattern, String input) {
        List<TranslationMetadata.Range> ranges = new ArrayList<>();
        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            ranges.add(new TranslationMetadata.Range(matcher.start(), matcher.end()));
        }
        return ranges;
    }

    private static final Pattern ayahRegex = Pattern.compile("([«{﴿][\\s\\S]*?[﴾}»])");
    private static final Pattern footerRegex = Pattern.compile("\\[\\[[\\s\\S]*?]]");
    public static final int MINIMUM_PROCESSING_VERSION = 5;

    public static Integer getHyperlinkAyahId(QuranText quranText) {
        String text = quranText.getText();
        if (text.length() < 5) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        return null;
    }
}
