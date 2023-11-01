package com.quran.labs.androidquran.ui.preference;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import com.quran.labs.androidquran.R;
import com.quran.labs.androidquran.data.Constants;
import com.quran.labs.androidquran.util.QuranUtils;

public class SeekBarPreference extends Preference implements SeekBar.OnSeekBarChangeListener {

    private TextView valueText;
    protected TextView previewText;
    protected View previewBox;

    private final String suffix;
    private final int defaultValue;
    private final int maxValue;
    private int currentValue = 0;
    protected int value = 0;

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        suffix = attrs.getAttributeValue(ANDROID_NS, "text");
        defaultValue = attrs.getAttributeIntValue(
                ANDROID_NS,
                "defaultValue",
                Constants.DEFAULT_TEXT_SIZE
        );
        maxValue = attrs.getAttributeIntValue(ANDROID_NS, "max", 100);
        currentValue = 0;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        SeekBar seekBar = (SeekBar) holder.findViewById(R.id.seekbar);
        valueText = (TextView) holder.findViewById(R.id.value);
        previewText = (TextView) holder.findViewById(R.id.pref_preview);
        previewBox = holder.findViewById(R.id.preview_square);
        previewText.setVisibility(getPreviewVisibility());
        seekBar.setOnSeekBarChangeListener(this);
        value = shouldDisableView ? getPersistedInt(defaultValue) : 0;
        seekBar.setMax(maxValue);
        seekBar.setProgress(value);
    }

    @Override
    protected void onSetInitialValue(Object defaultValue) {
        super.onSetInitialValue(defaultValue);
        value = shouldPersist() ? getPersistedInt(this.defaultValue) : (defaultValue != null ? (int) defaultValue : 0);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        String t = QuranUtils.getLocalizedNumber(getContext(), progress);
        valueText.setText(suffix == null ? t : t + suffix);
        currentValue = progress;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (shouldPersist()) {
            persistInt(currentValue);
            callChangeListener(currentValue);
        }
    }

    protected int getPreviewVisibility() {
        return View.GONE;
    }

    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";
}
