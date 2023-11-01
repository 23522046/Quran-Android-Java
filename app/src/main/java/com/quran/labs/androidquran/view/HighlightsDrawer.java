package com.quran.labs.androidquran.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.os.Build;
import android.util.TypedValue;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;

import com.quran.data.model.highlight.HighlightType;
import com.quran.data.model.highlight.HighlightType.Mode;
import com.quran.labs.androidquran.ui.helpers.AyahHighlight;
import com.quran.labs.androidquran.ui.helpers.AyahHighlight.TransitionAyahHighlight;
import com.quran.page.common.data.AyahBounds;
import com.quran.page.common.data.AyahCoordinates;
import com.quran.page.common.data.PageCoordinates;
import com.quran.page.common.draw.ImageDrawHelper;

import java.util.Map;
import java.util.SortedMap;

public class HighlightsDrawer implements ImageDrawHelper {

    private final AyahCoordinatesProvider ayahCoordinatesProvider;
    private final HighlightCoordinatesProvider highlightCoordinatesProvider;
    private final CurrentHighlightsProvider currentHighlightsProvider;
    private final OnDrawCallback superOnDraw;
    private final Mode[] highlightTypesFilter;

    private final RectF scaledRect = new RectF();
    private final Map<HighlightType.Mode, Set<AyahHighlight>> alreadyHighlighted = new java.util.HashMap<>();

    public HighlightsDrawer(
            AyahCoordinatesProvider ayahCoordinatesProvider,
            HighlightCoordinatesProvider highlightCoordinatesProvider,
            CurrentHighlightsProvider currentHighlightsProvider,
            OnDrawCallback superOnDraw,
            Mode... highlightTypesFilter) {
        this.ayahCoordinatesProvider = ayahCoordinatesProvider;
        this.highlightCoordinatesProvider = highlightCoordinatesProvider;
        this.currentHighlightsProvider = currentHighlightsProvider;
        this.superOnDraw = superOnDraw;
        this.highlightTypesFilter = highlightTypesFilter;
    }

    @Override
    public void draw(PageCoordinates pageCoordinates, Canvas canvas, ImageView image) {
        AyahCoordinates ayahCoordinates = ayahCoordinatesProvider.getAyahCoordinates();
        if (ayahCoordinates == null) return;

        Map<AyahHighlight, java.util.List<AyahBounds>> highlightCoordinates = highlightCoordinatesProvider.getHighlightCoordinates();
        if (highlightCoordinates == null) return;

        SortedMap<HighlightType, Set<AyahHighlight>> currentHighlights = currentHighlightsProvider.getCurrentHighlights();
        if (currentHighlights == null) return;

        alreadyHighlighted.clear();

        for (Map.Entry<HighlightType, Set<AyahHighlight>> entry : currentHighlights.entrySet()) {
            HighlightType highlightType = entry.getKey();
            Set<AyahHighlight> highlights = entry.getValue();

            if (!isHighlightTypeInFilter(highlightType.mode)) {
                continue;
            }

            Paint paint = PaintCache.getPaintForHighlightType(image.getContext(), highlightType);

            for (AyahHighlight highlight : highlights) {
                if (alreadyHighlightedContains(highlightType.mode, highlight)) {
                    continue;
                }

                java.util.List<RectF> rangesToDraw = highlightType.isTransitionAnimated()
                        ? null
                        : ayahCoordinates.glyphCoordinates.getBounds(highlight.getKey(), true, highlightType.mode == Mode.BACKGROUND);

                if (rangesToDraw != null && !rangesToDraw.isEmpty()) {
                    for (RectF bounds : rangesToDraw) {
                        if (highlightType.mode == Mode.UNDERLINE) {
                            float underlineThickness = TypedValue.applyDimension(
                                    COMPLEX_UNIT_DIP, UNDERLINE_THICKNESS_DIPS, image.getResources().getDisplayMetrics());
                            bounds.top = bounds.bottom;
                            bounds.bottom += underlineThickness;
                        }

                        image.getImageMatrix().mapRect(scaledRect, bounds);
                        scaledRect.offset(image.getPaddingLeft(), image.getPaddingTop());

                        switch (highlightType.mode) {
                            case HIDE:
                                clipOutRect(canvas, scaledRect);
                                break;

                            case COLOR:
                                canvas.save();
                                canvas.clipRect(scaledRect);
                                PorterDuffColorFilter appliedFilter = image.getColorFilter();
                                image.setColorFilter(paint.getColorFilter());
                                superOnDraw.onDraw(canvas);
                                image.setColorFilter(appliedFilter);
                                canvas.restore();
                                clipOutRect(canvas, scaledRect);
                                break;

                            case HIGHLIGHT:
                            case BACKGROUND:
                            case UNDERLINE:
                                canvas.drawRect(scaledRect, paint);
                                break;
                        }
                    }

                    alreadyHighlighted.computeIfAbsent(highlightType.mode, k -> new java.util.HashSet<>()).add(highlight);
                }
            }
        }
    }

    private boolean isHighlightTypeInFilter(Mode mode) {
        for (Mode filterMode : highlightTypesFilter) {
            if (mode == filterMode) {
                return true;
            }
        }
        return false;
    }

    private boolean alreadyHighlightedContains(Mode mode, AyahHighlight highlight) {
        Set<AyahHighlight> alreadyHighlightedSet = alreadyHighlighted.getOrDefault(mode, java.util.Collections.emptySet());

        if (alreadyHighlightedSet.contains(highlight)) {
            return true;
        }

        if (highlight.isTransition()) {
            TransitionAyahHighlight transitionHighlight = (TransitionAyahHighlight) highlight;

            if (alreadyHighlightedSet.contains(transitionHighlight.getSource())
                    || alreadyHighlightedSet.contains(transitionHighlight.getDestination())) {
                return true;
            }
        }

        return false;
    }

    private void clipOutRect(Canvas canvas, RectF rect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            canvas.clipOutRect(rect);
        } else {
            canvas.clipRect(rect, Region.Op.DIFFERENCE);
        }
    }

    public interface AyahCoordinatesProvider {
        AyahCoordinates getAyahCoordinates();
    }

    public interface HighlightCoordinatesProvider {
        Map<AyahHighlight, java.util.List<AyahBounds>> getHighlightCoordinates();
    }

    public interface CurrentHighlightsProvider {
        SortedMap<HighlightType, Set<AyahHighlight>> getCurrentHighlights();
    }

    public interface OnDrawCallback {
        void onDraw(Canvas canvas);
    }

    private static class PaintCache {
        private static final java.util.Map<Pair<Mode, Integer>, Paint> cache = new java.util.HashMap<>();

        static Paint getPaintForHighlightType(Context context, HighlightType type) {
            return cache.computeIfAbsent(new Pair<>(type.mode, type.colorResId), key -> {
                Paint paint = new Paint();
                int color = ContextCompat.getColor(context, type.colorResId);

                switch (type.mode) {
                    case COLOR:
                        paint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP));
                        break;

                    default:
                        paint.setColor(color);
                        break;
                }

                return paint;
            });
        }
    }

    private static final float UNDERLINE_THICKNESS_DIPS = 3f;
}
