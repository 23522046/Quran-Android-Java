package com.quran.labs.androidquran.presenter.quran.ayahtracker;

import android.app.Activity;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.widget.ImageView;
import com.quran.data.core.QuranInfo;
import com.quran.data.di.QuranPageScope;
import com.quran.data.model.AyahGlyph.AyahEndGlyph;
import com.quran.data.model.AyahGlyph.WordGlyph;
import com.quran.data.model.AyahWord;
import com.quran.data.model.SuraAyah;
import com.quran.data.model.bookmark.Bookmark;
import com.quran.data.model.highlight.HighlightInfo;
import com.quran.data.model.highlight.HighlightType;
import com.quran.data.model.selection.AyahSelection;
import com.quran.data.model.selection.SelectionIndicator;
import com.quran.labs.androidquran.common.LocalTranslation;
import com.quran.labs.androidquran.common.QuranAyahInfo;
import com.quran.labs.androidquran.data.QuranDisplayData;
import com.quran.labs.androidquran.data.SuraAyahIterator;
import com.quran.labs.androidquran.presenter.Presenter;
import com.quran.labs.androidquran.presenter.quran.ayahtracker.AyahTrackerPresenter.AyahInteractionHandler;
import com.quran.labs.androidquran.ui.PagerActivity;
import com.quran.labs.androidquran.ui.helpers.AyahSelectedListener.EventType;
import com.quran.labs.androidquran.ui.helpers.AyahSelectedListener.EventType.DOUBLE_TAP;
import com.quran.labs.androidquran.ui.helpers.AyahSelectedListener.EventType.LONG_PRESS;
import com.quran.labs.androidquran.ui.helpers.AyahSelectedListener.EventType.SINGLE_TAP;
import com.quran.labs.androidquran.ui.helpers.AyahTracker;
import com.quran.labs.androidquran.ui.helpers.HighlightTypes;
import com.quran.labs.androidquran.util.QuranFileUtils;
import com.quran.labs.androidquran.util.QuranSettings;
import com.quran.mobile.bookmark.model.BookmarkModel;
import com.quran.page.common.data.AyahCoordinates;
import com.quran.page.common.data.PageCoordinates;
import com.quran.reading.common.AudioEventPresenter;
import com.quran.reading.common.ReadingEventPresenter;
import com.quran.recitation.events.RecitationEventPresenter;
import com.quran.recitation.presenter.RecitationHighlightsPresenter;
import com.quran.recitation.presenter.RecitationHighlightsPresenter.HighlightAction;
import com.quran.recitation.presenter.RecitationHighlightsPresenter.RecitationPage;
import com.quran.recitation.presenter.RecitationPopupPresenter;
import com.quran.recitation.presenter.RecitationPopupPresenter.PopupContainer;
import com.quran.recitation.presenter.RecitationPresenter;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.MainScope;
import kotlinx.coroutines.cancel;
import kotlinx.coroutines.flow.launchIn;
import kotlinx.coroutines.flow.onEach;
import javax.inject.Inject;

@QuranPageScope
public class AyahTrackerPresenter implements AyahTracker, Presenter<AyahInteractionHandler>, PopupContainer, RecitationPage {
    private final QuranInfo quranInfo;
    private final QuranFileUtils quranFileUtils;
    private final QuranDisplayData quranDisplayData;
    private final QuranSettings quranSettings;
    private final ReadingEventPresenter readingEventPresenter;
    private final BookmarkModel bookmarkModel;
    private final AudioEventPresenter audioEventPresenter;
    private final RecitationPresenter recitationPresenter;
    private final RecitationEventPresenter recitationEventPresenter;
    private final RecitationPopupPresenter recitationPopupPresenter;
    private final RecitationHighlightsPresenter recitationHighlightsPresenter;

    private CoroutineScope scope;
    private AyahTrackerItem[] items;
    private HighlightInfo pendingHighlightInfo;
    private SuraAyah lastHighlightedAyah;
    private SuraAyah lastHighlightedAudioAyah;
    private final boolean isRecitationEnabled;

