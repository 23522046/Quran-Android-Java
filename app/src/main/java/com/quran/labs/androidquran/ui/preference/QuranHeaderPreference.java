package com.quran.labs.androidquran.ui.preference;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.TextView;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import com.quran.labs.androidquran.R;

public class QuranHeaderPreference extends Preference {

    public QuranHeaderPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.about_header);
        setSelectable(false);
    }

    public QuranHeaderPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public QuranHeaderPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public QuranHeaderPreference(Context context) {
        this(context, null);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        if (isEnabled()) {
            TextView tv = holder.findViewById(R.id.title);
            if (tv != null) {
                tv.setTextColor(Color.WHITE);
            }
        }
    }
}
