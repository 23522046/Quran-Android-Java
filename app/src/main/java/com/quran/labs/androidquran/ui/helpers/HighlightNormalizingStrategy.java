import android.graphics.RectF;
import com.quran.page.common.data.AyahBounds;

import java.util.List;

abstract class HighlightNormalizingStrategy {
    abstract void normalize(List<AyahBounds> start, List<AyahBounds> end);
    abstract boolean isNormalized(List<AyahBounds> start, List<AyahBounds> end);

    void apply(List<AyahBounds> start, List<AyahBounds> end) {
        if (isNormalized(start, end)) return;
        normalize(start, end);
    }
}

class NormalizeToMaxAyahBoundsWithDivisionStrategy extends HighlightNormalizingStrategy {

    @Override
    void normalize(List<AyahBounds> start, List<AyahBounds> end) {
        int startSize = start.size();
        int endSize = end.size();
        int minSize = Math.min(startSize, endSize);
        int maxSize = Math.max(startSize, endSize);
        List<AyahBounds> minList = (startSize < endSize) ? start : end;
        int diff = maxSize - minSize;

        RectF rectToBeDivided = minList.get(minSize - 1).bounds;
        float originalLeft = rectToBeDivided.left;
        float originalRight = rectToBeDivided.right;
        float originalTop = rectToBeDivided.top;
        float originalBottom = rectToBeDivided.bottom;
        minList.remove(minSize - 1);
        float part = (originalRight - originalLeft) / (diff + 1);

        for (int i = 0; i < diff + 1; i++) {
            float left = originalLeft + part * i;
            float right = left + part;
            RectF rect = new RectF(left, originalTop, right, originalBottom);
            AyahBounds ayahBounds = new AyahBounds(0, 0, rect);
            minList.add(ayahBounds);
        }
    }

    @Override
    boolean isNormalized(List<AyahBounds> start, List<AyahBounds> end) {
        return start.size() == end.size();
    }
}

class NormalizeToMinAyahBoundsWithGrowingDivisionStrategy extends NormalizeToMaxAyahBoundsWithDivisionStrategy {

    @Override
    void normalize(List<AyahBounds> start, List<AyahBounds> end) {
        int startSize = start.size();
        int endSize = end.size();

        if (startSize >= endSize) {
            int diff = Math.abs(startSize - endSize);
            List<AyahBounds> toBeDeleted = start.subList(0, diff);
            toBeDeleted.clear();
        } else {
            super.normalize(start, end);
        }
    }
}
