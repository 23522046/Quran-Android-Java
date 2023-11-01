package com.quran.labs.androidquran.ui.preference;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;

public class SeekBarTextBrightnessPreference extends SeekBarPreference {

    public SeekBarTextBrightnessPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected int getPreviewVisibility() {
        return View.VISIBLE;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        super.onProgressChanged(seekBar, progress, fromUser);
        int lineColor = Color.argb(progress, 255, 255, 255);
        previewText.setTextColor(lineColor);
    }
}
