package com.quran.labs.androidquran.view;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.widget.ImageView;

import androidx.annotation.ColorInt;

import com.quran.page.common.data.AyahCoordinates;
import com.quran.page.common.data.PageCoordinates;
import com.quran.page.common.draw.ImageDrawHelper;

import java.util.Collection;

public class GlyphBoundsDebuggingDrawer implements ImageDrawHelper {

    private final AyahCoordinatesProvider ayahCoordinatesProvider;

    private final boolean debugExpandedLines = true;
    private final boolean debugLines = false;
    private final boolean debugExpandedGlyphs = true;
    private final boolean debugGlyphs = true;

    private final int gray = 0xFFCCCCCC;
    private final int yellow = 0xFFFFC107;
    private final int green = 0xFF4CAF50;

    public GlyphBoundsDebuggingDrawer(AyahCoordinatesProvider ayahCoordinatesProvider) {
        this.ayahCoordinatesProvider = ayahCoordinatesProvider;
    }

    @Override
    public void draw(PageCoordinates pageCoordinates, Canvas canvas, ImageView image) {
        AyahCoordinates ayahCoordinates = ayahCoordinatesProvider.getAyahCoordinates();
        if (ayahCoordinates == null) return;

        if (debugExpandedLines) {
            drawBounds(ayahCoordinates.getExpandedLineBounds().values(), canvas, image, gray);
        }

        if (debugLines) {
            drawBounds(ayahCoordinates.getLineBounds().values(), canvas, image, yellow);
        }

        if (debugExpandedGlyphs) {
            drawBounds(ayahCoordinates.getExpandedGlyphs(), canvas, image, yellow);
        }

        if (debugGlyphs) {
            drawBounds(ayahCoordinates.getGlyphsByLine().values().flatten(), canvas, image, green);
        }
    }

    private void drawBounds(Collection<RectF> bounds, Canvas canvas, ImageView image, @ColorInt int color) {
        drawBounds(bounds, canvas, image, new int[]{color, darkenColor(color)});
    }

    private void drawBounds(Collection<RectF> bounds, Canvas canvas, ImageView image, @ColorInt int[] colors) {
        RectF scaledRect = new RectF();
        Paint[] paints = new Paint[colors.length];
        for (int i = 0; i < colors.length; i++) {
            paints[i] = new Paint();
            paints[i].setColor(colors[i]);
            paints[i].setAlpha((int) (255 * 0.5f));
        }

        for (int idx = 0; idx < bounds.size(); idx++) {
            RectF rect = bounds.get(idx);
            image.getImageMatrix().mapRect(scaledRect, rect);
            scaledRect.offset(image.getPaddingLeft(), image.getPaddingTop());
            canvas.drawRect(scaledRect, paints[idx % paints.length]);
        }
    }

    private int darkenColor(@ColorInt int color, float factor) {
        int alpha = Color.alpha(color);
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] = hsv[2] * factor;
        return Color.HSVToColor(alpha, hsv);
    }

    public interface AyahCoordinatesProvider {
        AyahCoordinates getAyahCoordinates();
    }
}
