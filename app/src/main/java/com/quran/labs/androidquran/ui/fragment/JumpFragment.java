package com.quran.labs.androidquran.ui.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.fragment.app.DialogFragment;

import com.quran.common.search.SearchTextUtil;
import com.quran.data.core.QuranInfo;
import com.quran.labs.androidquran.QuranApplication;
import com.quran.labs.androidquran.R;
import com.quran.labs.androidquran.ui.helpers.JumpDestination;
import com.quran.labs.androidquran.util.QuranUtils;
import com.quran.labs.androidquran.view.ForceCompleteTextView;

import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

/**
 * {@link DialogFragment} of a dialog for quickly selecting and jumping to a particular location in the Quran.
 * A location can be selected by page number or Surah/Ayah.
 */
public class JumpFragment extends DialogFragment {
    @Inject
    QuranInfo quranInfo;
    private boolean suppressJump = false;

    private ForceCompleteTextView suraInput;
    private EditText ayahInput;
    private EditText pageInput;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = requireActivity();
        LayoutInflater inflater = activity.getLayoutInflater();

        @SuppressLint("InflateParams")
        View layout = inflater.inflate(R.layout.jump_dialog, null);

        Builder builder = new Builder(activity);
        builder.setTitle(activity.getString(R.string.menu_jump));

        // Sura chooser
        suraInput = layout.findViewById(R.id.sura_spinner);
        String[] suras = activity.getResources().getStringArray(R.array.sura_names);
        suras = Stream.of(suras)
                .mapIndexed((index, sura) ->
                        QuranUtils.getLocalizedNumber(activity, index + 1) + ". " + sura)
                .toArray(String[]::new);

        InfixFilterArrayAdapter suraAdapter = new InfixFilterArrayAdapter(
                activity,
                android.R.layout.simple_spinner_dropdown_item, suras
        );
        suraInput.setAdapter(suraAdapter);

        // Ayah chooser
        ayahInput = layout.findViewById(R.id.ayah_spinner);

