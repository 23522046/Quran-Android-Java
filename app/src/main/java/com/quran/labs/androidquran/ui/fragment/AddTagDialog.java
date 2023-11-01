package com.quran.labs.androidquran.ui.fragment;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.quran.labs.androidquran.QuranApplication;
import com.quran.labs.androidquran.R;
import com.quran.data.model.bookmark.Tag;
import com.quran.labs.androidquran.presenter.bookmark.AddTagDialogPresenter;

import javax.inject.Inject;

public class AddTagDialog extends DialogFragment {
    
    @Inject
    AddTagDialogPresenter addTagDialogPresenter;
    
    private TextInputEditText textInputEditText;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        ((QuranApplication) context.getApplicationContext()).getApplicationComponent().inject(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        addTagDialogPresenter.bind(this);
    }

    @Override
    public void onStop() {
        addTagDialogPresenter.unbind(this);
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {
            Button positiveButton = dialog.getButton(Dialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(view -> {
                long id = getArguments().getLong(EXTRA_ID, -1);
                String name = textInputEditText.getText().toString();
                boolean success = addTagDialogPresenter.validate(name, id);
                if (success) {
                    if (id > -1) {
                        addTagDialogPresenter.updateTag(new Tag(id, name));
                    } else {
                        addTagDialogPresenter.addTag(name);
                    }
                    dismiss();
                }
            });
        }

        textInputEditText.requestFocus();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        long id = args.getLong(EXTRA_ID, -1);
        String originalName = args.getString(EXTRA_NAME, "");

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle(getString(R.string.tag_dlg_title));

        @SuppressLint("InflateParams")
        TextInputEditText text = (TextInputEditText) getLayoutInflater()
                .inflate(R.layout.tag_dialog, null);

        if (id > -1) {
            text.setText(originalName);
            text.setSelection(originalName.length());
        }

        textInputEditText = text;

        builder.setView(text);
        builder.setPositiveButton(getString(R.string.dialog_ok), null);

        return builder.create();
    }

    public void onBlankTagName() {
        textInputEditText.setError(getString(R.string.tag_blank_tag_error));
    }

    public void onDuplicateTagName() {
        textInputEditText.setError(getString(R.string.tag_duplicate_tag_error));
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE |
                            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        }
    }

    public static final String TAG = "AddTagDialog";

    private static final String EXTRA_ID = "id";
    private static final String EXTRA_NAME = "name";

    public static AddTagDialog newInstance(long id, String name) {
        Bundle args = new Bundle();
        args.putLong(EXTRA_ID, id);
        args.putString(EXTRA_NAME, name);
        AddTagDialog dialog = new AddTagDialog();
        dialog.setArguments(args);
        return dialog;
    }
}
