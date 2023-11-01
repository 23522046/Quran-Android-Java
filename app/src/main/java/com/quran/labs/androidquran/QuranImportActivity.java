package com.quran.labs.androidquran;

import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.quran.data.model.bookmark.BookmarkData;
import com.quran.labs.androidquran.presenter.QuranImportPresenter;
import com.quran.labs.androidquran.ui.util.ToastCompat;

import javax.inject.Inject;

public class QuranImportActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private AlertDialog alertDialog = null;

    @Inject
    QuranImportPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        QuranApplication quranApp = (QuranApplication) getApplication();
        quranApp.refreshLocale(this, false);
        super.onCreate(savedInstanceState);
        quranApp.getApplicationComponent().inject(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        presenter.bind(this);
    }

    @Override
    protected void onPause() {
        presenter.unbind(this);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        presenter.unbind(this);
        if (alertDialog != null) {
            alertDialog.dismiss();
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        presenter.onPermissionsResult(requestCode, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public boolean isShowingDialog() {
        return alertDialog != null;
    }

    public void showImportConfirmationDialog(BookmarkData bookmarkData) {
        String dialogMessage = getString(R.string.import_data_and_override, bookmarkData.getBookmarks().size(), bookmarkData.getTags().size());
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(dialogMessage)
                .setPositiveButton(R.string.import_data, (dialog, which) -> presenter.importData(bookmarkData))
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> finish())
                .setOnCancelListener(dialog -> finish());
        alertDialog = builder.show();
    }

    public void showImportComplete() {
        ToastCompat.makeText(this, R.string.import_successful, Toast.LENGTH_LONG).show();
        finish();
    }

    public void showError() {
        showErrorInternal(R.string.import_data_error);
    }

    public void showPermissionsError() {
        showErrorInternal(R.string.import_data_permissions_error);
    }

    private void showErrorInternal(@StringRes int messageId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(messageId)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> finish());
        alertDialog = builder.show();
    }
}
