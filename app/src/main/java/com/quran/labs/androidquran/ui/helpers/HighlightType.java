import androidx.annotation.ColorRes;

public class HighlightType implements Comparable<HighlightType> {
    private long id;
    private int colorResId;
    private Mode mode;
    private boolean isSingle;
    private boolean isTransitionAnimated;

    public HighlightType(long id, int colorResId, Mode mode, boolean isSingle, boolean isTransitionAnimated) {
        this.id = id;
        this.colorResId = colorResId;
        this.mode = mode;
        this.isSingle = isSingle;
        this.isTransitionAnimated = isTransitionAnimated;
    }

    public enum Mode {
        HIGHLIGHT,  // Highlights the text of the ayah (rectangular overlay on the text)
        BACKGROUND, // Applies a background color to the entire line (full height/width, even ayahs that are centered like first 2 pages)
        UNDERLINE,  // Draw an underline below the text of the ayah
        COLOR,      // Change the text color of the ayah/word (apply a color filter)
        HIDE        // Hide the ayah/word (i.e. won't be rendered)
    }

    @Override
    public int compareTo(HighlightType other) {
        return Long.compare(id, other.id);
    }
}
