package com.quran.labs.androidquran.ui.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.lifecycleScope;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;

import com.quran.labs.androidquran.BuildConfig;
import com.quran.labs.androidquran.QuranAdvancedPreferenceActivity;
import com.quran.labs.androidquran.QuranApplication;
import com.quran.labs.androidquran.QuranImportActivity;
import com.quran.labs.androidquran.R;
import com.quran.labs.androidquran.data.Constants;
import com.quran.labs.androidquran.model.bookmark.BookmarkImportExportModel;
import com.quran.labs.androidquran.service.util.PermissionUtil;
import com.quran.labs.androidquran.ui.preference.DataListPreference;
import com.quran.labs.androidquran.ui.util.ToastCompat;
import com.quran.labs.androidquran.util.QuranFileUtils;
import com.quran.labs.androidquran.util.QuranScreenInfo;
import com.quran.labs.androidquran.util.QuranSettings;
import com.quran.labs.androidquran.util.QuranUtils;
import com.quran.labs.androidquran.util.RecordingLogTree;
import com.quran.labs.androidquran.util.StorageUtils;
import com.quran.labs.androidquran.util.StorageUtils.Storage;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.observers.DisposableMaybeObserver;
import io.reactivex.rxjava3.observers.DisposableSingleObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.launch;
import kotlinx.coroutines.withContext;
import timber.log.Timber;

import java.io.File;
import java.util.HashMap;
import java.util.List;

public class QuranAdvancedSettingsFragment extends PreferenceFragmentCompat {
    private DataListPreference listStoragePref;
    private List<Storage> storageList;
    private Context appContext;

    private int appSize = 0;
    private boolean isPaused = false;
    private String internalSdcardLocation = null;
    private Dialog dialog = null;
    private Disposable exportSubscription = null;
    private Disposable logsSubscription = null;

