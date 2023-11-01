package com.quran.labs.androidquran.ui.fragment;

import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import com.quran.labs.androidquran.BuildConfig;
import com.quran.labs.androidquran.R;

public class AboutFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.about);

        String flavor = BuildConfig.FLAVOR + "Images";
        PreferenceCategory parent = findPreference("aboutDataSources");
        
        for (String key : imagePrefKeys) {
            if (!key.equals(flavor)) {
                Preference pref = findPreference(key);
                if (pref != null && parent != null) {
                    parent.removePreference(pref);
                }
            }
        }
    }

    private static final String[] imagePrefKeys = {"madaniImages", "naskhImages", "qaloonImages"};
}
