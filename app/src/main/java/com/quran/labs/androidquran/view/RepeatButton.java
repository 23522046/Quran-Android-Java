package com.quran.labs.androidquran.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

import com.quran.labs.androidquran.R;

public class RepeatButton extends AppCompatImageView {

    private String text = "";
    private TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private boolean canDraw = false;
    private int viewWidth = 0;
    private int viewHeight = 0;
    private int textXPosition = 0;
    private int textYPosition = 0;
    private int textYPadding;

    public RepeatButton(Context context) {
        super(context);
        init(context, null, 0);
    }

    public RepeatButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public RepeatButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        paint.setColor(Color.WHITE);
        textYPadding = getResources().getDimensionPixelSize(R.dimen.repeat_text_y_padding);
        paint.setTextSize(getResources().getDimensionPixelSize(R.dimen.repeat_superscript_text_size));
    }

    public void setText(String text) {
        this.text = text;
        updateCoordinates();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        viewWidth = getMeasuredWidth();
        viewHeight = getMeasuredHeight();
        updateCoordinates();
    }

    private void updateCoordinates() {
        canDraw = false;
        if (getDrawable() != null) {
            int boundsWidth = getDrawable().getBounds().width();
            if (boundsWidth > 0) {
                textXPosition = viewWidth - (viewWidth - boundsWidth) / 2;
                textYPosition = textYPadding + (viewHeight - getDrawable().getBounds().height()) / 2;
                canDraw = true;
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int length = text.length();
        if (canDraw && length > 0) {
            canvas.drawText(text, 0, length, textXPosition, textYPosition, paint);
        }
    }
}
