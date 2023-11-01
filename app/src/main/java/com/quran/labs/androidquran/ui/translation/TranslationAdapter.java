import android.content.Context;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.SuperscriptSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.LayoutRes;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.quran.common.search.SearchTextUtil;
import com.quran.data.model.SuraAyah;
import com.quran.data.model.highlight.HighlightType;
import com.quran.labs.androidquran.R;
import com.quran.labs.androidquran.common.QuranAyahInfo;
import com.quran.labs.androidquran.model.translation.ArabicDatabaseUtils;
import com.quran.labs.androidquran.ui.helpers.ExpandFootnoteSpan;
import com.quran.labs.androidquran.ui.helpers.ExpandTafseerSpan;
import com.quran.labs.androidquran.ui.helpers.HighlightTypes;
import com.quran.labs.androidquran.ui.helpers.UthmaniSpan;
import com.quran.labs.androidquran.ui.util.TypefaceManager;
import com.quran.labs.androidquran.util.QuranSettings;
import com.quran.labs.androidquran.util.QuranUtils;
import com.quran.labs.androidquran.view.AyahNumberView;
import com.quran.labs.androidquran.view.DividerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TranslationAdapter extends RecyclerView.Adapter<TranslationAdapter.RowViewHolder> {

    private Context context;
    private RecyclerView recyclerView;
    private View.OnClickListener onClickListener;
    private OnVerseSelectedListener onVerseSelectedListener;
    private OnJumpToAyahListener onJumpToVerseListener;
    private LayoutInflater inflater;
    private List<TranslationViewRow> data;
    private int fontSize;
    private int textColor;
    private int footnoteColor;
    private int inlineAyahColor;
    private int dividerColor;
    private int arabicTextColor;
    private int suraHeaderColor;
    private int ayahSelectionColor;
    private boolean isNightMode;
    private int highlightedAyah;
    private int highlightedRowCount;
    private int highlightedStartPosition;
    private HighlightType highlightType;
    private Set<Pair<Integer, Integer>> expandedTafseerAyahs;
    private Set<Pair<Integer, Integer>> expandedHyperlinks;
    private Set<Pair<Integer, Integer>> expandedFootnotes;

    // Constants
    private static final float ARABIC_MULTIPLIER = 1.4f;
    private static final int MAX_TAFSEER_LENGTH = 750;
    private static final int HIGHLIGHT_CHANGE = 1;

    public TranslationAdapter(
            Context context,
            RecyclerView recyclerView,
            View.OnClickListener onClickListener,
            OnVerseSelectedListener onVerseSelectedListener,
            OnJumpToAyahListener onJumpToVerseListener
    ) {
        this.context = context;
        this.recyclerView = recyclerView;
        this.onClickListener = onClickListener;
        this.onVerseSelectedListener = onVerseSelectedListener;
        this.onJumpToVerseListener = onJumpToVerseListener;
        this.inflater = LayoutInflater.from(context);
        this.data = new ArrayList<>();
        this.expandedTafseerAyahs = new HashSet<>();
        this.highlightedAyah = 0;
        this.highlightedRowCount = 0;
        this.highlightedStartPosition = -1;
    }

    public void setData(List<TranslationViewRow> data) {
        this.data.clear();
        expandedTafseerAyahs.clear();
        this.data.addAll(data);
        if (highlightedAyah > 0) {
            highlightAyah(highlightedAyah, true, highlightType != null ? highlightType : HighlightTypes.SELECTION, true);
        }
    }

    public void setHighlightedAyah(int ayahId, HighlightType highlightType) {
        highlightAyah(ayahId, true, highlightType);
    }

    public QuranAyahInfo highlightedAyahInfo() {
        for (TranslationViewRow row : data) {
            if (row.ayahInfo.ayahId == highlightedAyah) {
                return row.ayahInfo;
            }
        }
        return null;
    }

    public void unhighlight() {
        if (highlightedAyah > 0 && highlightedRowCount > 0) {
            int start = highlightedStartPosition;
            int count = highlightedRowCount;
            notifyItemRangeChanged(start, count);
        }
        highlightedAyah = 0;
        highlightedRowCount = 0;
        highlightedStartPosition = -1;
    }

    public void refresh(QuranSettings quranSettings) {
        this.fontSize = quranSettings.getTranslationTextSize();
        this.isNightMode = quranSettings.isNightMode();
        if (isNightMode) {
            float originalTextBrightness = quranSettings.getNightModeTextBrightness();
            float backgroundBrightness = quranSettings.getNightModeBackgroundBrightness();
            int adjustedBrightness = (int) (50 * Math.log1p(backgroundBrightness) + originalTextBrightness);
            int textBrightness = Math.min(adjustedBrightness, 255);
            this.textColor = Color.rgb(textBrightness, textBrightness, textBrightness);
            this.footnoteColor = ContextCompat.getColor(context, R.color.translation_footnote_color);
            this.arabicTextColor = textColor;
            this.dividerColor = textColor;
            this.suraHeaderColor = ContextCompat.getColor(context, R.color.translation_sura_header_night);
            this.ayahSelectionColor = ContextCompat.getColor(context, R.color.translation_ayah_selected_color_night);
        } else {
            this.textColor = ContextCompat.getColor(context, R.color.translation_text_color);
            this.footnoteColor = ContextCompat.getColor(context, R.color.translation_footnote_color);
            this.dividerColor = ContextCompat.getColor(context, R.color.translation_divider_color);
            this.arabicTextColor = Color.BLACK;
            this.suraHeaderColor = ContextCompat.getColor(context, R.color.translation_sura_header);
            this.ayahSelectionColor = ContextCompat.getColor(context, R.color.translation_ayah_selected_color);
        }
        this.inlineAyahColor = ContextCompat.getColor(context, R.color.translation_translator_color);

        if (!this.data.isEmpty()) {
            notifyDataSetChanged();
        }
    }

    private void handleClick(View view) {
        int position = recyclerView.getChildAdapterPosition(view);
        if (highlightedAyah != 0 && position != RecyclerView.NO_POSITION) {
            QuranAyahInfo ayahInfo = data.get(position).ayahInfo;
            if (ayahInfo.ayahId != highlightedAyah && highlightType == HighlightTypes.SELECTION) {
                onVerseSelectedListener.onVerseSelected(ayahInfo);
                return;
            }
        }
        onClickListener.onClick(view);
    }

    private boolean selectVerseRows(View view) {
        int position = recyclerView.getChildAdapterPosition(view);
        if (position != RecyclerView.NO_POSITION) {
            QuranAyahInfo ayahInfo = data.get(position).ayahInfo;
            highlightAyah(ayahInfo.ayahId, true, HighlightTypes.SELECTION);
            onVerseSelectedListener.onVerseSelected(ayahInfo);
            return true;
        }
        return false;
    }

    private void toggleExpandTafseer(View view) {
        int position = recyclerView.getChildAdapterPosition(view);
        if (position != RecyclerView.NO_POSITION) {
            TranslationViewRow data = this.data.get(position);
            Pair<Integer, Integer> what = new Pair<>(data.ayahInfo.ayahId, data.translationIndex);
            if (expandedTafseerAyahs.contains(what)) {
                expandedTafseerAyahs.remove(what);
            } else {
                expandedTafseerAyahs.add(what);
            }
            notifyItemChanged(position);
        }
    }

    private void toggleTafseerJump(View view) {
        int position = recyclerView.getChildAdapterPosition(view);
        if (position != RecyclerView.NO_POSITION) {
            TranslationViewRow item = data.get(position);
            SuraAyah targetAyah = item.link;
            Integer targetPage = item.linkPage;
            if (targetAyah != null && targetPage != null) {
                int match = -1;
                for (int i = 0; i < data.size(); i++) {
                    if (data.get(i).ayahInfo.asSuraAyah().equals(targetAyah)) {
                        match = i;
                        break;
                    }
                }
                if (match > -1) {
                    recyclerView.smoothScrollToPosition(match);
                } else {
                    onJumpToVerseListener.onJumpToAyah(targetAyah, targetPage);
                }
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        return data.get(position).type;
    }

    @Override
    public RowViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        @LayoutRes int layout;
        switch (viewType) {
            case TranslationViewRow.Type.SURA_HEADER:
                layout = R.layout.quran_translation_header_row;
                break;
            case TranslationViewRow.Type.BASMALLAH:
            case TranslationViewRow.Type.QURAN_TEXT:
                layout = R.layout.quran_translation_arabic_row;
                break;
            case TranslationViewRow.Type.SPACER:
                layout = R.layout.quran_translation_spacer_row;
                break;
            case TranslationViewRow.Type.VERSE_NUMBER:
                layout = R.layout.quran_translation_verse_number_row;
                break;
            case TranslationViewRow.Type.TRANSLATOR:
                layout = R.layout.quran_translation_translator_row;
                break;
            default:
                layout = R.layout.quran_translation_text_row;
                break;
        }

        View view = inflater.inflate(layout, parent, false);
        return new RowViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RowViewHolder holder, int position) {
        TranslationViewRow row = data.get(position);
        if (holder.text != null) {
            holder.text.setOnClickListener(onClickListener);
            CharSequence text;
            if (row.type == TranslationViewRow.Type.SURA_HEADER) {
                text = row.data;
                holder.text.setBackgroundColor(suraHeaderColor);
            } else if (row.type == TranslationViewRow.Type.BASMALLAH || row.type == TranslationViewRow.Type.QURAN_TEXT) {
                SpannableString str = new SpannableString(
                        (row.type == TranslationViewRow.Type.BASMALLAH)
                                ? ArabicDatabaseUtils.AR_BASMALLAH
                                : ArabicDatabaseUtils.getAyahWithoutBasmallah(
                                row.ayahInfo.sura, row.ayahInfo.ayah, row.ayahInfo.arabicText
                        )
                );
                str.setSpan(new UthmaniSpan(context), 0, str.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                text = str;
                holder.text.setTextColor(arabicTextColor);
                holder.text.setTextSize(ARABIC_MULTIPLIER * fontSize);
            } else {
                if (row.type == TranslationViewRow.Type.TRANSLATOR) {
                    text = row.data;
                } else {
                    text = row.data != null ? row.data : "";
                    int length = text.length();
                    boolean expandHyperlink = expandedHyperlinks.contains(new Pair<>(row.ayahInfo.ayahId, row.translationIndex));
                    if (row.link != null && !expandHyperlink) {
                        holder.text.setOnClickListener(expandHyperlinkClickListener);
                    }
                    SpannableStringBuilder spannableBuilder = new SpannableStringBuilder(text);

                    for (Pair<Integer, Integer> range : row.ayat) {
                        ForegroundColorSpan span = new ForegroundColorSpan(inlineAyahColor);
                        spannableBuilder.setSpan(span, range.first, range.second + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }

                    List<Integer> expandedFootnotes = expandedFootnotes.get(row.ayahInfo);
                    CharSequence spannable = new SpannableString(
                            row.footnoteCognizantText(spannableBuilder, expandedFootnotes, this::collapsedFootnoteSpan, this::expandedFootnote)
                    );
                    if (row.link != null && !expandHyperlink) {
                        spannable = getAyahLink(row.link);
                    } else if (length > MAX_TAFSEER_LENGTH) {
                        spannable = truncateTextIfNeeded(spannable, row.ayahInfo.ayahId, row.translationIndex);
                    }

                    boolean isRtl;
                    if (row.type == TranslationViewRow.Type.TRANSLATION) {
                        isRtl = row.isRtl();
                    } else {
                        isRtl = QuranUtils.isRtl(row.ayahInfo.sura);
                    }
                    text = formatText(spannable, isRtl);
                    holder.text.setOnClickListener(null);
                }
                if (row.ayahInfo.ayahId == highlightedAyah && highlightType == HighlightTypes.SELECTION) {
                    holder.text.setBackgroundColor(ayahSelectionColor);
                } else {
                    holder.text.setBackgroundColor(Color.TRANSPARENT);
                }
            }
            holder.text.setText(text);
        }
        holder.linkView.setTag(row);
        holder.linkView.setOnClickListener(row.linkClickListener);
    }

    private void collapsedFootnoteSpan(View view) {
        int position = recyclerView.getChildAdapterPosition(view);
        if (position != RecyclerView.NO_POSITION) {
            Pair<Integer, Integer> what = new Pair<>(data.get(position).ayahInfo.ayahId, data.get(position).translationIndex);
            if (expandedFootnotes.contains(what)) {
                expandedFootnotes.remove(what);
            }
            notifyItemChanged(position);
        }
    }

    private void expandedFootnote(View view) {
        int position = recyclerView.getChildAdapterPosition(view);
        if (position != RecyclerView.NO_POSITION) {
            Pair<Integer, Integer> what = new Pair<>(data.get(position).ayahInfo.ayahId, data.get(position).translationIndex);
            expandedFootnotes.add(what);
            notifyItemChanged(position);
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void highlightAyah(int ayahId, boolean scrollToAyah, HighlightType highlightType) {
        highlightAyah(ayahId, scrollToAyah, highlightType, false);
    }

    public void highlightAyah(int ayahId, boolean scrollToAyah, HighlightType highlightType, boolean isInitialLoad) {
        int newHighlightPosition = -1;
        int highlightStart = -1;
        int highlightCount = -1;
        for (int i = 0; i < data.size(); i++) {
            TranslationViewRow row = data.get(i);
            if (row.ayahInfo.ayahId == ayahId) {
                newHighlightPosition = i;
                if (highlightStart == -1) {
                    highlightStart = i;
                }
                highlightCount = 1;
            }
        }
        if (newHighlightPosition != -1) {
            if (highlightedAyah != 0 && highlightedStartPosition != -1 && highlightedRowCount != 0) {
                notifyItemRangeChanged(highlightedStartPosition, highlightedRowCount);
            }
            highlightedAyah = ayahId;
            highlightedStartPosition = highlightStart;
            highlightedRowCount = highlightCount;
            this.highlightType = highlightType;
            if (scrollToAyah) {
                recyclerView.smoothScrollToPosition(newHighlightPosition);
            }
            notifyItemRangeChanged(newHighlightPosition, 1);
            if (!isInitialLoad) {
                onVerseSelectedListener.onVerseSelected(data.get(newHighlightPosition).ayahInfo);
            }
        } else if (highlightedAyah != 0 && highlightedStartPosition != -1 && highlightedRowCount != 0) {
            notifyItemRangeChanged(highlightedStartPosition, highlightedRowCount);
            highlightedAyah = 0;
            highlightedStartPosition = -1;
            highlightedRowCount = 0;
            this.highlightType = null;
        }
    }

    private CharSequence getAyahLink(SuraAyah suraAyah) {
        String linkText = context.getString(R.string.link);
        String ayahLink = QuranUtils.getSuraAyahString(suraAyah);
        return SearchTextUtil.getSpannable(linkText + " (" + ayahLink + ")", true, inlineAyahColor, highlightType == HighlightTypes.LINK ? ayahSelectionColor : textColor);
    }

    private SpannableString truncateTextIfNeeded(CharSequence text, int ayahId, int translationIndex) {
        String moreText = context.getString(R.string.more);
        String lessText = context.getString(R.string.less);
        SpannableString spannable = new SpannableString(text.toString());
        spannable.setSpan(new RelativeSizeSpan(0.7f), text.length() - moreText.length(), text.length(), 0);
        spannable.setSpan(new SuperscriptSpan(), text.length() - moreText.length(), text.length(), 0);
        spannable.setSpan(new ForegroundColorSpan(inlineAyahColor), text.length() - moreText.length(), text.length(), 0);
        spannable.setSpan(new ExpandTafseerSpan(moreText, lessText, this::toggleExpandTafseer, inlineAyahColor), text.length() - moreText.length(), text.length(), 0);
        return spannable;
    }

    private CharSequence formatText(CharSequence text, boolean isRtl) {
        TypefaceManager manager = TypefaceManager.getInstance(context);
        if (isRtl) {
            manager.setSurahNames(holder.translationTypeface);
            manager.setTranslationTextDirection(holder.translationTextDirection);
            return SearchTextUtil.fixText(text, isRtl, textColor, arabicTextColor);
        } else {
            manager.setSurahNames(null);
            manager.setTranslationTextDirection(null);
            return SearchTextUtil.getSpannable(text, true, textColor, arabicTextColor);
        }
    }

    public interface OnVerseSelectedListener {
        void onVerseSelected(QuranAyahInfo ayah);
    }

    public interface OnJumpToAyahListener {
        void onJumpToAyah(SuraAyah suraAyah, int page);
    }

    static class RowViewHolder extends RecyclerView.ViewHolder {
        TextView text;
        AyahNumberView linkView;
        TranslationViewRow.Type type;

        RowViewHolder(View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.text);
            linkView = itemView.findViewById(R.id.link_view);
            type = TranslationViewRow.Type.fromId(itemView.getId());
        }
    }
}
