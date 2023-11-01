package com.quran.labs.androidquran.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.quran.data.core.QuranInfo;
import com.quran.labs.androidquran.QuranApplication;
import com.quran.labs.androidquran.R;
import com.quran.labs.androidquran.data.Constants;
import com.quran.labs.androidquran.data.QuranDisplayData;
import com.quran.labs.androidquran.data.QuranFileConstants;
import com.quran.labs.androidquran.presenter.data.JuzListPresenter;
import com.quran.labs.androidquran.ui.QuranActivity;
import com.quran.labs.androidquran.ui.helpers.QuranListAdapter;
import com.quran.labs.androidquran.ui.helpers.QuranRow;
import com.quran.labs.androidquran.ui.helpers.QuranRow.Builder;
import com.quran.labs.androidquran.util.QuranSettings;
import com.quran.labs.androidquran.util.QuranUtils;
import com.quran.labs.androidquran.view.JuzView;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.observers.DisposableSingleObserver;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.MainScope;
import kotlinx.coroutines.launch;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Fragment that displays a list of all Juz (using [QuranListAdapter], each divided into
 * 8 parts (with headings for each Juz).
 * When a Juz part is selected (or a Juz heading), [QuranActivity.jumpTo] is called to
 * jump to that page.
 */
public class JuzListFragment extends Fragment {
    private RecyclerView recyclerView;
    private Disposable disposable;
    private QuranListAdapter adapter;
    private final CoroutineScope mainScope = new MainScope();

    @Inject
    QuranInfo quranInfo;

    @Inject
    QuranDisplayData quranDisplayData;

    @Inject
    JuzListPresenter juzListPresenter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.quran_list, container, false);

        Context context = requireContext();
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        adapter = new QuranListAdapter(context, recyclerView, new QuranRow[0], false);
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onDestroyView() {
        adapter = null;
        recyclerView = null;
        super.onDestroyView();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        ((QuranApplication) context.getApplicationContext()).getApplicationComponent().inject(this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mainScope.launch(() -> fetchJuz2List());
    }

    @Override
    public void onPause() {
        disposable.dispose();
        super.onPause();
    }

    @Override
    public void onResume() {
        Activity activity = requireActivity();
        if (activity instanceof QuranActivity) {
            disposable = ((QuranActivity) activity).latestPageObservable
                    .first(Constants.NO_PAGE)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(new DisposableSingleObserver<Integer>() {
                        @Override
                        public void onSuccess(Integer recentPage) {
                            if (recentPage != Constants.NO_PAGE) {
                                int juz = quranInfo.getJuzFromPage(recentPage);
                                int position = (juz - 1) * 9;
                                recyclerView.scrollToPosition(position);
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                        }
                    });
        }

        QuranSettings settings = QuranSettings.getInstance(activity);
        if (settings.isArabicNames()) {
            updateScrollBarPositionHoneycomb();
        }
        super.onResume();
    }

    private void updateScrollBarPositionHoneycomb() {
        recyclerView.setVerticalScrollbarPosition(View.SCROLLBAR_POSITION_LEFT);
    }

    private void fetchJuz2List() {
        String[] quarters;
        if (QuranFileConstants.FETCH_QUARTER_NAMES_FROM_DATABASE) {
            quarters = juzListPresenter.quarters().toArray(new String[0]);
        } else {
            Context context = getContext();
            if (context != null) {
                quarters = context.getResources().getStringArray(R.array.quarter_prefix_array);
            } else {
                quarters = new String[0];
            }
        }

        if (isAdded() && quarters.length > 0) {
            updateJuz2List(quarters);
        }
    }

    private void updateJuz2List(String[] quarters) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        List<QuranRow> elements = new ArrayList<>();
        for (int i = 0; i < 8 * Constants.JUZ2_COUNT; i++) {
            QuranInfo.SuraAyah pos = quranInfo.getQuarterByIndex(i);
            int page = quranInfo.getPageFromSuraAyah(pos.sura, pos.ayah);

            if (i % 8 == 0) {
                int juz = 1 + i / 8;
                String juzTitle = getString(
                        R.string.juz2_description,
                        QuranUtils.getLocalizedNumber(activity, juz)
                );
                QuranRow header = new Builder()
                        .withType(QuranRow.HEADER)
                        .withText(juzTitle)
                        .withPage(quranInfo.getStartingPageForJuz(juz))
                        .build();
                elements.add(header);
            }

            String metadata = getString(
                    R.string.sura_ayah_notification_str,
                    quranDisplayData.getSuraName(activity, pos.sura, false), pos.ayah
            );
            String juzTextWithEllipsis = quarters[i] + "...";
            QuranRow row = new Builder()
                    .withText(juzTextWithEllipsis)
                    .withMetadata(metadata)
                    .withPage(page)
                    .withJuzType(ENTRY_TYPES[i % 4])
                    .build();
            if (i % 4 == 0) {
                String overlayText = QuranUtils.getLocalizedNumber(activity, 1 + i / 4);
                row.setJuzOverlayText(overlayText);
            }
            elements.add(row);
        }

        adapter.setElements(elements.toArray(new QuranRow[0]));
    }

    private static final int[] ENTRY_TYPES = {
            JuzView.TYPE_JUZ, JuzView.TYPE_QUARTER,
            JuzView.TYPE_HALF, JuzView.TYPE_THREE_QUARTERS
    };

    public static JuzListFragment newInstance() {
        return new JuzListFragment();
    }
}