        // Page chooser
        pageInput = layout.findViewById(R.id.page_number);
        pageInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                dismiss();
                onSubmit();
                return true;
            } else {
                return false;
            }
        });

        pageInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String numberStr = s.toString();
                Integer pageNumber = tryParseInt(numberStr);
                if (suppressJump) {
                    pageInput.setTag(pageNumber);
                } else {
                    Integer selectedSura = (Integer) suraInput.getTag();
                    if (selectedSura != null) {
                        int sura = selectedSura;
                        int ayahCount = quranInfo.getNumberOfAyahs(sura);
                        int ayah = Math.min(Math.max(tryParseInt(numberStr, 1), 1), ayahCount);
                        int page = quranInfo.getPageFromSuraAyah(sura, ayah);
                        pageInput.setHint(QuranUtils.getLocalizedNumber(activity, page));
                        pageInput.setText(null);
                    }
                    pageInput.setTag(ayah);
                    if (!String.valueOf(pageNumber).equals(numberStr) && numberStr.length() > 0) {
                        s.replace(0, s.length(), String.valueOf(pageNumber));
                    }
                }
            }
        });

        suraInput.setOnForceCompleteListener((v, position, id) -> {
            String enteredText = suraInput.getText().toString();
            String suraName;
            if (position >= 0) {
                suraName = suraAdapter.getItem(position);
            } else if (Stream.of(suras).anyMatch(enteredText::equals)) {
                suraName = enteredText;
            } else if (suraAdapter.isEmpty()) {
                suraName = null;
            } else {
                suraName = suraAdapter.getItem(0);
            }
            int sura = Stream.of(suras).indexOf(suraName) + 1;
            if (sura == 0) {
                sura = 1;
            }
            suraInput.setTag(sura);
            suraInput.setText(suras[sura - 1]);
            String ayahValue = ayahInput.getText().toString();
            ayahInput.setText(ayahValue.isEmpty() ? " " : ayahValue);
        });

        ayahInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                Context context = getActivity();
                String ayahString = s.toString();
                int ayah = tryParseInt(ayahString, 1);
                if (suppressJump) {
                    ayahInput.setTag(ayah);
                } else {
                    Integer suraTag = (Integer) suraInput.getTag();
                    if (suraTag != null) {
                        int sura = suraTag;
                        int ayahCount = quranInfo.getNumberOfAyahs(sura);
                        ayah = Math.min(Math.max(ayah, 1), ayahCount);
                        int page = quranInfo.getPageFromSuraAyah(sura, ayah);
                        pageInput.setHint(QuranUtils.getLocalizedNumber(context, page));
                        pageInput.setText(null);
                    }
                    ayahInput.setTag(ayah);
                    String correctText = String.valueOf(ayah);
                    if (!correctText.equals(ayahString) && s.length() > 0) {
                        s.replace(0, s.length(), correctText);
                    }
                }
            }
        });

        builder.setView(layout);
        builder.setPositiveButton(getString(R.string.dialog_ok), (dialog, which) -> {
            layout.requestFocus();
            dismiss();
            onSubmit();
        });
        return builder.create();
    }

    private void onSubmit() {
        try {
            String pageStr = pageInput.getText().toString();
            Integer page = pageStr.isEmpty() ? Integer.parseInt(pageInput.getHint().toString()) : tryParseInt(pageStr);
            if (page != null) {
                Integer selectedSura = (Integer) suraInput.getTag();
                Integer selectedAyah = (Integer) ayahInput.getTag();
                if (getActivity() instanceof JumpDestination) {
                    ((JumpDestination) getActivity()).jumpToAndHighlight(page, selectedSura, selectedAyah);
                }
            }
        } catch (Exception e) {
            Timber.d(e, "Could not jump, something went wrong...");
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        ((QuranApplication) context.getApplicationContext()).getApplicationComponent().inject(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setSoftInputMode(
                    LayoutParams.SOFT_INPUT_STATE_VISIBLE | LayoutParams.SOFT_INPUT_ADJUST_PAN);
        }
    }

    /**
     * ListAdapter that supports filtering by using case-insensitive infix (substring).
     */
    private class InfixFilterArrayAdapter extends BaseAdapter implements Filterable {
        private final Context context;
        @LayoutRes
        private final int itemLayoutRes;
        private final List<String> originalItems;
        private List<String> items;
        private final LayoutInflater inflater;
        private final Filter filter = new ItemFilter();
        private final boolean isRtl;
        private final List<String> searchPreparedItems;

        InfixFilterArrayAdapter(Context context, @LayoutRes int itemLayoutRes, List<String> originalItems) {
            this.context = context;
            this.itemLayoutRes = itemLayoutRes;
            this.originalItems = originalItems;
            this.items = originalItems;
            this.inflater = LayoutInflater.from(context);
            this.isRtl = SearchTextUtil.isRtl(originalItems.get(0));
            this.searchPreparedItems = originalItems.stream()
                    .map(s -> prepareForSearch(s, isRtl))
                    .collect(Collectors.toList());
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public String getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView != null ? convertView : inflater.inflate(itemLayoutRes, parent, false);
            TextView text = (TextView) view;
            text.setText(getItem(position));
            return view;
        }

        @Override
        public Filter getFilter() {
            return filter;
        }

        private String prepareForSearch(String input, boolean isRtl) {
            return SearchTextUtil.asSearchableString(input, isRtl);
        }

        private class ItemFilter extends Filter {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();

                if (constraint == null || constraint.length() == 0) {
                    results.values = originalItems;
                    results.count = originalItems.size();
                } else {
                    String infix = cleanUpQueryString(constraint.toString());
                    String filteredIndex = tryParseInt(infix) != null ? tryParseInt(infix).toString() : null;
                    List<String> filteredCopy = originalItems.stream()
                            .filter((sura, index) ->
                                    searchPreparedItems.get(index).contains(infix) ||
                                            (filteredIndex != null && String.valueOf(index + 1).contains(filteredIndex)))
                            .collect(Collectors.toList());
                    results.values = filteredCopy;
                    results.count = filteredCopy.size();
                }
                return results;
            }

            private String cleanUpQueryString(String query) {
                return SearchTextUtil.isRtl(query) ? SearchTextUtil.asSearchableString(query, true) : query.toLowerCase();
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                items = (List<String>) results.values;
                if (results.count > 0) {
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }
        }
    }

    private Integer tryParseInt(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer tryParseInt(String text, int defaultValue) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static final String TAG = "JumpFragment";
}
