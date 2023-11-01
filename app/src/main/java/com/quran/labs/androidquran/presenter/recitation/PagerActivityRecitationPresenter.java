import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import com.quran.data.core.QuranInfo;
import com.quran.data.model.SuraAyah;
import com.quran.labs.androidquran.common.toolbar.R;
import com.quran.labs.androidquran.ui.PagerActivity;
import com.quran.labs.androidquran.ui.helpers.SlidingPagerAdapter;
import com.quran.labs.androidquran.view.AudioStatusBar;
import com.quran.page.common.toolbar.AyahToolBar;
import com.quran.reading.common.ReadingEventPresenter;
import com.quran.recitation.common.RecitationSession;
import com.quran.recitation.events.RecitationEventPresenter;
import com.quran.recitation.events.RecitationPlaybackEventPresenter;
import com.quran.recitation.events.RecitationSelection;
import com.quran.recitation.presenter.RecitationPlaybackPresenter;
import com.quran.recitation.presenter.RecitationPresenter;
import com.quran.recitation.presenter.RecitationSettings;
import kotlinx.coroutines.MainScope;
import timber.log.Timber;

public class PagerActivityRecitationPresenter implements AudioStatusBar.AudioBarRecitationListener, DefaultLifecycleObserver, LifecycleEventObserver {
    private final MainScope scope = new MainScope();

    private Bridge bridge;

    public static class Bridge {
        public final Boolean isDualPageVisible;
        public final Integer currentPage;
        public final AudioStatusBar audioStatusBar;
        public final AyahToolBar ayahToolBar;
        public final Consumer<SuraAyah> ensurePage;
        public final Consumer<Integer> showSlider;

        public Bridge(Boolean isDualPageVisible, Integer currentPage, AudioStatusBar audioStatusBar, AyahToolBar ayahToolBar, Consumer<SuraAyah> ensurePage, Consumer<Integer> showSlider) {
            this.isDualPageVisible = isDualPageVisible;
            this.currentPage = currentPage;
            this.audioStatusBar = audioStatusBar;
            this.ayahToolBar = ayahToolBar;
            this.ensurePage = ensurePage;
            this.showSlider = showSlider;
        }
    }

    private final QuranInfo quranInfo;
    private final ReadingEventPresenter readingEventPresenter;
    private final RecitationPresenter recitationPresenter;
    private final RecitationEventPresenter recitationEventPresenter;
    private final RecitationPlaybackPresenter recitationPlaybackPresenter;
    private final RecitationPlaybackEventPresenter recitationPlaybackEventPresenter;
    private final RecitationSettings recitationSettings;

    public PagerActivityRecitationPresenter(QuranInfo quranInfo, ReadingEventPresenter readingEventPresenter, RecitationPresenter recitationPresenter, RecitationEventPresenter recitationEventPresenter, RecitationPlaybackPresenter recitationPlaybackPresenter, RecitationPlaybackEventPresenter recitationPlaybackEventPresenter, RecitationSettings recitationSettings) {
        this.quranInfo = quranInfo;
        this.readingEventPresenter = readingEventPresenter;
        this.recitationPresenter = recitationPresenter;
        this.recitationEventPresenter = recitationEventPresenter;
        this.recitationPlaybackPresenter = recitationPlaybackPresenter;
        this.recitationPlaybackEventPresenter = recitationPlaybackEventPresenter;
        this.recitationSettings = recitationSettings;
    }

    public void bind(PagerActivity activity, Bridge bridge) {
        if (!isRecitationEnabled()) return;
        this.bridge = bridge;
        activity.getLifecycle().addObserver(this);
    }

    public void unbind(PagerActivity activity) {
        activity.getLifecycle().removeObserver(this);
        scope.cancel();
    }

    private boolean isRecitationEnabled() {
        return recitationPresenter.isRecitationEnabled();
    }

    @Override
    public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
        if (!isRecitationEnabled()) return;

