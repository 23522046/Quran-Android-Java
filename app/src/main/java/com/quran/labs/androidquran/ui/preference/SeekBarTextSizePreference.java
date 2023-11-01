package com.quran.labs.androidquran.ui.preference;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

public class SeekBarTextSizePreference extends SeekBarPreference {

    public SeekBarTextSizePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected int getPreviewVisibility() {
        return View.VISIBLE;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        super.onProgressChanged(seekBar, progress, fromUser);
        previewText.setTextSize(progress);
    }
}
