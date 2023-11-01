import android.text.style.ClickableSpan;
import android.view.View;

public class ExpandFootnoteSpan extends ClickableSpan {
    private final int number;
    private final ExpandFootnoteExpander expander;

    public ExpandFootnoteSpan(int number, ExpandFootnoteExpander expander) {
        this.number = number;
        this.expander = expander;
    }

    @Override
    public void onClick(View widget) {
        expander.expand(widget, number);
    }

    public interface ExpandFootnoteExpander {
        void expand(View widget, int number);
    }
}
