package com.quran.labs.androidquran.widget;

import android.content.Intent;
import android.os.Bundle;
import android.view.Window;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.quran.labs.androidquran.QuranApplication;
import com.quran.labs.androidquran.ui.PagerActivity;
import com.quran.labs.androidquran.ui.fragment.JumpFragment;
import com.quran.labs.androidquran.ui.helpers.JumpDestination;
import com.quran.labs.androidquran.util.QuranSettings;

import javax.inject.Inject;

/**
 * Transparent activity that just shows a [JumpFragment]. Clicking outside or closing the dialog
 * finishes the activity.
 */
public class ShowJumpFragmentActivity extends AppCompatActivity implements JumpDestination {

    @Inject
    QuranSettings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((QuranApplication) getApplication()).getApplicationComponent().inject(this);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    @Override
    protected void onStart() {
        if (getSupportFragmentManager().getFragments().isEmpty()) {
            getSupportFragmentManager().registerFragmentLifecycleCallbacks(
                    new FragmentManager.FragmentLifecycleCallbacks() {
                        @Override
                        public void onFragmentDestroyed(FragmentManager fm, Fragment f) {
                            finish();
                        }
                    }, false);
            new JumpFragment().show(getSupportFragmentManager(), JumpFragment.TAG);
        }
        super.onStart();
    }

    @Override
    public void jumpTo(int page) {
        Intent intent = new Intent(this, PagerActivity.class);
        intent.putExtra("page", page);
        intent.putExtra(PagerActivity.EXTRA_JUMP_TO_TRANSLATION, settings.wasShowingTranslation());
        startActivity(intent);
    }

    @Override
    public void jumpToAndHighlight(int page, int sura, int ayah) {
        Intent intent = new Intent(this, PagerActivity.class);
        intent.putExtra("page", page);
        intent.putExtra(PagerActivity.EXTRA_HIGHLIGHT_SURA, sura);
        intent.putExtra(PagerActivity.EXTRA_HIGHLIGHT_AYAH, ayah);
        startActivity(intent);
    }
}
