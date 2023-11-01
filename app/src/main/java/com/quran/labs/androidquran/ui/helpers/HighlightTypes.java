import com.quran.data.model.highlight.HighlightType;
import com.quran.labs.androidquran.R;

public class HighlightTypes {
    public static final HighlightType SELECTION = new HighlightType(1, R.color.selection_highlight, HighlightType.Mode.HIGHLIGHT, true);
    public static final HighlightType AUDIO = new HighlightType(2, R.color.audio_highlight, HighlightType.Mode.HIGHLIGHT, true, true);
    public static final HighlightType NOTE = new HighlightType(3, R.color.note_highlight, HighlightType.Mode.HIGHLIGHT, false);
    public static final HighlightType BOOKMARK = new HighlightType(4, R.color.bookmark_highlight, HighlightType.Mode.HIGHLIGHT, false);

    public static HighlightAnimationConfig getAnimationConfig(HighlightType type) {
        if (type == AUDIO) {
            return HighlightAnimationConfig.Audio;
        } else {
            return HighlightAnimationConfig.None;
        }
    }
}
