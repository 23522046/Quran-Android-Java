package com.quran.labs.androidquran.ui;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import com.quran.labs.androidquran.AboutUsActivity;
import com.quran.labs.androidquran.HelpActivity;
import com.quran.labs.androidquran.QuranApplication;
import com.quran.labs.androidquran.QuranPreferenceActivity;
import com.quran.labs.androidquran.R;
import com.quran.labs.androidquran.SearchActivity;
import com.quran.labs.androidquran.ShortcutsActivity;
import com.quran.labs.androidquran.data.Constants;
import com.quran.labs.androidquran.model.bookmark.RecentPageModel;
import com.quran.labs.androidquran.presenter.data.QuranIndexEventLogger;
import com.quran.labs.androidquran.presenter.translation.TranslationManagerPresenter;
import com.quran.labs.androidquran.service.AudioService;
import com.quran.labs.androidquran.ui.fragment.AddTagDialog;
import com.quran.labs.androidquran.ui.fragment.TagBookmarkDialog;
import com.quran.labs.androidquran.ui.helpers.JumpDestination;
import com.quran.labs.androidquran.util.AudioUtils;
import com.quran.labs.androidquran.util.QuranSettings;
import com.quran.labs.androidquran.util.QuranUtils;
import com.quran.labs.androidquran.view.SlidingTabLayout;
import com.quran.mobile.di.ExtraScreenProvider;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import timber.log.Timber;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.abs;

