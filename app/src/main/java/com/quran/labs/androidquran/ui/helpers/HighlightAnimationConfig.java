import android.animation.TimeInterpolator;
import android.animation.TypeEvaluator;
import android.view.animation.AccelerateDecelerateInterpolator;

public abstract class HighlightAnimationConfig {
    public final int duration;
    public final TypeEvaluator<?> typeEvaluator;
    public final TimeInterpolator interpolator;

    public HighlightAnimationConfig(int duration, TypeEvaluator<?> typeEvaluator, TimeInterpolator interpolator) {
        this.duration = duration;
        this.typeEvaluator = typeEvaluator;
        this.interpolator = interpolator;
    }

    public static class None extends HighlightAnimationConfig {
        public None() {
            super(0, null, null);
        }
    }

    public static class Audio extends HighlightAnimationConfig {
        public Audio() {
            super(500, new HighlightAnimationTypeEvaluator(new NormalizeToMinAyahBoundsWithGrowingDivisionStrategy()), new AccelerateDecelerateInterpolator());
        }
    }
}