    @Inject
    public AyahTrackerPresenter(QuranInfo quranInfo, QuranFileUtils quranFileUtils, QuranDisplayData quranDisplayData,
                                QuranSettings quranSettings, ReadingEventPresenter readingEventPresenter,
                                BookmarkModel bookmarkModel, AudioEventPresenter audioEventPresenter,
                                RecitationPresenter recitationPresenter, RecitationEventPresenter recitationEventPresenter,
                                RecitationPopupPresenter recitationPopupPresenter,
                                RecitationHighlightsPresenter recitationHighlightsPresenter) {
        this.quranInfo = quranInfo;
        this.quranFileUtils = quranFileUtils;
        this.quranDisplayData = quranDisplayData;
        this.quranSettings = quranSettings;
        this.readingEventPresenter = readingEventPresenter;
        this.bookmarkModel = bookmarkModel;
        this.audioEventPresenter = audioEventPresenter;
        this.recitationPresenter = recitationPresenter;
        this.recitationEventPresenter = recitationEventPresenter;
        this.recitationPopupPresenter = recitationPopupPresenter;
        this.recitationHighlightsPresenter = recitationHighlightsPresenter;

        this.isRecitationEnabled = recitationPresenter.isRecitationEnabled();
    }

    private void subscribe() {
        readingEventPresenter.ayahSelectionFlow()
            .onEach(this::onAyahSelectionChanged)
            .launchIn(scope);

        audioEventPresenter.audioPlaybackAyahFlow()
            .onEach(this::onAudioSelectionChanged)
            .launchIn(scope);

        for (AyahTrackerItem item : items) {
            bookmarkModel.bookmarksForPage(item.getPage())
                .onEach(this::onBookmarksChanged)
                .launchIn(scope);
        }
    }

    public void setPageBounds(PageCoordinates pageCoordinates) {
        for (AyahTrackerItem item : items) {
            item.onSetPageBounds(pageCoordinates);
        }
    }

    public void setAyahCoordinates(AyahCoordinates ayahCoordinates) {
        for (AyahTrackerItem item : items) {
            item.onSetAyahCoordinates(ayahCoordinates);
        }

        HighlightInfo pendingHighlightInfo = this.pendingHighlightInfo;
        if (pendingHighlightInfo != null && ayahCoordinates.getAyahCoordinates().size() > 0) {
            highlightAyah(pendingHighlightInfo.getSura(), pendingHighlightInfo.getAyah(),
                pendingHighlightInfo.getWord(), pendingHighlightInfo.getHighlightType(),
                pendingHighlightInfo.isScrollToAyah());
        }

        if (isRecitationEnabled) {
            recitationHighlightsPresenter.refresh();
        }
    }

    private void onAyahSelectionChanged(AyahSelection ayahSelection) {
        SuraAyah startSuraAyah = ayahSelection.startSuraAyah();

        // Optimization - if the current ayah is still highlighted, don't issue a request to unhighlight.
        if (!startSuraAyah.equals(lastHighlightedAyah)) {
            unHighlightAyahs(HighlightTypes.SELECTION);
            lastHighlightedAyah = startSuraAyah;
        }

        if (ayahSelection instanceof AyahSelection.Ayah) {
            SuraAyah suraAyah = ((AyahSelection.Ayah) ayahSelection).getSuraAyah();
            boolean scrollToAyah = ((AyahSelection.Ayah) ayahSelection).getSelectionIndicator() == SelectionIndicator.ScrollOnly;
            highlightAyah(suraAyah.getSura(), suraAyah.getAyah(), -1, HighlightTypes.SELECTION, scrollToAyah);
        } else if (ayahSelection instanceof AyahSelection.AyahRange) {
            SuraAyahIterator highlightAyatIterator = new SuraAyahIterator(quranInfo, ((AyahSelection.AyahRange) ayahSelection).getStartSuraAyah(), ((AyahSelection.AyahRange) ayahSelection).getEndSuraAyah());
            Set<String> highlightedAyat = highlightAyatIterator.asSet();

            for (AyahTrackerItem item : items) {
                Set<String> pageAyat = quranDisplayData.getAyahKeysOnPage(item.getPage());
                Set<String> elements = intersection(pageAyat, highlightedAyat);
                if (!elements.isEmpty()) {
                    item.onHighlightAyat(item.getPage(), elements, HighlightTypes.SELECTION);
                }
            }
        } else {
            // Nothing is selected, and we already cleared.
        }
    }