        PagerActivity activity = (PagerActivity) source;
        switch (event) {
            case ON_CREATE:
                recitationPresenter.bind(activity);
                bridge.audioStatusBar.setAudioBarRecitationListener(this);
                // Show recitation button in audio bar and ayah toolbar
                onRecitationEnabledStateChanged(true);
                subscribe();
                break;
            case ON_RESUME:
                recitationPlaybackPresenter.bind(activity);
                break;
            case ON_PAUSE:
                recitationPlaybackPresenter.unbind(activity);
                break;
            case ON_DESTROY:
                recitationPresenter.unbind(activity);
                unbind(activity);
                break;
            default:
                break;
        }
    }

    private void subscribe() {
        // Handle Changes to Recitation Enabled
        recitationPresenter.isRecitationEnabledFlow()
                .onEach(this::onRecitationEnabledStateChanged)
                .launchIn(scope);

        // Recitation Events
        recitationEventPresenter.listeningStateFlow
                .onEach(this::onListeningStateChange)
                .launchIn(scope);
        recitationEventPresenter.recitationChangeFlow
                .onEach(this::onRecitationChange)
                .launchIn(scope);
        recitationEventPresenter.recitationSessionFlow
                .onEach(this::onRecitationSessionChange)
                .launchIn(scope);
        recitationEventPresenter.recitationSelectionFlow
                .onEach(this::onRecitationSelection)
                .launchIn(scope);

        // Recitation Playback Events
        recitationPlaybackEventPresenter.playingStateFlow
                .onEach(this::onRecitationPlayingState)
                .launchIn(scope);
        recitationPlaybackPresenter.recitationPlaybackFlow
                .onEach(this::onRecitationPlayback)
                .launchIn(scope);
    }

    public void onSessionEnd() {
        // End recitation service if running
        if (isRecitationEnabled() && recitationEventPresenter.hasRecitationSession()) {
            recitationPresenter.endSession();
        }
    }

    public void onPermissionsResult(int requestCode, int[] grantResults) {
        recitationPresenter.onRequestPermissionsResult(requestCode, grantResults);
    }

    // Recitation Events

    private void onRecitationEnabledStateChanged(boolean isEnabled) {
        bridge.audioStatusBar.apply(() -> {
            if (audioStatusBar.isRecitationEnabled() != isEnabled) {
                audioStatusBar.setRecitationEnabled(isEnabled);
                audioStatusBar.switchMode(audioStatusBar.getCurrentMode(), true);
            }
        });
        bridge.ayahToolBar.apply(() -> {
            if (ayahToolBar.isRecitationEnabled() != isEnabled) {
                ayahToolBar.setRecitationEnabled(isEnabled);
                ayahToolBar.setMenuItemVisibility(R.id.cab_recite_from_here, isEnabled);
            }
        });
    }

    private void onListeningStateChange(boolean isListening) {
        refreshAudioStatusBarRecitationState();
    }

    private void onRecitationChange(SuraAyah ayah) {
        SuraAyah curAyah = recitationEventPresenter.recitationSession() != null ?
                recitationEventPresenter.recitationSession().currentAyah() : ayah;
        bridge.ensurePage(curAyah);
        // temp workaround for forced into stopped mode on rotation because of audio service CONNECT
        refreshAudioStatusBarRecitationState();
    }

    private void onRecitationSessionChange(RecitationSession session) {
        refreshAudioStatusBarRecitationState();
    }

    private void onRecitationSelection(RecitationSelection selection) {
        SuraAyah ayah = selection.ayah();
        if (ayah != null) {
            bridge.ensurePage(ayah);
        }
    }

    private void onRecitationPlayingState(boolean isPlaying) {
        refreshAudioStatusBarRecitationState();
    }

    private void onRecitationPlayback(RecitationSelection playback) {
        SuraAyah ayah = playback.ayah();
        if (ayah != null) {
            bridge.ensurePage(ayah);
        }
        // temp workaround for forced into stopped mode on rotation because of audio service CONNECT
        refreshAudioStatusBarRecitationState();
    }

    private void refreshAudioStatusBarRecitationState() {
        AudioStatusBar audioStatusBar = bridge.audioStatusBar();
        if (audioStatusBar == null) return;

        boolean hasSession = recitationEventPresenter.hasRecitationSession();
        boolean isListening = recitationEventPresenter.isListening();
        boolean isPlaying = recitationPlaybackEventPresenter.isPlaying();

        int curMode = audioStatusBar.getCurrentMode();
        int newMode;
        if (!hasSession) {
            newMode = AudioStatusBar.STOPPED_MODE;
        } else if (isListening) {
            newMode = AudioStatusBar.RECITATION_LISTENING_MODE;
        } else if (isPlaying) {
            newMode = AudioStatusBar.RECITATION_PLAYING_MODE;
        } else {
            newMode = AudioStatusBar.RECITATION_STOPPED_MODE;
        }

        if (newMode != curMode) {
            audioStatusBar.switchMode(newMode);
        }
    }

    // AudioBarRecitationListener

    @Override
    public void onRecitationPressed() {
        recitationPresenter.startOrStopRecitation(this::ayahToStartFrom);
    }

    @Override
    public void onRecitationLongPressed() {
        recitationPresenter.startOrStopRecitation(this::ayahToStartFrom, true);
    }

    @Override
    public void onRecitationTranscriptPressed() {
        if (recitationEventPresenter.hasRecitationSession()) {
            bridge.showSlider(SlidingPagerAdapter.TRANSCRIPT_PAGE);
        } else {
            Timber.e("Transcript pressed but we don't have a session; this should never happen");
        }
    }

    @Override
    public void onHideVersesPressed() {
        recitationSettings.toggleAyahVisibility();
    }

    @Override
    public void onEndRecitationSessionPressed() {
        recitationPresenter.endSession();
    }

    @Override
    public void onPlayRecitationPressed() {
        recitationPlaybackPresenter.play();
    }

    @Override
    public void onPauseRecitationPressed() {
        recitationPlaybackPresenter.pauseIfPlaying();
    }

    private SuraAyah ayahToStartFrom() {
        int page = bridge.isDualPageVisible() ?
                bridge.currentPage() - 1 :
                bridge.currentPage();

        // If we're in ayah mode, start from selected ayah
        SuraAyah selectedAyah = readingEventPresenter.currentAyahSelection().startSuraAyah();
        if (selectedAyah != null) {
            return selectedAyah;
        }

        // If a sura starts on this page, assume the user meant to start there
        for (Integer sura : quranInfo.getListOfSurahWithStartingOnPage(page)) {
            return new SuraAyah(sura, 1);
        }

        // Otherwise, start from the beginning of the page
        return new SuraAyah(quranInfo.getSuraOnPage(page), quranInfo.getFirstAyahOnPage(page));
    }
}
