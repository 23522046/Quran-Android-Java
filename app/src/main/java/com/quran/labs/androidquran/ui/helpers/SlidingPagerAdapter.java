import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.quran.labs.androidquran.ui.fragment.AyahPlaybackFragment;
import com.quran.labs.androidquran.ui.fragment.AyahTranslationFragment;
import com.quran.labs.androidquran.ui.fragment.TagBookmarkFragment;
import com.quran.labs.androidquran.view.IconPageIndicator;
import com.quran.mobile.di.AyahActionFragmentProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SlidingPagerAdapter extends FragmentStatePagerAdapter implements IconPageIndicator.IconPagerAdapter {

    private final boolean isRtl;
    private final List<AyahActionFragmentProvider> pages;

    public SlidingPagerAdapter(FragmentManager fm, boolean isRtl, Set<AyahActionFragmentProvider> additionalPanels) {
        super(fm, "sliding");
        this.isRtl = isRtl;
        this.pages = new ArrayList<>();

        // Add the core ayah action panels
        pages.add(TagBookmarkFragment.Provider);
        pages.add(AyahTranslationFragment.Provider);
        pages.add(AyahPlaybackFragment.Provider);

        // Since additionalPanel Set may be unsorted, put them in a list and sort them by page number..
        List<AyahActionFragmentProvider> additionalPages = new ArrayList<>(additionalPanels);
        Collections.sort(additionalPages, (o1, o2) -> o1.getOrder() - o2.getOrder());
        // ..then add them to the pages list
        pages.addAll(additionalPages);
    }

    @Override
    public int getIconResId(int index) {
        int pos = getPagePosition(index);
        return pages.get(pos).getIconResId();
    }

    @Override
    public int getCount() {
        return pages.size();
    }

    @Override
    public Fragment getItem(int position) {
        int pos = getPagePosition(position);
        return pages.get(pos).newAyahActionFragment();
    }

    public int getPagePosition(int page) {
        return isRtl ? pages.size() - 1 - page : page;
    }

    public static final int TAG_PAGE = 0;
    public static final int TRANSLATION_PAGE = 1;
    public static final int AUDIO_PAGE = 2;
    public static final int TRANSCRIPT_PAGE = 3;
}
