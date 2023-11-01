package com.quran.labs.androidquran.ui.util;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.SparseArray;
import android.widget.ImageView;
import com.quran.data.model.SuraAyah;
import com.quran.data.model.selection.SelectionIndicator;
import com.quran.data.model.selection.SelectionRectangle;
import com.quran.page.common.data.AyahBounds;
import timber.log.Timber;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ImageAyahUtils {

    private static SuraAyah getAyahFromKey(String key) {
        String[] parts = key.split(":");
        if (parts.length != 2) {
            return null;
        }

        int sura = Integer.parseInt(parts[0]);
        int ayah = Integer.parseInt(parts[1]);
        return new SuraAyah(sura, ayah);
    }

    public static SuraAyah getAyahFromCoordinates(
            Map<String, List<AyahBounds>> coords,
            HighlightingImageView imageView,
            float xc,
            float yc) {
        Pair<SuraAyah, AyahBounds> result = getAyahBoundsFromCoordinates(coords, imageView, xc, yc);
        return result != null ? result.first : null;
    }

    private static Pair<SuraAyah, AyahBounds> getAyahBoundsFromCoordinates(
            Map<String, List<AyahBounds>> coords,
            HighlightingImageView imageView,
            float xc,
            float yc) {
        if (coords == null || imageView == null) {
            return null;
        }

        float[] pageXY = getPageXY(xc, yc, imageView);
        if (pageXY == null) {
            return null;
        }

        float x = pageXY[0];
        float y = pageXY[1];
        int closestLine = -1;
        int closestDelta = -1;

        SparseArray<List<String>> lineAyahs = new SparseArray<>();
        for (String key : coords.keySet()) {
            List<AyahBounds> boundsList = coords.get(key);
            if (boundsList == null) {
                continue;
            }

            for (AyahBounds b : boundsList) {
                int line = b.getLine();
                List<String> items = lineAyahs.get(line);
                if (items == null) {
                    items = new ArrayList<>();
                }
                items.add(key);
                lineAyahs.put(line, items);

                RectF boundsRect = b.getBounds();
                if (boundsRect.contains(x, y)) {
                    return new Pair<>(getAyahFromKey(key), b);
                }

                int delta = Math.min(
                        Math.abs((int) (boundsRect.bottom - y)),
                        Math.abs((int) (boundsRect.top - y))
                );

                if (closestDelta == -1 || delta < closestDelta) {
                    closestLine = b.getLine();
                    closestDelta = delta;
                }
            }
        }

        if (closestLine > -1) {
            int leastDeltaX = -1;
            String closestAyah = null;
            AyahBounds closestAyahBounds = null;
            List<String> ayat = lineAyahs.get(closestLine);

            if (ayat != null) {
                Timber.d("no exact match, %d candidates.", ayat.size());
                for (String ayah : ayat) {
                    List<AyahBounds> boundsList = coords.get(ayah);
                    if (boundsList == null) {
                        continue;
                    }

                    for (AyahBounds b : boundsList) {
                        if (b.getLine() > closestLine) {
                            break;
                        }

                        RectF boundsRect = b.getBounds();
                        if (b.getLine() == closestLine) {
                            if (boundsRect.right >= x && boundsRect.left <= x) {
                                return new Pair<>(getAyahFromKey(ayah), b);
                            }

                            int delta = Math.min(
                                    Math.abs((int) (boundsRect.right - x)),
                                    Math.abs((int) (boundsRect.left - x))
                            );

                            if (leastDeltaX == -1 || delta < leastDeltaX) {
                                closestAyah = ayah;
                                closestAyahBounds = b;
                                leastDeltaX = delta;
                            }
                        }
                    }
                }
            }

            if (closestAyah != null && closestAyahBounds != null) {
                Timber.d("fell back to closest ayah of %s", closestAyah);
                return new Pair<>(getAyahFromKey(closestAyah), closestAyahBounds);
            }
        }

        return null;
    }

    public static SelectionIndicator getToolBarPosition(
            List<AyahBounds> bounds,
            Matrix matrix,
            int xPadding,
            int yPadding) {
        if (bounds.isEmpty()) {
            return SelectionIndicator.None;
        }

        AyahBounds first = bounds.get(0);
        AyahBounds last = bounds.get(bounds.size() - 1);

        RectF mappedRect = new RectF();
        matrix.mapRect(mappedRect, first.getBounds());
        SelectionRectangle top = new SelectionRectangle(
                mappedRect.left + xPadding,
                mappedRect.top + yPadding,
                mappedRect.right + xPadding,
                mappedRect.bottom + yPadding
        );

        if (first == last) {
            return new SelectionIndicator.SelectedItemPosition(top, top);
        } else {
            matrix.mapRect(mappedRect, last.getBounds());
            SelectionRectangle bottom = new SelectionRectangle(
                    mappedRect.left + xPadding,
                    mappedRect.top + yPadding,
                    mappedRect.right + xPadding,
                    mappedRect.bottom + yPadding
            );
            return new SelectionIndicator.SelectedItemPosition(top, bottom);
        }
    }

    public static float[] getPageXY(float screenX, float screenY, ImageView imageView) {
        if (imageView.getDrawable() == null) {
            return null;
        }

        float[] results = new float[2];
        Matrix inverse = new Matrix();
        if (imageView.getImageMatrix().invert(inverse)) {
            float[] screenPoints = {screenX, screenY};
            inverse.mapPoints(results, screenPoints);
            results[1] = results[1] - imageView.getPaddingTop();
        }
        return results;
    }

    public static RectF getYBoundsForHighlight(
            Map<String, List<AyahBounds>> coordinateData, int sura, int ayah) {
        List<AyahBounds> ayahBounds = coordinateData.get(sura + ":" + ayah);
        if (ayahBounds == null) {
            return null;
        }

        RectF ayahBoundsRect = null;
        for (AyahBounds bounds : ayahBounds) {
            if (ayahBoundsRect == null) {
                ayahBoundsRect = bounds.getBounds();
            } else {
                ayahBoundsRect.union(bounds.getBounds());
            }
        }
        return ayahBoundsRect;
    }
}