    private static Set<String> intersection(Set<String> set1, Set<String> set2) {
        Set<String> result = new HashSet<>(set1);
        result.retainAll(set2);
        return result;
    }

    private void onAudioSelectionChanged(SuraAyah suraAyah) {
        SuraAyah currentLastHighlightAudioAyah = lastHighlightedAudioAyah;

        // If there is no currently highlighted audio ayah, go ahead and clear all.
        if (currentLastHighlightAudioAyah == null) {
            unHighlightAyahs(HighlightTypes.AUDIO);
        }

        // Either way, whether we unhighlighted all or not, highlight the new ayah.
        if (suraAyah != null) {
            highlightAyah(suraAyah.getSura(), suraAyah.getAyah(), -1, HighlightTypes.AUDIO, true);
        }

        // If we had a highlighted ayah before, unhighlight it now.
        // We do this *after* highlighting the new ayah so that the animations continue working.
        if (suraAyah != currentLastHighlightAudioAyah && currentLastHighlightAudioAyah != null) {
            int sura = currentLastHighlightAudioAyah.getSura();
            int ayah = currentLastHighlightAudioAyah.getAyah();
            int page = quranInfo.getPageFromSuraAyah(sura, ayah);

            for (AyahTrackerItem item : items) {
                if (item.getPage() == page) {
                    item.onUnHighlightAyah(page, sura, ayah, -1, HighlightTypes.AUDIO);
                }
            }
        }

        // Keep track of the last highlighted audio ayah.
        lastHighlightedAudioAyah = suraAyah;
    }

    private void onBookmarksChanged(List<Bookmark> bookmarks) {
        unHighlightAyahs(HighlightTypes.BOOKMARK);
        if (quranSettings.shouldHighlightBookmarks()) {
            for (AyahTrackerItem tracker : items) {
                List<Bookmark> pageBookmarks = new ArrayList<>();
                for (Bookmark bookmark : bookmarks) {
                    if (bookmark.getPage() == tracker.getPage()) {
                        pageBookmarks.add(bookmark);
                    }
                }

                Set<String> elements = new HashSet<>();
                for (Bookmark bookmark : pageBookmarks) {
                    elements.add(bookmark.getSura() + ":" + bookmark.getAyah());
                }

                if (!elements.isEmpty()) {
                    tracker.onHighlightAyat(tracker.getPage(), elements, HighlightTypes.BOOKMARK);
                }
            }
        }
    }

    private void highlightAyah(int sura, int ayah, int word, HighlightType type, boolean scrollToAyah) {
        int page = quranInfo.getPageFromSuraAyah(sura, ayah);
        boolean handled = false;

        for (AyahTrackerItem item : items) {
            if (item.getPage() == page) {
                handled = item.onHighlightAyah(page, sura, ayah, word, type, scrollToAyah);
                if (handled) {
                    break;
                }
            }
        }

        if (!handled) {
            pendingHighlightInfo = new HighlightInfo(sura, ayah, word, type, scrollToAyah);
        } else {
            pendingHighlightInfo = null;
        }
    }

    private void unHighlightAyahs(HighlightType type) {
        for (AyahTrackerItem item : items) {
            item.onUnHighlightAyahType(type);
        }
    }

    @Override
    public SelectionIndicator getToolBarPosition(int sura, int ayah) {
        int page = quranInfo.getPageFromSuraAyah(sura, ayah);
        for (AyahTrackerItem item : items) {
            SelectionIndicator position = item.getToolBarPosition(page, sura, ayah);
            if (position != SelectionIndicator.None && position != SelectionIndicator.ScrollOnly) {
                return position;
            }
        }
        return SelectionIndicator.None;
    }

