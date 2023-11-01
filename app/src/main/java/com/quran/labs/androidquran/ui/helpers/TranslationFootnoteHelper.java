import android.text.SpannableString;
import android.text.SpannableStringBuilder;

import java.util.Collections;
import java.util.List;

public class TranslationFootnoteHelper {

    public static CharSequence footnoteCognizantText(
            CharSequence data,
            List<IntRange> footnotes,
            SpannableStringBuilder spannableStringBuilder,
            List<Integer> expandedFootnotes,
            Function1<Integer, SpannableString> collapsedFootnoteSpannableStyler,
            Function2<SpannableStringBuilder, Integer, Integer, SpannableStringBuilder> expandedFootnoteSpannableStyler) {

        if (data != null) {
            List<IntRange> ranges = footnotes;
            Collections.sort(ranges, (o1, o2) -> o2.last - o1.last);

            for (int index = 0; index < ranges.size(); index++) {
                IntRange range = ranges.get(index);
                int number = ranges.size() - index;
                if (!expandedFootnotes.contains(number)) {
                    spannableStringBuilder.replace(
                            range.getFirst(),
                            range.getLast() + 1,
                            collapsedFootnoteSpannableStyler.invoke(number)
                    );
                } else {
                    expandedFootnoteSpannableStyler.invoke(spannableStringBuilder, range.getFirst(), range.getLast() + 1);
                }
            }
            return spannableStringBuilder;
        } else {
            return "";
        }
    }
}
