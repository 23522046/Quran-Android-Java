package com.quran.labs.androidquran.ui.preference;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;

public class SeekBarBackgroundBrightnessPreference extends SeekBarPreference {

    public SeekBarBackgroundBrightnessPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected int getPreviewVisibility() {
        return View.GONE;
    }

    @Override
    protected void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        super.onProgressChanged(seekBar, progress, fromUser);
        getPreviewBox().setVisibility(View.VISIBLE);

        int boxColor = Color.argb(255, progress, progress, progress);
        getPreviewBox().setBackgroundColor(boxColor);
    }
}
