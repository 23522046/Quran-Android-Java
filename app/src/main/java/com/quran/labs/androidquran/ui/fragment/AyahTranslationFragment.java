package com.quran.labs.androidquran.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import androidx.fragment.app.Fragment;

import com.quran.labs.androidquran.R;
import com.quran.labs.androidquran.common.LocalTranslation;
import com.quran.labs.androidquran.common.QuranAyahInfo;
import com.quran.labs.androidquran.presenter.translation.InlineTranslationPresenter;
import com.quran.labs.androidquran.ui.PagerActivity;
import com.quran.labs.androidquran.ui.helpers.SlidingPagerAdapter;
import com.quran.labs.androidquran.ui.util.TranslationsSpinnerAdapter;
import com.quran.labs.androidquran.util.QuranSettings;
import com.quran.data.core.QuranInfo;
import com.quran.data.model.VerseRange;
import com.quran.labs.androidquran.view.InlineTranslationView;
import com.quran.labs.androidquran.view.QuranSpinner;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import kotlin.math.abs;

public class AyahTranslationFragment extends AyahActionFragment implements InlineTranslationPresenter.TranslationScreen {
    private ProgressBar progressBar;
    private InlineTranslationView translationView;
    private View emptyState;
    private View translationControls;
    private QuranSpinner translator;

    private TranslationsSpinnerAdapter translationAdapter;

    @Inject
    QuranInfo quranInfo;

    @Inject
    QuranSettings quranSettings;

    @Inject
    InlineTranslationPresenter translationPresenter;

    public static class Provider implements AyahActionFragmentProvider {
        @Override
        public int getOrder() {
            return SlidingPagerAdapter.TRANSLATION_PAGE;
        }

        @Override
        public int getIconResId() {
            return com.quran.labs.androidquran.common.toolbar.R.drawable.ic_translation;
        }

        @Override
        public AyahActionFragment newAyahActionFragment() {
            return new AyahTranslationFragment();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof PagerActivity) {
            ((PagerActivity) context).getPagerActivityComponent().inject(this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.translation_panel, container, false);
        translator = view.findViewById(R.id.translator);
        translationView = view.findViewById(R.id.translation_view);
        progressBar = view.findViewById(R.id.progress);
        emptyState = view.findViewById(R.id.empty_state);
        translationControls = view.findViewById(R.id.controls);

        View next = translationControls.findViewById(R.id.next_ayah);
        next.setOnClickListener(onClickListener);
        View prev = translationControls.findViewById(R.id.previous_ayah);
        prev.setOnClickListener(onClickListener);
        Button getTranslations = view.findViewById(R.id.get_translations_button);
        getTranslations.setOnClickListener(onClickListener);

        return view;
    }

    @Override
    public void onResume() {
        // Currently needs to be before we call super.onResume
        translationPresenter.bind(this);
        super.onResume();
    }

    @Override
    public void onPause() {
        translationPresenter.unbind(this);
        super.onPause();
    }

    private final View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Activity activity = getActivity();
            if (activity instanceof PagerActivity) {
                switch (v.getId()) {
                    case R.id.get_translations_button:
                        ((PagerActivity) activity).startTranslationManager();
                        break;
                    case R.id.next_ayah:
                        readingEventPresenter.selectNextAyah();
                        break;
                    case R.id.previous_ayah:
                        readingEventPresenter.selectPreviousAyah();
                        break;
                }
            }
        }
    };

    @Override
    public void refreshView() {
        SuraAyah start = start;
        SuraAyah end = end;
        if (start == null || end == null) {
            return;
        }
        Activity activity = getActivity();
        if (activity instanceof PagerActivity) {
            List<LocalTranslation> translations = ((PagerActivity) activity).getTranslations();
            if (translations == null || translations.size() == 0) {
                progressBar.setVisibility(View.GONE);
                emptyState.setVisibility(View.VISIBLE);
                translationControls.setVisibility(View.GONE);
                return;
            }

            Set<String> activeTranslationsFilesNames = ((PagerActivity) activity).getActiveTranslationsFilesNames();
            if (activeTranslationsFilesNames == null) {
                activeTranslationsFilesNames = quranSettings.getActiveTranslations();
            }

            TranslationsSpinnerAdapter adapter = translationAdapter;
            if (adapter == null) {
                translationAdapter = new TranslationsSpinnerAdapter(
                        activity,
                        R.layout.translation_ab_spinner_item,
                        ((PagerActivity) activity).getTranslationNames(),
                        translations,
                        activeTranslationsFilesNames,
                        selectedItems -> {
                            quranSettings.setActiveTranslations(selectedItems);
                            refreshView();
                        }
                );
                translator.setAdapter(translationAdapter);
            } else {
                adapter.updateItems(
                        ((PagerActivity) activity).getTranslationNames(),
                        translations,
                        activeTranslationsFilesNames
                );
            }
            if (start.equals(end)) {
                translationControls.setVisibility(View.VISIBLE);
            } else {
                translationControls.setVisibility(View.GONE);
            }
            int verses = 1 + abs(quranInfo.getAyahId(start.getSura(), start.getAyah()) -
                    quranInfo.getAyahId(end.getSura(), end.getAyah()));
            VerseRange verseRange = new VerseRange(start.getSura(), start.getAyah(), end.getSura(), end.getAyah(), verses);
            translationPresenter.refresh(verseRange);
        }
    }

    @Override
    public void setVerses(LocalTranslation[] translations, List<QuranAyahInfo> verses) {
        progressBar.setVisibility(View.GONE);
        if (!verses.isEmpty()) {
            emptyState.setVisibility(View.GONE);
            translationView.setAyahs(translations, verses);
        } else {
            emptyState.setVisibility(View.VISIBLE);
        }
    }
}