    @Override
    public QuranAyahInfo getQuranAyahInfo(int sura, int ayah) {
        for (AyahTrackerItem item : items) {
            QuranAyahInfo quranAyahInfo = item.getQuranAyahInfo(sura, ayah);
            if (quranAyahInfo != null) {
                return quranAyahInfo;
            }
        }
        return null;
    }

    @Override
    public LocalTranslation[] getLocalTranslations() {
        for (AyahTrackerItem item : items) {
            LocalTranslation[] localTranslations = item.getLocalTranslations();
            if (localTranslations != null) {
                return localTranslations;
            }
        }
        return null;
    }

    public void onPressIgnoringSelectionState() {
        readingEventPresenter.onClick();
    }

    public void onLongPress(SuraAyah suraAyah) {
        handleLongPress(suraAyah);
    }

    public void endAyahMode() {
        readingEventPresenter.onAyahSelection(AyahSelection.None);
    }

    public boolean handleTouchEvent(Activity activity, MotionEvent event, EventType eventType, int page, boolean ayahCoordinatesError) {
        if (eventType == DOUBLE_TAP) {
            readingEventPresenter.onAyahSelection(AyahSelection.None);
        } else if (eventType == LONG_PRESS || readingEventPresenter.currentAyahSelection() != AyahSelection.None) {
            if (ayahCoordinatesError) {
                checkCoordinateData(activity);
            } else {
                handleAyahSelection(event, eventType, page);
            }
        } else if (eventType == SINGLE_TAP && recitationEventPresenter.hasRecitationSession()) {
            handleTap(event, eventType, page);
        } else {
            readingEventPresenter.onClick();
        }
        return true;
    }

    private void handleAyahSelection(MotionEvent ev, EventType eventType, int page) {
        SuraAyah result = getAyahForPosition(page, ev.getX(), ev.getY());
        if (result != null) {
            if (eventType == SINGLE_TAP) {
                readingEventPresenter.onAyahSelection(new AyahSelection.Ayah(result, getToolBarPosition(result.getSura(), result.getAyah())));
            } else if (eventType == LONG_PRESS) {
                handleLongPress(result);
            }
        }
    }

    private void handleTap(MotionEvent ev, EventType eventType, int page) {
        for (AyahTrackerItem item : items) {
            WordGlyph glyph = item.getGlyphForPosition(page, ev.getX(), ev.getY());
            if (glyph != null) {
                AyahWord portion;
                if (glyph instanceof WordGlyph) {
                    portion = ((WordGlyph) glyph).toAyahWord();
                } else if (glyph instanceof AyahEndGlyph) {
                    portion = ((AyahEndGlyph) glyph).getAyah();
                } else {
                    portion = (AyahWord) glyph;
                }
                readingEventPresenter.onClick(portion);
            }
        }
    }

    private void handleLongPress(SuraAyah selectedSuraAyah) {
        AyahSelection current = readingEventPresenter.currentAyahSelection();
        AyahSelection updatedAyahSelection = updateAyahRange(selectedSuraAyah, current);
        readingEventPresenter.onAyahSelection(updatedAyahSelection);
    }

    private AyahSelection updateAyahRange(SuraAyah selectedAyah, AyahSelection ayahSelection) {
        SuraAyah startAyah;
        SuraAyah endAyah = null;

        if (ayahSelection instanceof AyahSelection.None) {
            startAyah = selectedAyah;
        } else if (ayahSelection instanceof AyahSelection.Ayah) {
            SuraAyah currentAyah = ((AyahSelection.Ayah) ayahSelection).getSuraAyah();
            if (selectedAyah.compareTo(currentAyah) > 0) {
                startAyah = currentAyah;
                endAyah = selectedAyah;
            } else {
                startAyah = selectedAyah;
                endAyah = currentAyah;
            }
        } else if (ayahSelection instanceof AyahSelection.AyahRange) {
            SuraAyah startSuraAyah = ((AyahSelection.AyahRange) ayahSelection).getStartSuraAyah();
            if (selectedAyah.compareTo(startSuraAyah) > 0) {
                startAyah = startSuraAyah;
                endAyah = selectedAyah;
            } else {
                startAyah = selectedAyah;
                endAyah = startSuraAyah;
            }
        } else {
            startAyah = selectedAyah;
        }

        SelectionIndicator toolBarPosition = getToolBarPosition(selectedAyah.getSura(), selectedAyah.getAyah());
        if (endAyah == null) {
            return new AyahSelection.Ayah(startAyah, toolBarPosition);
        } else {
            return new AyahSelection.AyahRange(startAyah, endAyah, toolBarPosition);
        }
    }

