package com.quran.labs.androidquran.view;

import android.content.Context;
import android.view.View;

public class QuranCustomImagePageLayout extends QuranPageLayout {

    private final View wrappedView;

    public QuranCustomImagePageLayout(Context context, View wrappedView) {
        super(context);
        this.wrappedView = wrappedView;
        setIsFullWidth(true);
        initialize();
    }

    @Override
    protected View generateContentView(Context context, boolean isLandscape) {
        return wrappedView;
    }

    @Override
    protected boolean shouldWrapWithScrollView() {
        return false;
    }
}
