package com.quran.labs.androidquran.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;

import androidx.core.content.ContextCompat;

import com.quran.labs.androidquran.R;

public class JuzView extends Drawable {

    private int radius = 0;
    private int circleY = 0;
    private final int percentage;
    private float textOffset = 0f;

    private RectF circleRect;
    private final Paint circlePaint = new Paint();
    private TextPaint overlayTextPaint;
    private final Paint circleBackgroundPaint = new Paint();
    private final String overlayText;

    public static final int TYPE_JUZ = 1;
    public static final int TYPE_QUARTER = 2;
    public static final int TYPE_HALF = 3;
    public static final int TYPE_THREE_QUARTERS = 4;

    public JuzView(Context context, int type, String overlayText) {
        this.overlayText = overlayText;

        int circleColor = ContextCompat.getColor(context, R.color.accent_color);
        int circleBackground = ContextCompat.getColor(context, R.color.accent_color_dark);

        circlePaint.setStyle(Paint.Style.FILL);
        circlePaint.setColor(circleColor);
        circlePaint.setAntiAlias(true);

        circleBackgroundPaint.setStyle(Paint.Style.FILL);
        circleBackgroundPaint.setColor(circleBackground);
        circleBackgroundPaint.setAntiAlias(true);

        if (overlayText != null && !overlayText.isEmpty()) {
            int textPaintColor = ContextCompat.getColor(context, R.color.header_background);
            int textPaintSize = context.getResources().getDimensionPixelSize(R.dimen.juz_overlay_text_size);
            overlayTextPaint = new TextPaint();
            overlayTextPaint.setAntiAlias(true);
            overlayTextPaint.setColor(textPaintColor);
            overlayTextPaint.setTextSize(textPaintSize);
            overlayTextPaint.setTextAlign(Paint.Align.CENTER);

            if (overlayTextPaint != null) {
                float textHeight = overlayTextPaint.descent() - overlayTextPaint.ascent();
                textOffset = textHeight / 2 - overlayTextPaint.descent();
            }
        }

        this.percentage = switch (type) {
            case TYPE_JUZ -> 100;
            case TYPE_THREE_QUARTERS -> 75;
            case TYPE_HALF -> 50;
            case TYPE_QUARTER -> 25;
            default -> 0;
        };
    }

    @Override
    public void setBounds(int left, int top, int right, int bottom) {
        super.setBounds(left, top, right, bottom);
        radius = (right - left) / 2;
        int yOffset = (bottom - top - 2 * radius) / 2;
        circleY = radius + yOffset;
        circleRect = new RectF(left, top + yOffset, right, top + yOffset + 2 * radius);
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawCircle(radius, circleY, radius, circleBackgroundPaint);
        canvas.drawArc(
                circleRect, -90f,
                (float) (3.6 * percentage), true, circlePaint
        );
        if (overlayTextPaint != null && overlayText != null) {
            canvas.drawText(
                    overlayText, circleRect.centerX(),
                    circleRect.centerY() + textOffset, overlayTextPaint
            );
        }
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAlpha(int alpha) {
        // No-op for now
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        // No-op for now
    }
}
