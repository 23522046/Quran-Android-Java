import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.quran.data.core.QuranInfo;
import com.quran.labs.androidquran.data.Constants;
import com.quran.labs.androidquran.ui.fragment.QuranPageFragment;
import com.quran.labs.androidquran.ui.fragment.TabletFragment;
import com.quran.labs.androidquran.ui.fragment.TranslationFragment;
import com.quran.page.common.data.PageMode;
import com.quran.page.common.factory.PageViewFactory;
import timber.log.Timber;

public class QuranPageAdapter extends FragmentStatePagerAdapter {
    private boolean isDualPages;
    private boolean isShowingTranslation;
    private QuranInfo quranInfo;
    private boolean isSplitScreen;
    private PageMode pageMode;
    private int totalPages;
    private int totalPagesDual;
    private PageViewFactory pageViewFactory;

    public QuranPageAdapter(
        FragmentManager fm,
        boolean isDualPages,
        boolean isShowingTranslation,
        QuranInfo quranInfo,
        boolean isSplitScreen,
        PageViewFactory pageViewFactory
    ) {
        super(fm, isDualPages ? "dualPages" : "singlePage");
        this.isDualPages = isDualPages;
        this.isShowingTranslation = isShowingTranslation;
        this.quranInfo = quranInfo;
        this.isSplitScreen = isSplitScreen;
        this.pageMode = makePageMode();
        this.totalPages = quranInfo.getNumberOfPagesConsideringSkipped();
        this.totalPagesDual = totalPages / 2 + (totalPages % 2);
        this.pageViewFactory = pageViewFactory;
    }

    public void setTranslationMode() {
        if (!isShowingTranslation) {
            isShowingTranslation = true;
            pageMode = makePageMode();
            notifyDataSetChanged();
        }
    }

    public void setQuranMode() {
        if (isShowingTranslation) {
            isShowingTranslation = false;
            pageMode = makePageMode();
            notifyDataSetChanged();
        }
    }

    private PageMode makePageMode() {
        if (isDualPages) {
            if (isShowingTranslation && isSplitScreen) {
                return PageMode.DualScreenMode.Mix;
            } else if (isShowingTranslation) {
                return PageMode.DualScreenMode.Translation;
            } else {
                return PageMode.DualScreenMode.Arabic;
            }
        } else {
            if (isShowingTranslation) {
                return PageMode.SingleTranslationPage;
            } else {
                return PageMode.SingleArabicPage;
            }
        }
    }

    @Override
    public int getItemPosition(Object currentItem) {
        return POSITION_NONE;
    }

    @Override
    public int getCount() {
        return isDualPagesVisible() ? totalPagesDual : totalPages;
    }

    @Override
    public Fragment getItem(int position) {
        int page = quranInfo.getPageFromPosition(position, isDualPagesVisible());
        Timber.d("getting page: %d, from position %d", page, position);
        if (pageViewFactory != null) {
            return pageViewFactory.providePage(page, pageMode);
        } else if (isDualPages) {
            return TabletFragment.newInstance(page, isShowingTranslation ?
                    TabletFragment.Mode.TRANSLATION : TabletFragment.Mode.ARABIC, isSplitScreen);
        } else if (isShowingTranslation) {
            return TranslationFragment.newInstance(page);
        } else {
            return QuranPageFragment.newInstance(page);
        }
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object currentItem) {
        Fragment f = (Fragment) currentItem;
        Timber.d("destroying item: %d, %s", position, f);
        cleanupFragment(f);
        super.destroyItem(container, position, currentItem);
    }

    public void cleanupFragment(Fragment f) {
        if (f instanceof QuranPageFragment) {
            ((QuranPageFragment) f).cleanup();
        } else if (f instanceof TabletFragment) {
            ((TabletFragment) f).cleanup();
        }
    }

    private boolean isDualPagesVisible() {
        return isDualPages && !(isSplitScreen && isShowingTranslation);
    }
}
