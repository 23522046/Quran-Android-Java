package com.quran.labs.androidquran;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.quran.labs.androidquran.service.util.PermissionUtil;
import com.quran.labs.androidquran.ui.fragment.QuranAdvancedSettingsFragment;
import com.quran.labs.androidquran.ui.util.ToastCompat;
import com.quran.labs.androidquran.util.QuranSettings;

public class QuranAdvancedPreferenceActivity extends AppCompatActivity {

    private static final String SI_LOCATION_TO_WRITE = "SI_LOCATION_TO_WRITE";
    private static final int REQUEST_WRITE_TO_SDCARD_PERMISSION = 1;

    private String locationToWrite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((QuranApplication) getApplication()).refreshLocale(this, false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preferences);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.prefs_category_advanced);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState != null) {
            locationToWrite = savedInstanceState.getString(SI_LOCATION_TO_WRITE);
        }

        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.content);
        if (fragment == null) {
            fm.beginTransaction()
                    .replace(R.id.content, new QuranAdvancedSettingsFragment())
                    .commit();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (locationToWrite != null) {
            outState.putString(SI_LOCATION_TO_WRITE, locationToWrite);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void requestWriteExternalSdcardPermission(String newLocation) {
        if (PermissionUtil.canRequestWriteExternalStoragePermission(this)) {
            QuranSettings.getInstance(this).setSdcardPermissionsDialogPresented();
            locationToWrite = newLocation;
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_TO_SDCARD_PERMISSION
            );
        } else {
            // In the future, we should make this a direct link - perhaps using a Snackbar.
            ToastCompat.makeText(this, R.string.please_grant_permissions, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_WRITE_TO_SDCARD_PERMISSION) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && locationToWrite != null) {
                Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.content);
                if (fragment instanceof QuranAdvancedSettingsFragment) {
                    String location = locationToWrite;
                    if (location != null) {
                        ((QuranAdvancedSettingsFragment) fragment).moveFiles(location);
                    }
                }
            }
            locationToWrite = null;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
