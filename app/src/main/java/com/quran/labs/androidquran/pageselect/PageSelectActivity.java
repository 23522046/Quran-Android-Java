package com.quran.labs.androidquran.pageselect;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.quran.labs.androidquran.QuranApplication;
import com.quran.labs.androidquran.QuranDataActivity;
import com.quran.labs.androidquran.R;
import com.quran.labs.androidquran.ui.helpers.QuranDisplayHelper;
import com.quran.labs.androidquran.util.QuranSettings;

import java.util.List;

import javax.inject.Inject;

import kotlinx.coroutines.MainScope;
import kotlinx.coroutines.cancel;
import kotlinx.coroutines.launch;

public class PageSelectActivity extends AppCompatActivity {
  @Inject
  PageSelectPresenter presenter;

  @Inject
  QuranSettings quranSettings;

  private PageSelectAdapter adapter;
  private ViewPager viewPager;
  private LinearLayout layout;

  private MainScope scope = new MainScope();
  private boolean isProcessing = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ((QuranApplication) getApplication()).getApplicationComponent().inject(this);

    setContentView(R.layout.page_select);

    WindowManager windowManager = getWindowManager();
    int width = QuranDisplayHelper.getWidthKitKat(windowManager);

    adapter = new PageSelectAdapter(LayoutInflater.from(this), width, type -> onPageTypeSelected(type));

    viewPager = findViewById(R.id.pager);
    viewPager.setAdapter(adapter);

    int pageMargin = getResources().getDimensionPixelSize(R.dimen.page_margin);
    int pagerPadding = pageMargin * 2;
    viewPager.setPadding(pagerPadding, 0, pagerPadding, 0);
    viewPager.setClipToPadding(false);
    viewPager.setPageMargin(pageMargin);
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
    adapter.cleanUp();
    scope.cancel();
    super.onDestroy();
  }

  public void onUpdatedData(List<PageTypeItem> data) {
    adapter.replaceItems(data, viewPager);
  }

  private void onPageTypeSelected(String type) {
    String pageType = quranSettings.getPageType();
    if (!pageType.equals(type)) {
      isProcessing = true;
      scope.launch(() -> {
        presenter.migrateBookmarksData(pageType, type);
        quranSettings.removeDidDownloadPages();
        quranSettings.setPageType(type);
        Intent intent = new Intent(PageSelectActivity.this, QuranDataActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
      });
    } else {
      finish();
    }
    isProcessing = false;
  }
}
