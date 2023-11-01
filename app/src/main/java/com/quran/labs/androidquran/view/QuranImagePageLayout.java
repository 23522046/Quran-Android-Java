package com.quran.labs.androidquran.view;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import com.quran.labs.androidquran.ui.helpers.AyahSelectedListener;
import com.quran.labs.androidquran.ui.util.PageController;
import com.quran.labs.androidquran.util.QuranSettings;

/**
 * Layout class for a single Arabic page of the Quran, with margins/background.
 * <p>
 * Note that the image of the Quran page to be displayed is set by users of this class by calling
 * {@link #getImageView()} and calling the appropriate methods on that view.
 */
public class QuranImagePageLayout extends QuranPageLayout {

    private HighlightingImageView imageView;

    public QuranImagePageLayout(Context context) {
        super(context);
        initialize();
    }

    @Override
    protected View generateContentView(Context context, boolean isLandscape) {
        imageView = new HighlightingImageView(context);
        imageView.setAdjustViewBounds(true);
        imageView.setIsScrollable(isLandscape && shouldWrapWithScrollView(), isLandscape);
        return imageView;
    }

    @Override
    protected void updateView(QuranSettings quranSettings) {
        super.updateView(quranSettings);
        imageView.setNightMode(
                quranSettings.isNightMode(),
                quranSettings.getNightModeTextBrightness(),
                quranSettings.getNightModeBackgroundBrightness()
        );
    }

    @Override
    protected void setPageController(PageController controller, int pageNumber, int skips) {
        super.setPageController(controller, pageNumber, skips);
        GestureDetector gestureDetector = new GestureDetector(getContext(), new PageGestureDetector());
        OnTouchListener gestureListener = (v, event) -> gestureDetector.onTouchEvent(event);
        imageView.setOnTouchListener(gestureListener);
        imageView.setClickable(true);
        imageView.setLongClickable(true);
    }

    public HighlightingImageView getImageView() {
        return imageView;
    }

    private class PageGestureDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            return pageController.handleTouchEvent(
                    event,
                    AyahSelectedListener.EventType.SINGLE_TAP,
                    pageNumber
            );
        }

        @Override
        public boolean onDoubleTap(MotionEvent event) {
            return pageController.handleTouchEvent(
                    event,
                    AyahSelectedListener.EventType.DOUBLE_TAP,
                    pageNumber
            );
        }

        @Override
        public void onLongPress(MotionEvent event) {
            pageController.handleTouchEvent(
                    event,
                    AyahSelectedListener.EventType.LONG_PRESS,
                    pageNumber
            );
        }
    }
}