public class QuranActivity extends AppCompatActivity
        implements OnBookmarkTagsUpdateListener, JumpDestination {

    private AlertDialog upgradeDialog;
    private boolean showedTranslationUpgradeDialog;
    private boolean isRtl;
    private boolean isPaused;
    private MenuItem searchItem;
    private ActionMode supportActionMode;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private Observable<Integer> latestPageObservable;

    @Inject
    QuranSettings settings;

    @Inject
    AudioUtils audioUtils;

    @Inject
    RecentPageModel recentPageModel;

    @Inject
    TranslationManagerPresenter translationManagerPresenter;

    @Inject
    QuranIndexEventLogger quranIndexEventLogger;

    @Inject
    Set<ExtraScreenProvider> extraScreens;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        QuranApplication quranApp = (QuranApplication) getApplication();
        quranApp.refreshLocale(this, false);

        super.onCreate(savedInstanceState);
        quranApp.getApplicationComponent().quranActivityComponentBuilder().build().inject(this);

        setContentView(R.layout.quran_index);
        isRtl = isRtl();

        Toolbar tb = findViewById(R.id.toolbar);
        setSupportActionBar(tb);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle(R.string.app_name);
        }

        ViewPager pager = findViewById(R.id.index_pager);
        pager.setOffscreenPageLimit(3);
        PagerAdapter pagerAdapter = new PagerAdapter(getSupportFragmentManager());
        pager.setAdapter(pagerAdapter);

        SlidingTabLayout indicator = findViewById(R.id.indicator);
        indicator.setViewPager(pager);
        if (isRtl) {
            pager.setCurrentItem(TITLES.length - 1);
        }

        if (savedInstanceState != null) {
            showedTranslationUpgradeDialog = savedInstanceState.getBoolean(
                    SI_SHOWED_UPGRADE_DIALOG, false
            );
        }

        latestPageObservable = recentPageModel.getLatestPageObservable();
        Intent intent = getIntent();
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                if (extras.getBoolean(EXTRA_SHOW_TRANSLATION_UPGRADE, false)) {
                    if (!showedTranslationUpgradeDialog) {
                        showTranslationsUpgradeDialog();
                    }
                }
            }
            if (ShortcutsActivity.ACTION_JUMP_TO_LATEST.equals(intent.getAction())) {
                jumpToLastPage();
            }
        }
        updateTranslationsListAsNeeded();
        quranIndexEventLogger.logAnalytics();
    }

    @Override
    public void onResume() {
        compositeDisposable.add(latestPageObservable.subscribe(new io.reactivex.rxjava3.core.CompletableObserver() {
            @Override
            public void onSubscribe(Disposable d) {
                // Do nothing on subscribe
            }

            @Override
            public void onComplete() {
                // Do nothing on complete
            }

            @Override
            public void onError(Throwable e) {
                // Handle errors if needed
            }
        }));

        super.onResume();
        boolean isRtl = isRtl();
        if (isRtl != this.isRtl) {
            Intent i = getIntent();
            finish();
            startActivity(i);
        } else {
            compositeDisposable.add(
                    Completable.timer(500, MILLISECONDS)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new CompletableObserver() {
                                @Override
                                public void onSubscribe(Disposable d) {
                                    // Do nothing on subscribe
                                }

                                @Override
                                public void onComplete() {
                                    startService(
                                            audioUtils.getAudioIntent(QuranActivity.this, AudioService.ACTION_STOP)
                                    );
                                }

                                @Override
                                public void onError(Throwable e) {
                                    // Handle errors if needed
                                }
                            })
            );
        }
        isPaused = false;
    }


    @Override
    protected void onPause() {
        compositeDisposable.clear();
        isPaused = true;
        super.onPause();
    }

    private boolean isRtl() {
        return settings.isArabicNames() || QuranUtils.isRtl();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.home_menu, menu);
        searchItem = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView.setQueryHint(getString(R.string.search_hint));
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(
                        new ComponentName(this, SearchActivity.class)
                )
        );

        // Add additional injected screens (if any)
        for (ExtraScreenProvider screenProvider : extraScreens) {
            menu.add(Menu.NONE, screenProvider.getId(), Menu.NONE, screenProvider.getTitleResId());
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.settings:
                startActivity(new Intent(this, QuranPreferenceActivity.class));
                break;
            case R.id.last_page:
                jumpToLastPage();
                break;
            case R.id.help:
                startActivity(new Intent(this, HelpActivity.class));
                break;
            case R.id.about:
                startActivity(new Intent(this, AboutUsActivity.class));
                break;
            case R.id.jump:
                gotoPageDialog();
                break;
            case R.id.other_apps:
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://search?q=pub:quran.com"));
                if (getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) == null) {
                    intent.setData(Uri.parse("https://play.google.com/store/search?q=pub:quran.com"));
                }
                startActivity(intent);
                break;
            default:
                boolean handled = false;
                for (ExtraScreenProvider screenProvider : extraScreens) {
                    if (screenProvider.getId() == itemId) {
                        handled = screenProvider.onClick(this);
                        break;
                    }
                }
                return handled || super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onSupportActionModeFinished(ActionMode mode) {
        supportActionMode = null;
        super.onSupportActionModeFinished(mode);
    }

    @Override
    public void onSupportActionModeStarted(ActionMode mode) {
        supportActionMode = mode;
        super.onSupportActionModeStarted(mode);
    }

    @Override
    public void onBackPressed() {
        if (supportActionMode != null) {
            supportActionMode.finish();
        } else if (searchItem != null && searchItem.isActionViewExpanded()) {
            searchItem.collapseActionView();
        } else {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q &&
                    isTaskRoot() &&
                    (getSupportFragmentManager().getPrimaryNavigationFragment().getChildFragmentManager().getBackStackEntryCount() == 0) &&
                    getSupportFragmentManager().getBackStackEntryCount() == 0) {
                finishAfterTransition();
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(
                SI_SHOWED_UPGRADE_DIALOG,
                showedTranslationUpgradeDialog
        );
        super.onSaveInstanceState(outState);
    }

    private void jumpToLastPage() {
        compositeDisposable.add(
            latestPageObservable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new io.reactivex.rxjava3.core.Observer<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        // Not needed for your case
                    }

                    @Override
                    public void onNext(Integer recentPage) {
                        jumpTo((recentPage == Constants.NO_PAGE) ? 1 : recentPage);
                    }

                    @Override
                    public void onError(Throwable e) {
                        // Handle error if needed
                    }

                    @Override
                    public void onComplete() {
                        // Not needed for your case
                    }
                })
        );
    }


    private void updateTranslationsListAsNeeded() {
        if (!updatedTranslations) {
            long time = settings.getLastUpdatedTranslationDate();
            Timber.d("checking whether we should update translations..");
            if (System.currentTimeMillis() - time > Constants.TRANSLATION_REFRESH_TIME) {
                Timber.d("updating translations list...");
                updatedTranslations = true;
                translationManagerPresenter.checkForUpdates();
            }
        }
    }

    private void showTranslationsUpgradeDialog() {
        showedTranslationUpgradeDialog = true;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.translation_updates_available);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.translation_dialog_yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                upgradeDialog = null;
                launchTranslationActivity();
            }
        });

        builder.setNegativeButton(R.string.translation_dialog_later, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                upgradeDialog = null;
                // pretend we don't have updated translations.  we'll
                // check again after 10 days.
                settings.setHaveUpdatedTranslations(false);
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
        upgradeDialog = dialog;
    }


    private void launchTranslationActivity() {
        Intent i = new Intent(this, TranslationManagerActivity.class);
        startActivity(i);
    }

    @Override
    public void jumpTo(int page) {
        Intent i = new Intent(this, PagerActivity.class);
        i.putExtra("page", page);
        i.putExtra(PagerActivity.EXTRA_JUMP_TO_TRANSLATION, settings.wasShowingTranslation());
        startActivity(i);
    }

    @Override
    public void jumpToAndHighlight(int page, int sura, int ayah) {
        Intent i = new Intent(this, PagerActivity.class);
        i.putExtra("page", page);
        i.putExtra(PagerActivity.EXTRA_HIGHLIGHT_SURA, sura);
        i.putExtra(PagerActivity.EXTRA_HIGHLIGHT_AYAH, ayah);
        startActivity(i);
    }

    private void gotoPageDialog() {
        if (!isPaused) {
            FragmentManager fm = getSupportFragmentManager();
            JumpFragment jumpDialog = new JumpFragment();
            jumpDialog.show(fm, JumpFragment.TAG);
        }
    }

    public void addTag() {
        if (!isPaused) {
            FragmentManager fm = getSupportFragmentManager();
            AddTagDialog addTagDialog = new AddTagDialog();
            addTagDialog.show(fm, AddTagDialog.TAG);
        }
    }

    public void editTag(long id, String name) {
        if (!isPaused) {
            FragmentManager fm = getSupportFragmentManager();
            AddTagDialog addTagDialog = AddTagDialog.newInstance(id, name);
            addTagDialog.show(fm, AddTagDialog.TAG);
        }
    }

    public void tagBookmarks(long[] ids) {
        if (ids != null && ids.length == 1) {
            tagBookmark(ids[0]);
            return;
        }

        if (!isPaused) {
            FragmentManager fm = getSupportFragmentManager();
            TagBookmarkDialog tagBookmarkDialog = TagBookmarkDialog.newInstance(ids);
            tagBookmarkDialog.show(fm, TagBookmarkDialog.TAG);
        }
    }

    private void tagBookmark(long id) {
        if (!isPaused) {
            FragmentManager fm = getSupportFragmentManager();
            TagBookmarkDialog tagBookmarkDialog = TagBookmarkDialog.newInstance(id);
            tagBookmarkDialog.show(fm, TagBookmarkDialog.TAG);
        }
    }

    @Override
    public void onAddTagSelected() {
        FragmentManager fm = getSupportFragmentManager();
        AddTagDialog dialog = new AddTagDialog();
        dialog.show(fm, AddTagDialog.TAG);
    }

    private class PagerAdapter extends FragmentPagerAdapter {

        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public Fragment getItem(int position) {
            int pos = isRtl ? Math.abs(position - 2) : position;
            switch (pos) {
                case SURA_LIST:
                    return SuraListFragment.newInstance();
                case JUZ2_LIST:
                    return JuzListFragment.newInstance();
                case BOOKMARKS_LIST:
                    return BookmarksFragment.newInstance();
                default:
                    return BookmarksFragment.newInstance();
            }
        }

        @Override
        public long getItemId(int position) {
            int pos = isRtl ? Math.abs(position - 2) : position;
            switch (pos) {
                case SURA_LIST:
                    return SURA_LIST;
                case JUZ2_LIST:
                    return JUZ2_LIST;
                case BOOKMARKS_LIST:
                    return BOOKMARKS_LIST;
                default:
                    return BOOKMARKS_LIST;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            int resId = isRtl ? ARABIC_TITLES[position] : TITLES[position];
            return getString(resId);
        }
    }

    private static final int SURA_LIST = 0;
    private static final int JUZ2_LIST = 1;
    private static final int BOOKMARKS_LIST = 2;
    private static boolean updatedTranslations = false;
}
