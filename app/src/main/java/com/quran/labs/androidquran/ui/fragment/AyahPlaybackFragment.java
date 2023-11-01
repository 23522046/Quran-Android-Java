package com.quran.labs.androidquran.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import com.quran.data.core.QuranInfo;
import com.quran.data.model.SuraAyah;
import com.quran.labs.androidquran.R;
import com.quran.labs.androidquran.dao.audio.AudioRequest;
import com.quran.labs.androidquran.ui.PagerActivity;
import com.quran.labs.androidquran.ui.helpers.SlidingPagerAdapter;
import com.quran.labs.androidquran.ui.util.TypefaceManager;
import com.quran.labs.androidquran.util.QuranSettings;
import com.quran.labs.androidquran.util.QuranUtils;
import com.quran.labs.androidquran.view.QuranSpinner;
import com.shawnlin.numberpicker.NumberPicker;
import java.text.NumberFormat;
import java.util.Locale;
import javax.inject.Inject;

public class AyahPlaybackFragment extends AyahActionFragment {

    private static final int MAX_REPEATS = 25;
    private static final int ITEM_LAYOUT = R.layout.sherlock_spinner_item;
    private static final int ITEM_DROPDOWN_LAYOUT = R.layout.sherlock_spinner_dropdown_item;
    private static final int DEFAULT_VERSE_REPEAT = 1;
    private static final int DEFAULT_RANGE_REPEAT = 1;

    private SuraAyah decidedStart = null;
    private SuraAyah decidedEnd = null;
    private boolean shouldEnforce = false;
    private int rangeRepeatCount = 0;
    private int verseRepeatCount = 0;

    private Button applyButton;
    private QuranSpinner startSuraSpinner;
    private QuranSpinner startAyahSpinner;
    private QuranSpinner endingSuraSpinner;
    private QuranSpinner endingAyahSpinner;
    private NumberPicker repeatVersePicker;
    private NumberPicker repeatRangePicker;
    private CheckBox restrictToRange;

    private ArrayAdapter<CharSequence> startAyahAdapter;
    private ArrayAdapter<CharSequence> endingAyahAdapter;

    private AudioRequest lastSeenAudioRequest = null;
    private boolean isOpen = false;

    @Inject
    QuranInfo quranInfo;

    public static class Provider implements AyahActionFragmentProvider {
        @Override
        public int getOrder() {
            return SlidingPagerAdapter.AUDIO_PAGE;
        }

        @Override
        public int getIconResId() {
            return com.quran.labs.androidquran.common.toolbar.R.drawable.ic_play;
        }

        @Override
        public AyahActionFragment newAyahActionFragment() {
            return new AyahPlaybackFragment();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.audio_panel, container, false);
        view.setOnClickListener(onClickListener);

        startSuraSpinner = view.findViewById(R.id.start_sura_spinner);
        startAyahSpinner = view.findViewById(R.id.start_ayah_spinner);
        endingSuraSpinner = view.findViewById(R.id.end_sura_spinner);
        endingAyahSpinner = view.findViewById(R.id.end_ayah_spinner);
        restrictToRange = view.findViewById(R.id.restrict_to_range);
        applyButton = view.findViewById(R.id.apply);
        applyButton.setOnClickListener(onClickListener);
        repeatVersePicker = view.findViewById(R.id.repeat_verse_picker);
        repeatRangePicker = view.findViewById(R.id.repeat_range_picker);

        Context context = requireContext();
        boolean isArabicNames = QuranSettings.getInstance(context).isArabicNames();
        Locale locale = isArabicNames ? new Locale("ar") : Locale.getDefault();
        NumberFormat numberFormat = NumberFormat.getNumberInstance(locale);
        String[] values = new String[MAX_REPEATS + 1];
        for (int i = 1; i <= MAX_REPEATS; i++) {
            values[i - 1] = numberFormat.format(i);
        }
        values[MAX_REPEATS] = getString(R.string.infinity);
        
        if (isArabicNames) {
            repeatVersePicker.setFormatter(value -> arFormat(value));
            repeatRangePicker.setFormatter(value -> arFormat(value));
            Typeface typeface = TypefaceManager.getHeaderFooterTypeface(context);
            repeatVersePicker.setTypeface(typeface);
            repeatVersePicker.setSelectedTypeface(typeface);
            repeatRangePicker.setTypeface(typeface);
            repeatRangePicker.setSelectedTypeface(typeface);
            repeatVersePicker.setSelectedTextSize(R.dimen.arabic_number_picker_selected_text_size);
            repeatRangePicker.setSelectedTextSize(R.dimen.arabic_number_picker_selected_text_size);
            repeatVersePicker.setTextSize(R.dimen.arabic_number_picker_text_size);
            repeatRangePicker.setTextSize(R.dimen.arabic_number_picker_text_size);
        }

