package com.quran.labs.androidquran.util;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import androidx.annotation.StringRes;
import com.quran.data.model.QuranText;
import com.quran.labs.androidquran.R;
import com.quran.labs.androidquran.common.LocalTranslation;
import com.quran.labs.androidquran.common.QuranAyahInfo;
import com.quran.labs.androidquran.data.QuranDisplayData;
import com.quran.labs.androidquran.model.translation.ArabicDatabaseUtils;
import com.quran.labs.androidquran.ui.util.ToastCompat;
import dagger.Reusable;
import java.text.NumberFormat;
import java.util.Locale;
import javax.inject.Inject;

@Reusable
public class ShareUtil {

    private final QuranDisplayData quranDisplayData;

    @Inject
    public ShareUtil(QuranDisplayData quranDisplayData) {
        this.quranDisplayData = quranDisplayData;
    }

    public void copyVerses(Activity activity, List<QuranText> verses) {
        String text = getShareText(activity, verses);
        copyToClipboard(activity, text);
    }

    public void copyToClipboard(Activity activity, String text) {
        ClipboardManager cm = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(activity.getString(R.string.app_name), text);
        if (cm != null) {
            cm.setPrimaryClip(clip);
            ToastCompat.makeText(activity, activity.getString(R.string.ayah_copied_popup), Toast.LENGTH_SHORT).show();
        }
    }

    public void shareVerses(Activity activity, List<QuranText> verses) {
        String text = getShareText(activity, verses);
        shareViaIntent(activity, text, com.quran.labs.androidquran.common.toolbar.R.string.share_ayah_text);
    }

    public void shareViaIntent(Activity activity, String text, @StringRes int titleResId) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        activity.startActivity(Intent.createChooser(intent, activity.getString(titleResId)));
    }

    public String getShareText(Context context, QuranAyahInfo ayahInfo, LocalTranslation[] translationNames) {
        StringBuilder builder = new StringBuilder();
        ayahInfo.getArabicText().ifPresent(arabicText -> {
            builder.append("{ ");
            builder.append(ArabicDatabaseUtils.getAyahWithoutBasmallah(
                    ayahInfo.getSura(), ayahInfo.getAyah(), arabicText.trim()));
            builder.append(" }");
            builder.append("\n");
            builder.append("[");
            builder.append(quranDisplayData.getSuraAyahString(
                    context, ayahInfo.getSura(), ayahInfo.getAyah(), R.string.sura_ayah_sharing_str));
            builder.append("]");
        });

        ayahInfo.getTexts().forEach((i, translation) -> {
            String text = translation.getText();
            if (!text.isEmpty()) {
                builder.append("\n\n");
                if (i < translationNames.length) {
                    builder.append(translationNames[i].getTranslatorName());
                    builder.append(":\n");
                }
                builder.append(text);
            }
        });

        if (!ayahInfo.getArabicText().isPresent()) {
            builder.append("\n");
            builder.append("-");
            builder.append(quranDisplayData.getSuraAyahString(
                    context, ayahInfo.getSura(), ayahInfo.getAyah(), R.string.sura_ayah_notification_str));
        }

        return builder.toString();
    }

    private String getShareText(Activity activity, List<QuranText> verses) {
        int size = verses.size();
        boolean wantInlineAyahNumbers = size > 1;
        boolean isArabicNames = QuranSettings.getInstance(activity).isArabicNames();
        Locale locale = isArabicNames ? new Locale("ar") : Locale.getDefault();
        NumberFormat numberFormat = NumberFormat.getNumberInstance(locale);

        StringBuilder builder = new StringBuilder();
        builder.append("{ ");
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                builder.append(" ");
            }

            QuranText verse = verses.get(i);
            builder.append(ArabicDatabaseUtils.getAyahWithoutBasmallah(
                    verse.getSura(), verse.getAyah(), verse.getText().trim()));
            if (wantInlineAyahNumbers) {
                builder.append(" (");
                builder.append(numberFormat.format(verse.getAyah()));
                builder.append(")");
            }
        }

        // append } and a new line after the last ayah
        builder.append(" }\n");
        // append [ before sura label
        builder.append("[");
        QuranText firstVerse = verses.get(0);
        builder.append(quranDisplayData.getSuraName(activity, firstVerse.getSura(), true));
        builder.append(": ");
        builder.append(numberFormat.format(firstVerse.getAyah()));
        if (size > 1) {
            QuranText lastVerse = verses.get(size - 1);
            if (firstVerse.getSura() != lastVerse.getSura()) {
                builder.append(" - ");
                builder.append(quranDisplayData.getSuraName(activity, lastVerse.getSura(), true));
                builder.append(": ");
            } else {
                builder.append("-");
            }
            builder.append(numberFormat.format(lastVerse.getAyah()));
        }
        // close sura label and append two new lines
        builder.append("]");

        return builder.toString();
    }
}