    private SuraAyah getAyahForPosition(int page, float x, float y) {
        for (AyahTrackerItem item : items) {
            SuraAyah ayah = item.getAyahForPosition(page, x, y);
            if (ayah != null) {
                return ayah;
            }
        }
        return null;
    }

    private void checkCoordinateData(Activity activity) {
        if (activity instanceof PagerActivity && (!quranFileUtils.haveAyaPositionFile(activity) || !quranFileUtils.hasArabicSearchDatabase())) {
            ((PagerActivity) activity).showGetRequiredFilesDialog();
        }
    }

    @Override
    public void bind(AyahInteractionHandler what) {
        items = what.ayahTrackerItems;
        scope = new MainScope();
        if (isRecitationEnabled) {
            recitationPopupPresenter.bind(this);
            recitationHighlightsPresenter.bind(this);
        }
        subscribe();
    }

    @Override
    public void unbind(AyahInteractionHandler what) {
        if (isRecitationEnabled) {
            recitationHighlightsPresenter.unbind(this);
            recitationPopupPresenter.unbind(this);
        }
        items = new AyahTrackerItem[0];
        scope.cancel();
    }

    @Override
    public ImageView getQuranPageImageView(int page) {
        AyahTrackerItem item = null;
        for (AyahTrackerItem tracker : items) {
            if (tracker.getPage() == page) {
                item = tracker;
                break;
            }
        }
        if (item == null) {
            return null;
        }
        ImageView ayahView = item.getAyahView();
        if (ayahView instanceof ImageView) {
            return (ImageView) ayahView;
        }
        return null;
    }

    @Override
    public SelectionIndicator.SelectedItemPosition getSelectionBoundsForWord(int page, AyahWord word) {
        AyahTrackerItem item = null;
        for (AyahTrackerItem tracker : items) {
            if (tracker.getPage() == page) {
                item = tracker;
                break;
            }
        }
        if (item == null) {
            return null;
        }
        SelectionIndicator position = item.getToolBarPosition(item.getPage(), word);
        if (position instanceof SelectionIndicator.SelectedItemPosition) {
            return (SelectionIndicator.SelectedItemPosition) position;
        }
        return null;
    }

    @Override
    public List<RectF> getBoundsForWord(AyahWord word) {
        if (items.length > 0 && items[0] instanceof AyahImageTrackerItem) {
            return ((AyahImageTrackerItem) items[0]).getPageGlyphsCoords().getBoundsForWord(word);
        }
        return null;
    }

    @Override
    public Set<Integer> getPageNumbers() {
        Set<Integer> pageNumbers = new HashSet<>();
        for (AyahTrackerItem item : items) {
            pageNumbers.add(item.getPage());
        }
        return pageNumbers;
    }

    @Override
    public void applyHighlights(List<HighlightAction> highlights) {
        for (HighlightAction action : highlights) {
            int sura = action.getSura();
            int ayah = action.getAyah();
            int page = quranInfo.getPageFromSuraAyah(sura, ayah);

            for (AyahTrackerItem item : items) {
                if (item.getPage() == page) {
                    if (action.getAction() == HighlightAction.ActionType.HIGHLIGHT) {
                        item.onHighlightAyah(page, sura, ayah, -1, action.getHighlightType(), false);
                    } else if (action.getAction() == HighlightAction.ActionType.UNHIGHLIGHT) {
                        item.onUnHighlightAyah(page, sura, ayah, -1, action.getHighlightType());
                    }
                }
            }
        }
    }

    public interface AyahInteractionHandler {
        AyahTrackerItem[] ayahTrackerItems;
    }
}
