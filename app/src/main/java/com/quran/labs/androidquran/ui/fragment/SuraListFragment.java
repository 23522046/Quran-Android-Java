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
import com.quran.labs.androidquran.data.Constants.JUZ2_COUNT;
import com.quran.labs.androidquran.data.Constants.SURAS_COUNT;
import com.quran.labs.androidquran.data.QuranDisplayData;
import com.quran.labs.androidquran.ui.QuranActivity;
import com.quran.labs.androidquran.ui.helpers.QuranListAdapter;
import com.quran.labs.androidquran.ui.helpers.QuranRow;
import com.quran.labs.androidquran.util.QuranSettings;
import com.quran.labs.androidquran.util.QuranUtils;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.observers.DisposableSingleObserver;
import javax.inject.Inject;

public class SuraListFragment extends Fragment {

    @Inject
    QuranInfo quranInfo;

    @Inject
    QuranDisplayData quranDisplayData;

    @Inject
    QuranSettings quranSettings;

    private RecyclerView recyclerView;
    private int numberOfPages = 0;
    private boolean showSuraTranslatedName = false;
    private Disposable disposable = null;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        ((QuranApplication) context.getApplicationContext()).getApplicationComponent().inject(this);
        numberOfPages = quranInfo.getNumberOfPages();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.quran_list, container, false);
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(new QuranListAdapter(requireActivity(), recyclerView, getSuraList(), false));
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Activity activity = requireActivity();
        if (activity instanceof QuranActivity) {
            boolean newValueOfShowSuraTranslatedName = quranSettings.isShowSuraTranslatedName();
            if (showSuraTranslatedName != newValueOfShowSuraTranslatedName) {
                showHideSuraTranslatedName();
                showSuraTranslatedName = newValueOfShowSuraTranslatedName;
            }
            disposable = ((QuranActivity) requireActivity()).getLatestPageObservable()
                    .first(Constants.NO_PAGE)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(new DisposableSingleObserver<Integer>() {
                        @Override
                        public void onSuccess(Integer recentPage) {
                            if (recentPage != Constants.NO_PAGE) {
                                int sura = quranDisplayData.safelyGetSuraOnPage(recentPage);
                                int juz = quranInfo.getJuzFromPage(recentPage);
                                int position = sura + juz - 1;
                                recyclerView.scrollToPosition(position);
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                        }
                    });

            if (quranSettings.isArabicNames()) {
                updateScrollBarPositionHoneycomb();
            }
        }
    }

    @Override
    public void onPause() {
        if (disposable != null) {
            disposable.dispose();
        }
        super.onPause();
    }

    private void updateScrollBarPositionHoneycomb() {
        recyclerView.setVerticalScrollbarPosition(View.SCROLLBAR_POSITION_LEFT);
    }

    private QuranRow[] getSuraList() {
        int next;
        int pos = 0;
        int sura = 1;
        QuranRow[] elements = new QuranRow[SURAS_COUNT + JUZ2_COUNT];

        Activity activity = requireActivity();
        boolean wantPrefix = activity.getResources().getBoolean(R.bool.show_surat_prefix);
        boolean wantTranslation = quranSettings.isShowSuraTranslatedName();

        for (int juz = 1; juz <= JUZ2_COUNT; juz++) {
            String headerTitle = activity.getString(R.string.juz2_description, QuranUtils.getLocalizedNumber(activity, juz));
            QuranRow.Builder headerBuilder = new QuranRow.Builder()
                    .withType(QuranRow.HEADER)
                    .withText(headerTitle)
                    .withPage(quranInfo.getStartingPageForJuz(juz));
            elements[pos++] = headerBuilder.build();
            next = (juz == JUZ2_COUNT) ? numberOfPages + 1 : quranInfo.getStartingPageForJuz(juz + 1);

            while (sura <= SURAS_COUNT && quranInfo.getPageNumberForSura(sura) < next) {
                QuranRow.Builder builder = new QuranRow.Builder()
                        .withText(quranDisplayData.getSuraName(activity, sura, wantPrefix, wantTranslation))
                        .withMetadata(quranDisplayData.getSuraListMetaString(activity, sura))
                        .withSura(sura)
                        .withPage(quranInfo.getPageNumberForSura(sura));
                elements[pos++] = builder.build();
                sura++;
            }
        }

        QuranRow[] nonNullElements = new QuranRow[pos];
        System.arraycopy(elements, 0, nonNullElements, 0, pos);
        return nonNullElements;
    }

    private void showHideSuraTranslatedName() {
        QuranRow[] elements = getSuraList();
        ((QuranListAdapter) recyclerView.getAdapter()).setElements(elements);
        recyclerView.getAdapter().notifyDataSetChanged();
    }

    public static SuraListFragment newInstance() {
        return new SuraListFragment();
    }
}