        repeatVersePicker.setMinValue(1);
        repeatVersePicker.setMaxValue(MAX_REPEATS + 1);
        repeatRangePicker.setMinValue(1);
        repeatRangePicker.setMaxValue(MAX_REPEATS + 1);
        repeatVersePicker.setDisplayedValues(values);
        repeatRangePicker.setDisplayedValues(values);
        repeatRangePicker.setValue(defaultRangeRepeat);
        repeatVersePicker.setValue(defaultVerseRepeat);
        repeatRangePicker.setOnValueChangedListener((numberPicker, oldValue, newValue) -> {
            if (newValue > 1) {
                restrictToRange.setChecked(true);
            }
        });

        startAyahAdapter = initializeAyahSpinner(context, startAyahSpinner);
        endingAyahAdapter = initializeAyahSpinner(context, endingAyahSpinner);
        initializeSuraSpinner(context, startSuraSpinner, startAyahAdapter);
        initializeSuraSpinner(context, endingSuraSpinner, endingAyahAdapter);

        String[] repeatOptions = context.getResources().getStringArray(R.array.repeatValues);
        ArrayAdapter<CharSequence> rangeAdapter = new ArrayAdapter<>(context, ITEM_LAYOUT, repeatOptions);
        rangeAdapter.setDropDownViewResource(ITEM_DROPDOWN_LAYOUT);
        ArrayAdapter<CharSequence> verseAdapter = new ArrayAdapter<>(context, ITEM_LAYOUT, repeatOptions);
        verseAdapter.setDropDownViewResource(ITEM_DROPDOWN_LAYOUT);

        return view;
    }

