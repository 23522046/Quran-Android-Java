package com.quran.labs.androidquran.ui.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;

import com.quran.data.source.PageProvider;
import com.quran.labs.androidquran.QuranAdvancedPreferenceActivity;
import com.quran.labs.androidquran.QuranApplication;
import com.quran.labs.androidquran.QuranPreferenceActivity;
import com.quran.labs.androidquran.R;
import com.quran.labs.androidquran.data.Constants;
import com.quran.labs.androidquran.pageselect.PageSelectActivity;
import com.quran.labs.androidquran.ui.TranslationManagerActivity;
import com.quran.mobile.di.ExtraPreferencesProvider;
import com.quran.mobile.feature.downloadmanager.AudioManagerActivity;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

public class QuranSettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Inject
    Map<String, PageProvider> pageTypes;

    @Inject
    Set<ExtraPreferencesProvider> extraPreferences;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.quran_preferences);

        final android.content.Context appContext = requireContext().getApplicationContext();

        // Field injection
        ((QuranApplication) appContext).getApplicationComponent().inject(this);

        // Handle translation manager click
        Preference translationPref = findPreference(Constants.PREF_TRANSLATION_MANAGER);
        if (translationPref != null) {
            translationPref.setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(requireActivity(), TranslationManagerActivity.class));
                return true;
            });
        }

        // Handle audio manager click
        Preference audioManagerPref = findPreference(Constants.PREF_AUDIO_MANAGER);
        if (audioManagerPref != null) {
            audioManagerPref.setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(requireActivity(), AudioManagerActivity.class));
                return true;
            });
        }

        Preference pageChangePref = findPreference(Constants.PREF_PAGE_TYPE);
        if (pageTypes.size() < 2 && pageChangePref != null) {
            PreferenceGroup readingPrefs = (PreferenceGroup) findPreference(Constants.PREF_READING_CATEGORY);
            if (readingPrefs != null) {
                readingPrefs.removePreference(pageChangePref);
            }
        }

        // Add additional injected preferences (if any)
        extraPreferences.stream()
                .sorted((a, b) -> Integer.compare(a.getOrder(), b.getOrder()))
                .forEach(extraPref -> extraPref.addPreferences(getPreferenceScreen()));
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key != null && key.equals(Constants.PREF_USE_ARABIC_NAMES)) {
            android.app.Activity context = requireActivity();
            if (context instanceof QuranPreferenceActivity) {
                ((QuranPreferenceActivity) context).restartActivity();
            }
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        String key = preference.getKey();
        if ("key_prefs_advanced".equals(key)) {
            startActivity(new Intent(requireActivity(), QuranAdvancedPreferenceActivity.class));
            return true;
        } else if (Constants.PREF_PAGE_TYPE.equals(key)) {
            startActivity(new Intent(requireActivity(), PageSelectActivity.class));
            return true;
        }

        for (ExtraPreferencesProvider extraPref : extraPreferences) {
            if (extraPref.onPreferenceClick(preference)) {
                return true;
            }
        }

        return super.onPreferenceTreeClick(preference);
    }
}
