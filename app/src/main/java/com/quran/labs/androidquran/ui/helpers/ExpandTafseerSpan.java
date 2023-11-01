import android.text.style.ClickableSpan;
import android.view.View;

public class ExpandTafseerSpan extends ClickableSpan {
    private final View.OnClickListener listener;

    public ExpandTafseerSpan(View.OnClickListener listener) {
        this.listener = listener;
    }

    @Override
    public void onClick(View widget) {
        listener.onClick(widget);
    }
}