    private String arFormat(int value) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(new Locale("ar"));
        return numberFormat.format(value);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof PagerActivity) {
            ((PagerActivity) context).getPagerActivityComponent().inject(this);
        }
    }

    private final View.OnClickListener onClickListener = v -> {
        if (v.getId() == R.id.apply) {
            apply();
        }
    };

    private void apply() {
        Context context = getActivity();
        if (context instanceof PagerActivity) {
            SuraAyah start = new SuraAyah(
                startSuraSpinner.getSelectedItemPosition() + 1,
                startAyahSpinner.getSelectedItemPosition() + 1
            );
            SuraAyah ending = new SuraAyah(
                endingSuraSpinner.getSelectedItemPosition() + 1,
                endingAyahSpinner.getSelectedItemPosition() + 1
            );
            // Force the correct order
            SuraAyah currentStart;
            SuraAyah currentEnding;
            if (ending.after(start)) {
                currentStart = start;
                currentEnding = ending;
            } else {
                currentStart = ending;
                currentEnding = start;
            }

            int page = quranInfo.getPageFromSuraAyah(currentStart.getSura(), currentStart.getAyah());
            int repeatVerse = repeatVersePicker.getValue() - 1;
            int repeatRange = repeatRangePicker.getValue() - 1;
            if (repeatVerse == MAX_REPEATS) {
                repeatVerse = -1;
            }
            if (repeatRange == MAX_REPEATS) {
                repeatRange = -1;
            }
            // Overwrite if infinite checkbox is checked
            int verseRepeat = repeatVerse;
            int rangeRepeat = repeatRange;
            boolean enforceRange = restrictToRange.isChecked();
            boolean updatedRange = false;

            if (!currentStart.equals(decidedStart) || !currentEnding.equals(decidedEnd)) {
                // Different range or not playing, so make a new request
                updatedRange = true;
                ((PagerActivity) context).playFromAyah(currentStart, currentEnding, page, verseRepeat, rangeRepeat, enforceRange);
            } else if (shouldEnforce != enforceRange || rangeRepeatCount != rangeRepeat || verseRepeatCount != verseRepeat) {
                // Can just update repeat settings
                if (!((PagerActivity) context).updatePlayOptions(rangeRepeat, verseRepeat, enforceRange)) {
                    // Audio stopped in the process, let's start it
                    ((PagerActivity) context).playFromAyah(currentStart, currentEnding, page, verseRepeat, rangeRepeat, enforceRange);
                }
            }

            ((PagerActivity) context).endAyahMode();
            if (updatedRange) {
                ((PagerActivity) context).toggleActionBarVisibility(true);
            }
        }
    }

    private void initializeSuraSpinner(Context context, QuranSpinner spinner, final ArrayAdapter<CharSequence> ayahAdapter) {
        String[] suras = context.getResources().getStringArray(R.array.sura_names);
        for (int i = 0; i < suras.length; i++) {
            suras[i] = QuranUtils.getLocalizedNumber(context, i + 1) + ". " + suras[i];
        }
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(context, ITEM_LAYOUT, suras);
        adapter.setDropDownViewResource(ITEM_DROPDOWN_LAYOUT);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long rowId) {
                int sura = position + 1;
                int ayahCount = quranInfo.getNumberOfAyahs(sura);
                CharSequence[] ayahs = new CharSequence[ayahCount];
                for (int i = 0; i < ayahCount; i++) {
                    ayahs[i] = QuranUtils.getLocalizedNumber(context, i + 1);
                }
                ayahAdapter.clear();
                for (int i = 0; i < ayahCount; i++) {
                    ayahAdapter.add(ayahs[i]);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
    }

    private ArrayAdapter<CharSequence> initializeAyahSpinner(Context context, QuranSpinner spinner) {
        ArrayAdapter<CharSequence> ayahAdapter = new ArrayAdapter<>(context, ITEM_LAYOUT);
        ayahAdapter.setDropDownViewResource(ITEM_DROPDOWN_LAYOUT);
        ayahAdapter.add(QuranUtils.getLocalizedNumber(context, 100)); // Initialize with a single item to measure
        spinner.setAdapter(ayahAdapter);
        return ayahAdapter;
    }

    private void updateAyahSpinner(QuranSpinner spinner, ArrayAdapter<CharSequence> adapter, int maxAyah, int currentAyah) {
        Context context = getActivity();
        if (context != null) {
            CharSequence[] ayahs = new CharSequence[maxAyah];
            for (int i = 0; i < maxAyah; i++) {
                ayahs[i] = QuranUtils.getLocalizedNumber(context, i + 1);
            }
            adapter.clear();
            for (int i = 0; i < maxAyah; i++) {
                adapter.add(ayahs[i]);
            }
            spinner.setSelection(currentAyah - 1);
        }
    }

    @Override
    public void onToggleDetailsPanel(boolean isVisible) {
        isOpen = isVisible;
        if (!isOpen) {
            refreshView();
        }
    }

    @Override
    public void refreshView() {
        Context context = getActivity();
        SuraAyah selectionEnd = end;
        SuraAyah selectionStart = start;
        boolean shouldReset = true;

        if (context instanceof PagerActivity && selectionStart != null && selectionEnd != null && !isOpen) {
            AudioRequest lastRequest = ((PagerActivity) context).getLastAudioRequest();
            SuraAyah start;
            SuraAyah ending;

            if (lastRequest != null) {
                start = lastRequest.getStart();
                ending = lastRequest.getEnd();

                if (!lastRequest.equals(lastSeenAudioRequest)) {
                    verseRepeatCount = lastRequest.getRepeatInfo();
                    rangeRepeatCount = lastRequest.getRangeRepeatInfo();
                    shouldEnforce = lastRequest.isEnforceBounds();
                } else {
                    shouldReset = false;
                }
                decidedStart = start;
                decidedEnd = ending;
                applyButton.setText(R.string.play_apply);
            } else {
                start = selectionStart;

                if (selectionStart.equals(selectionEnd)) {
                    int startPage = quranInfo.getPageFromSuraAyah(start.getSura(), start.getAyah());
                    int[] pageBounds = quranInfo.getPageBounds(startPage);
                    ending = new SuraAyah(pageBounds[2], pageBounds[3]);
                    shouldEnforce = false;
                } else {
                    ending = selectionEnd;
                    shouldEnforce = true;
                }

                rangeRepeatCount = 0;
                verseRepeatCount = 0;
                decidedStart = null;
                decidedEnd = null;
                applyButton.setText(R.string.play_apply_and_play);
            }

            lastSeenAudioRequest = lastRequest;

            int maxAyat = quranInfo.getNumberOfAyahs(start.getSura());
            if (maxAyat == -1) {
                return;
            }
            updateAyahSpinner(startAyahSpinner, startAyahAdapter, maxAyat, start.getAyah());
            int endAyat = (ending.getSura() == start.getSura()) ? maxAyat : quranInfo.getNumberOfAyahs(ending.getSura());
            updateAyahSpinner(endingAyahSpinner, endingAyahAdapter, endAyat, ending.getAyah());
            startSuraSpinner.setSelection(start.getSura() - 1);
            endingSuraSpinner.setSelection(ending.getSura() - 1);

            if (shouldReset) {
                restrictToRange.setChecked(shouldEnforce);
                repeatRangePicker.setValue(rangeRepeatCount + 1);
                repeatVersePicker.setValue(verseRepeatCount + 1);
            }
        }
    }
}
