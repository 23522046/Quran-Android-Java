import android.content.Context;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;
import com.quran.labs.androidquran.ui.util.TypefaceManager;

public class UthmaniSpan extends MetricAffectingSpan {

    private Typeface typeface;

    public UthmaniSpan(Context context) {
        this.typeface = TypefaceManager.getUthmaniTypeface(context);
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        ds.setTypeface(typeface);
    }

    @Override
    public void updateMeasureState(TextPaint paint) {
        paint.setTypeface(typeface);
    }
}