    private BookmarkImportExportModel bookmarkImportExportModel;
    private QuranFileUtils quranFileUtils;
    private QuranScreenInfo quranScreenInfo;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.quran_advanced_preferences);
        Context context = requireActivity();
        appContext = context.getApplicationContext();

        // Field injection
        ((QuranApplication) appContext).getApplicationComponent().inject(this);

        Preference logsPref = findPreference(Constants.PREF_LOGS);
        if (BuildConfig.DEBUG || "beta".equals(BuildConfig.BUILD_TYPE)) {
            logsPref.setOnPreferenceClickListener(preference -> {
                if (logsSubscription == null) {
                    logsSubscription = Observable.fromIterable(Timber.forest())
                            .filter(tree -> tree instanceof RecordingLogTree)
                            .firstElement()
                            .map(tree -> ((RecordingLogTree) tree).getLogs())
                            .map(logs -> QuranUtils.getDebugInfo(appContext, quranScreenInfo) + "\n\n" + logs)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new DisposableMaybeObserver<String>() {
                                @Override
                                public void onSuccess(String logs) {
                                    Intent intent = new Intent(Intent.ACTION_SEND);
                                    intent.setType("message/rfc822");
                                    intent.putExtra(Intent.EXTRA_EMAIL, new String[]{appContext.getString(R.string.logs_email)});
                                    intent.putExtra(Intent.EXTRA_TEXT, logs);
                                    intent.putExtra(Intent.EXTRA_SUBJECT, "Logs");
                                    startActivity(Intent.createChooser(intent, appContext.getString(R.string.prefs_send_logs_title)));
                                    logsSubscription = null;
                                }

                                @Override
                                public void onError(Throwable e) {
                                }

                                @Override
                                public void onComplete() {
                                }
                            });
                }
                return true;
            });
        } else {
            removeAdvancePreference(logsPref);
        }

        Preference importPref = findPreference(Constants.PREF_IMPORT);
        importPref.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            String[] mimeTypes = {"application/*", "text/*"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            startActivityForResult(intent, REQUEST_CODE_IMPORT);
            return true;
        });

        Preference exportPref = findPreference(Constants.PREF_EXPORT);
        exportPref.setOnPreferenceClickListener(preference -> {
            if (exportSubscription == null) {
                exportSubscription = bookmarkImportExportModel.exportBookmarksObservable()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(new DisposableSingleObserver<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                onBookmarkExportSuccess(uri, context);
                                exportSubscription = null;
                            }

                            @Override
                            public void onError(Throwable e) {
                                exportSubscription = null;
                                onExportBookmarksError(context);
                            }
                        });
            }
            return true;
        });

        Preference exportCSVPref = findPreference(Constants.PREF_EXPORT_CSV);
        exportCSVPref.setOnPreferenceClickListener(preference -> {
            if (exportSubscription == null) {
                exportSubscription = bookmarkImportExportModel.exportBookmarksCSVObservable()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(new DisposableSingleObserver<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                onBookmarkExportSuccess(uri, context);
                                exportSubscription = null;
                            }

                            @Override
                            public void onError(Throwable e) {
                                exportSubscription = null;
                                onExportBookmarksError(context);
                            }
                        });
            }
            return true;
        });

        internalSdcardLocation = Environment.getExternalStorageDirectory().getAbsolutePath();
        listStoragePref = findPreference(getString(R.string.prefs_app_location));
        listStoragePref.setEnabled(false);
        try {
            storageList = StorageUtils.getAllStorageLocations(context.getApplicationContext());
            if (storageList.size() <= 1) {
                hideStorageListPref();
            } else {
                lifecycleScope.launch(() -> {
                    listStoragePref.setSummary(R.string.prefs_calculating_app_size);
                    appSize = quranFileUtils.getAppUsedSpace(appContext);

                    if (!isPaused) {
                        loadStorageOptions(appContext);
                        listStoragePref.setSummary(R.string.prefs_app_location_summary);
                    }
                });
            }
        } catch (Exception e) {
            Timber.e(e, "error loading storage options");
            hideStorageListPref();
        }
    }

    private void onBookmarkExportSuccess(Uri uri, Context context) {
        Intent shareIntent = createShareIntent(uri);
        List<ActivityInfo> intents = appContext.getPackageManager()
                .queryIntentActivities(shareIntent, 0);
        if (intents.size() > 1) {
            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.prefs_export_title)));
        } else {
            File exportedPath = new File(appContext.getExternalFilesDir(null), "backups");
            String exported = appContext.getString(R.string.exported_data, exportedPath.toString());
            ToastCompat.makeText(appContext, exported, Toast.LENGTH_LONG).show();
        }
    }

    private void onExportBookmarksError(Context context) {
        if (isAdded()) {
            ToastCompat.makeText(context, R.string.export_data_error, Toast.LENGTH_LONG).show();
        }
    }

    private Intent createShareIntent(Uri uri) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.setDataAndType(uri, "application/json");
        return shareIntent;
    }

    @Override
    public void onDestroy() {
        if (exportSubscription != null) {
            exportSubscription.dispose();
        }
        if (logsSubscription != null) {
            logsSubscription.dispose();
        }
        if (dialog != null) {
            dialog.dismiss();
        }
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_IMPORT && resultCode == Activity.RESULT_OK) {
            Activity activity = getActivity();
            if (activity != null) {
                Intent intent = new Intent(activity, QuranImportActivity.class);
                intent.setData(data.getData());
                startActivity(intent);
            }
        }
    }

    private void removeAdvancePreference(Preference preference) {
        PreferenceGroup group = findPreference(Constants.PREF_ADVANCED_QURAN_SETTINGS);
        if (group != null) {
            group.removePreference(preference);
        }
    }

    private void hideStorageListPref() {
        removeAdvancePreference(listStoragePref);
    }

    private void loadStorageOptions(Context context) {
        try {
            if (appSize == -1) {
                hideStorageListPref();
                return;
            }
            listStoragePref.setLabelsAndSummaries(context, appSize, storageList);
            HashMap<String, Storage> storageMap = new HashMap<>(storageList.size());
            for (Storage storage : storageList) {
                storageMap.put(storage.mountPoint, storage);
            }
            listStoragePref.setOnPreferenceChangeListener((preference, newValue) -> {
                Context context1 = requireActivity();
                QuranSettings settings = QuranSettings.getInstance(context1);
                if (TextUtils.isEmpty(settings.getAppCustomLocation()) && Environment.getExternalStorageDirectory().equals(newValue)) {
                    return false;
                }
                String newLocation = (String) newValue;
                Storage destStorage = storageMap.get(newLocation);
                String current = settings.getAppCustomLocation();
                if (appSize < destStorage.getFreeSpace()) {
                    if (current == null || !current.equals(newLocation)) {
                        handleMove(newLocation, destStorage);
                    }
                } else {
                    ToastCompat.makeText(context1, getString(R.string.prefs_no_enough_space_to_move_files), Toast.LENGTH_LONG).show();
                }
                return false;
            });
            listStoragePref.setEnabled(true);
        } catch (Exception e) {
            Timber.e(e, "error loading storage options");
            hideStorageListPref();
        }
    }

    private void requestExternalStoragePermission(String newLocation) {
        Activity activity = getActivity();
        if (activity instanceof QuranAdvancedPreferenceActivity) {
            ((QuranAdvancedPreferenceActivity) activity).requestWriteExternalSdcardPermission(newLocation);
        }
    }

    private void handleMove(String newLocation, Storage storageLocation) {
        if (newLocation.equals(internalSdcardLocation)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                showScopedStorageConfirmation(newLocation, storageLocation);
            } else {
                moveFiles(newLocation, storageLocation);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            moveFiles(newLocation, storageLocation);
        } else {
            showKitKatConfirmation(newLocation, storageLocation);
        }
    }

    private void showScopedStorageConfirmation(String newLocation, Storage storageLocation) {
        showConfirmation(newLocation, storageLocation, R.string.scoped_storage_message);
    }

    private void showKitKatConfirmation(String newLocation, Storage storageLocation) {
        showConfirmation(newLocation, storageLocation, R.string.kitkat_external_message);
    }

    private void showConfirmation(String newLocation, Storage storageLocation, @StringRes int message) {
        Context context = getActivity();
        if (context != null) {
            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setTitle(R.string.warning)
                    .setMessage(message)
                    .setPositiveButton(R.string.dialog_ok, (currentDialog, which) -> {
                        moveFiles(newLocation, storageLocation);
                        currentDialog.dismiss();
                        this.dialog = null;
                    })
                    .setNegativeButton(R.string.cancel, (currentDialog, which) -> {
                        currentDialog.dismiss();
                        this.dialog = null;
                    })
                    .create();
            dialog.show();
            this.dialog = dialog;
        }
    }

    private void moveFiles(String newLocation, Storage storageLocation) {
        Context context = getActivity();
        if (context != null) {
            if (storageLocation.doesRequirePermission() && !PermissionUtil.haveWriteExternalStoragePermission(context)) {
                requestExternalStoragePermission(newLocation);
            } else {
                moveFiles(newLocation);
            }
        }
    }

    public void moveFiles(String newLocation) {
        Context context = getActivity();
        if (context != null) {
            lifecycleScope.launch(() -> {
                ProgressDialog progressDialog = new ProgressDialog(context);
                progressDialog.setMessage(getString(R.string.prefs_copying_app_files));
                progressDialog.setCancelable(false);
                progressDialog.show();
                dialog = progressDialog;

                boolean result = quranFileUtils.moveAppFiles(appContext, newLocation);

                if (!isPaused) {
                    dialog.dismiss();
                    if (result) {
                        QuranSettings quranSettings = QuranSettings.getInstance(appContext);
                        quranSettings.setAppCustomLocation(newLocation);
                        quranSettings.removeDidDownloadPages();
                        listStoragePref.setValue(newLocation);
                    } else {
                        ToastCompat.makeText(appContext, getString(R.string.prefs_err_moving_app_files), Toast.LENGTH_LONG).show();
                    }
                    dialog = null;
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        isPaused = false;
    }

    @Override
    public void onPause() {
        isPaused = true;
        super.onPause();
    }

    private static final int REQUEST_CODE_IMPORT = 1;
}
