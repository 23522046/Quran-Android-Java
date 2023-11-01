package com.quran.labs.androidquran.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.StyleRes;
import androidx.core.content.ContextCompat;

import com.quran.labs.androidquran.R;
import com.quran.labs.androidquran.common.LocalTranslation;
import com.quran.labs.androidquran.common.QuranAyahInfo;
import com.quran.labs.androidquran.common.TranslationMetadata;
import com.quran.labs.androidquran.util.QuranSettings;

public class InlineTranslationView extends ScrollView {
    private int leftRightMargin = 0;
    private int topBottomMargin = 0;

    @StyleRes
    private int textStyle = 0;
    private int fontSize = 0;
    private int footerSpacerHeight = 0;
    private int inlineAyahColor;

    private LinearLayout linearLayout;

    private List<QuranAyahInfo> ayat;
    private LocalTranslation[] translations;

    public InlineTranslationView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public InlineTranslationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public InlineTranslationView(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        setFillViewport(true);
        linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        addView(linearLayout, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        leftRightMargin = getResources().getDimensionPixelSize(R.dimen.translation_left_right_margin);
        topBottomMargin = getResources().getDimensionPixelSize(R.dimen.translation_top_bottom_margin);
        footerSpacerHeight = getResources().getDimensionPixelSize(R.dimen.translation_footer_spacer);
        initResources();
    }

    private void initResources() {
        QuranSettings settings = QuranSettings.getInstance(getContext());
        fontSize = settings.getTranslationTextSize();
        textStyle = R.style.TranslationText;
        inlineAyahColor = ContextCompat.getColor(getContext(), R.color.translation_translator_color);
    }

    public void refresh() {
        if (ayat != null && translations != null) {
            initResources();
            setAyahs(translations, ayat);
        }
    }

    public void setAyahs(LocalTranslation[] translations, List<QuranAyahInfo> ayat) {
        linearLayout.removeAllViews();
        if (!ayat.isEmpty() && ayat.get(0).getTexts().size() > 0) {
            this.ayat = ayat;
            this.translations = translations;
            int i = 0;
            int ayatSize = ayat.size();
            while (i < ayatSize) {
                addTextForAyah(translations, ayat.get(i));
                i++;
            }
            addFooterSpacer();
            scrollTo(0, 0);
        }
    }

    private void addFooterSpacer() {
        View view = new View(getContext());
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, footerSpacerHeight);
        linearLayout.addView(view, params);
    }

    private void addTextForAyah(LocalTranslation[] translations, QuranAyahInfo ayah) {
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params.setMargins(leftRightMargin, topBottomMargin, leftRightMargin, topBottomMargin);
        int suraNumber = ayah.getSura();
        int ayahNumber = ayah.getAyah();
        TextView ayahHeader = new TextView(getContext());
        ayahHeader.setTextColor(Color.WHITE);
        ayahHeader.setTextSize(fontSize);
        ayahHeader.setTypeface(null, Typeface.BOLD);
        ayahHeader.setText(getResources().getString(R.string.sura_ayah, suraNumber, ayahNumber));
        linearLayout.addView(ayahHeader, params);
        TextView ayahView = new TextView(getContext());
        ayahView.setTextAppearance(getContext(), textStyle);
        ayahView.setTextColor(Color.WHITE);
        ayahView.setTextSize(fontSize);

        // translation
        boolean showHeader = translations.length > 1;
        SpannableStringBuilder builder = new SpannableStringBuilder();
        for (int i = 0; i < translations.length; i++) {
            String translationText = ayah.getTexts().get(i).getTranslationText();
            if (!TextUtils.isEmpty(translationText)) {
                if (showHeader) {
                    if (i > 0) {
                        builder.append("\n\n");
                    }
                    int start = builder.length();
                    builder.append(translations[i].getTranslatorName());
                    builder.setSpan(new StyleSpan(Typeface.BOLD),
                            start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    builder.append("\n\n");
                }

                // irrespective of whether it's a link or not, show the text
                builder.append(stylize(ayah.getTexts().get(i), translationText));
            }
        }
        ayahView.append(builder);
        params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params.setMargins(leftRightMargin, topBottomMargin, leftRightMargin, topBottomMargin);
        ayahView.setTextIsSelectable(true);
        linearLayout.addView(ayahView, params);
    }

    private SpannableString collapsedFootnoteSpan(int number) {
        return new SpannableString("");
    }

    private SpannableStringBuilder expandedFootnote(SpannableStringBuilder spannableStringBuilder, int start, int end) {
        return spannableStringBuilder;
    }

    private CharSequence stylize(TranslationMetadata metadata, String translationText) {
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(translationText);

        for (Pair<Integer, Integer> range : metadata.getAyat()) {
            ForegroundColorSpan span = new ForegroundColorSpan(inlineAyahColor);
            spannableStringBuilder.setSpan(
                    span,
                    range.first,
                    range.second + 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        return metadata.footnoteCognizantText(
                spannableStringBuilder,
                Collections.emptyList(),
                this::collapsedFootnoteSpan,
                this::expandedFootnote
        );
    }
}
