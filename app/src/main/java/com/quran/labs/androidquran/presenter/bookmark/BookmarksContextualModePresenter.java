package com.quran.labs.androidquran.presenter.bookmark;

import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;

import com.quran.labs.androidquran.R;
import com.quran.labs.androidquran.presenter.Presenter;
import com.quran.labs.androidquran.ui.fragment.BookmarksFragment;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class BookmarksContextualModePresenter implements Presenter<BookmarksFragment> {

    private ActionMode actionMode;
    private BookmarksFragment fragment;
    private AppCompatActivity activity;

    @Inject
    public BookmarksContextualModePresenter() {
    }

    public boolean isInActionMode() {
        return actionMode != null;
    }

    private void startActionMode() {
        if (activity != null) {
            actionMode = activity.startSupportActionMode(new ModeCallback());
        }
    }

    public void invalidateActionMode(boolean startIfStopped) {
        if (actionMode != null) {
            actionMode.invalidate();
        } else if (startIfStopped) {
            startActionMode();
        }
    }

    public void finishActionMode() {
        if (actionMode != null) {
            actionMode.finish();
        }
    }

    @Override
    public void bind(BookmarksFragment what) {
        fragment = what;
        activity = (AppCompatActivity) what.getActivity();
    }

    @Override
    public void unbind(BookmarksFragment what) {
        if (what == fragment) {
            fragment = null;
            activity = null;
        }
    }

    private class ModeCallback implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            if (activity != null) {
                activity.getMenuInflater().inflate(R.menu.bookmark_contextual_menu, menu);
            }
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            if (fragment != null) {
                fragment.prepareContextualMenu(menu);
            }
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            boolean result = false;
            if (fragment != null) {
                result = fragment.onContextualActionClicked(item.getItemId());
            }
            finishActionMode();
            return result;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            if (fragment != null) {
                fragment.onCloseContextualActionMenu();
            }
            if (mode == actionMode) {
                actionMode = null;
            }
        }
    }
}
