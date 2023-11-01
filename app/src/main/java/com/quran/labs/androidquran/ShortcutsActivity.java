package com.quran.labs.androidquran;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.ShortcutManager;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.quran.labs.androidquran.ui.QuranActivity;
import com.quran.labs.androidquran.widget.ShowJumpFragmentActivity;

/**
 * Handle shortcuts by launching the appropriate activity.
 * Currently, there is one shortcut to go to the last page,
 * and one shortcut to jump to any location in the Quran.
 */
public class ShortcutsActivity extends AppCompatActivity {

    public static final String ACTION_JUMP_TO_LATEST = "com.quran.labs.androidquran.last_page";
    public static final String ACTION_JUMP_TO = "com.quran.labs.androidquran.jump_to";
    private static final String JUMP_TO_LATEST_SHORTCUT_NAME = "lastPage";
    private static final String JUMP_TO_SHORTCUT_NAME = "jumpTo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String action = getIntent().getAction();
        Intent intentToLaunch;
        String shortcutSelected;
        switch (action) {
            case ACTION_JUMP_TO_LATEST:
                intentToLaunch = new Intent(this, QuranActivity.class);
                intentToLaunch.setAction(action);
                shortcutSelected = JUMP_TO_LATEST_SHORTCUT_NAME;
                break;
            case ACTION_JUMP_TO:
                intentToLaunch = new Intent(this, ShowJumpFragmentActivity.class);
                shortcutSelected = JUMP_TO_SHORTCUT_NAME;
                break;
            default:
                finish();
                return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            recordShortcutUsage(shortcutSelected);
        }
        finish();
        startActivity(intentToLaunch);
    }

    @TargetApi(Build.VERSION_CODES.N_MR1)
    private void recordShortcutUsage(String shortcut) {
        ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
        if (shortcutManager != null) {
            shortcutManager.reportShortcutUsed(shortcut);
        }
    }
}
