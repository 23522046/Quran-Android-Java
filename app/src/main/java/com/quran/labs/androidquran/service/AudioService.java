import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.SparseIntArray;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.quran.data.core.QuranInfo;
import com.quran.data.model.SuraAyah;
import com.quran.labs.androidquran.QuranApplication;
import com.quran.labs.androidquran.R;
import com.quran.labs.androidquran.dao.audio.AudioPlaybackInfo;
import com.quran.labs.androidquran.dao.audio.AudioRequest;
import com.quran.labs.androidquran.data.Constants;
import com.quran.labs.androidquran.data.QuranDisplayData;
import com.quran.labs.androidquran.data.QuranFileConstants;
import com.quran.labs.androidquran.database.DatabaseUtils;
import com.quran.labs.androidquran.database.SuraTimingDatabaseHandler;
import com.quran.labs.androidquran.extension.AudioUtils;
import com.quran.labs.androidquran.presenter.audio.service.AudioQueue;
import com.quran.labs.androidquran.service.util.AudioFocusHelper;
import com.quran.labs.androidquran.service.util.AudioFocusable;
import com.quran.labs.androidquran.service.util.QuranDownloadNotifier;
import com.quran.labs.androidquran.ui.PagerActivity;
import com.quran.labs.androidquran.util.AudioUtils;
import com.quran.reading.common.AudioEventPresenter;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import timber.log.Timber;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class AudioService extends Service implements MediaPlayer.OnCompletionListener,
    MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, AudioFocusable,
    MediaPlayer.OnSeekCompleteListener {

    public static class AudioUpdateIntent {
        public static final String INTENT_NAME = "com.quran.labs.androidquran.audio.AudioUpdate";
        public static final String STATUS = "status";
        public static final String SURA = "sura";
        public static final String AYAH = "ayah";
        public static final String REPEAT_COUNT = "repeat_count";
        public static final String REQUEST = "request";
        public static final int STOPPED = 0;
        public static final int PLAYING = 1;
        public static final int PAUSED = 2;
    }

    private static final String ACTION_PLAYBACK = "com.quran.labs.androidquran.action.PLAYBACK";
    private static final String ACTION_PLAY = "com.quran.labs.androidquran.action.PLAY";
    private static final String ACTION_PAUSE = "com.quran.labs.androidquran.action.PAUSE";
    private static final String ACTION_STOP = "com.quran.labs.androidquran.action.STOP";
    private static final String ACTION_SKIP = "com.quran.labs.androidquran.action.SKIP";
    private static final String ACTION_REWIND = "com.quran.labs.androidquran.action.REWIND";
    private static final String ACTION_CONNECT = "com.quran.labs.androidquran.action.CONNECT";
    private static final String ACTION_UPDATE_REPEAT = "com.quran.labs.androidquran.action.UPDATE_REPEAT";

    private static final int REQUEST_CODE_MAIN = 0;
    private static final int REQUEST_CODE_PREVIOUS = 1;
    private static final int REQUEST_CODE_PAUSE = 2;
    private static final int REQUEST_CODE_SKIP = 3;
    private static final int REQUEST_CODE_STOP = 4;
    private static final int REQUEST_CODE_RESUME = 5;

    private static final float DUCK_VOLUME = 0.1f;
    private static final String EXTRA_PLAY_INFO = "com.quran.labs.androidquran.PLAY_INFO";
    private static final String NOTIFICATION_CHANNEL_ID = Constants.AUDIO_CHANNEL;

    private static final int MSG_INCOMING = 1;
    private static final int MSG_START_AUDIO = 2;
    private static final int MSG_UPDATE_AUDIO_POS = 3;

    private MediaPlayer player;
    private boolean playerOverride = false;
    private AudioRequest audioRequest;
    private AudioQueue audioQueue;
    private State state = State.Stopped;
    private enum State {Stopped, Preparing, Playing, Paused}
    private AudioFocus audioFocus = AudioFocus.NoFocusNoDuck;
    private enum AudioFocus {NoFocusNoDuck, NoFocusCanDuck, Focused}
    private boolean isSetupAsForeground = false;
    private final int NOTIFICATION_ID = Constants.NOTIFICATION_ID_AUDIO_PLAYBACK;
    private WifiLock wifiLock;
    private AudioFocusHelper audioFocusHelper;
    private NotificationManager notificationManager;
    private LocalBroadcastManager broadcastManager;
    private BroadcastReceiver noisyAudioStreamReceiver;
    private MediaSessionCompat mediaSession;
    private Looper serviceLooper;
    private ServiceHandler serviceHandler;
    private NotificationCompat.Builder notificationBuilder;
    private NotificationCompat.Builder pausedNotificationBuilder;
    private boolean didSetNotificationIconOnNotificationBuilder = false;
    private int gaplessSura = 0;
    private int notificationColor = 0;
    private volatile Bitmap notificationIcon = null;
    private Bitmap displayIcon = null;
    private SparseIntArray gaplessSuraData = new SparseIntArray();
    private Disposable timingDisposable;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    private QuranInfo quranInfo;
    private QuranDisplayData quranDisplayData;
    private AudioUtils audioUtils;
    private AudioEventPresenter audioEventPresenter;

    private static final int MSG_INCOMING = 1;
    private static final int MSG_START_AUDIO = 2;
    private static final int MSG_UPDATE_AUDIO_POS = 3;

    private static final String ACTION_PLAYBACK = "com.quran.labs.androidquran.action.PLAYBACK";
    private static final String ACTION_CONNECT = "com.quran.labs.androidquran.action.CONNECT";
    private static final String ACTION_PLAY = "com.quran.labs.androidquran.action.PLAY";
    private static final String ACTION_PAUSE = "com.quran.labs.androidquran.action.PAUSE";
    private static final String ACTION_SKIP = "com.quran.labs.androidquran.action.SKIP";
    private static final String ACTION_STOP = "com.quran.labs.androidquran.action.STOP";
    private static final String ACTION_REWIND = "com.quran.labs.androidquran.action.REWIND";
    private static final String ACTION_UPDATE_REPEAT = "com.quran.labs.androidquran.action.UPDATE_REPEAT";
    private static final String EXTRA_PLAY_INFO = "com.quran.labs.androidquran.extra.PLAY_INFO";

    @Override
    public void onCreate() {
        Timber.i("debug: Creating service");
        HandlerThread thread = new HandlerThread("AyahAudioService", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);
        Context appContext = getApplicationContext();
        ((QuranApplication) appContext).getApplicationComponent().inject(this);
        wifiLock = ((WifiManager) appContext.getSystemService(WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "QuranAudioLock");
        notificationManager = (NotificationManager) appContext.getSystemService(NOTIFICATION_SERVICE);
        audioFocusHelper = new AudioFocusHelper(appContext, this);
        broadcastManager = LocalBroadcastManager.getInstance(appContext);
        noisyAudioStreamReceiver = new NoisyAudioStreamReceiver();
        registerReceiver(noisyAudioStreamReceiver,
                new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        ComponentName receiver = new ComponentName(this, MediaButtonReceiver.class);
        mediaSession = new MediaSessionCompat(appContext, "QuranMediaSession", receiver, null);
        mediaSession.setCallback(new MediaSessionCallback(), serviceHandler);
        String channelName = getString(R.string.notification_channel_audio);
        setupNotificationChannel(notificationManager, NOTIFICATION_CHANNEL_ID, channelName);
        notificationColor = ContextCompat.getColor(this, R.color.audio_notification_color);
        try {
            Bitmap placeholder = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            displayIcon = placeholder;
            Canvas canvas = new Canvas(placeholder);
            canvas.drawColor(notificationColor);
        } catch (OutOfMemoryError oom) {
            Timber.e(oom);
        }
        Bitmap icon = displayIcon;
        if (icon != null) {
            compositeDisposable.add(
                    Maybe.fromCallable(this::generateNotificationIcon)
                            .subscribeOn(Schedulers.io())
                            .subscribe(bitmap -> notificationIcon = bitmap)
            );
        }
    }

    private class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_INCOMING && msg.obj != null) {
                Intent intent = (Intent) msg.obj;
                handleIntent(intent);
            } else if (msg.what == MSG_START_AUDIO) {
                configAndStartMediaPlayer();
            } else if (msg.what == MSG_UPDATE_AUDIO_POS) {
                updateAudioPlayPosition();
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            if (state == State.Stopped) {
                serviceHandler.removeCallbacksAndMessages(null);
                stopSelf();
            }
        } else {
            String action = intent.getAction();
            if (ACTION_PLAYBACK.equals(action) || Intent.ACTION_MEDIA_BUTTON.equals(action)) {
                setUpAsForeground();
            }
            Message message = serviceHandler.obtainMessage(MSG_INCOMING, intent);
            serviceHandler.sendMessage(message);
        }
        return START_NOT_STICKY;
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        if (ACTION_CONNECT.equals(action)) {
            if (state == State.Stopped) {
                processStopRequest(true);
            } else {
                int sura = -1;
                int ayah = -1;
                int repeatCount = -200;
                int state = AudioUpdateIntent.PLAYING;
                if (this.state == State.Paused) {
                    state = AudioUpdateIntent.PAUSED;
                }
                AudioQueue localAudioQueue = audioQueue;
                AudioRequest localAudioRequest = audioRequest;
                if (localAudioQueue != null && localAudioRequest != null) {
                    sura = localAudioQueue.getCurrentSura();
                    ayah = localAudioQueue.getCurrentAyah();
                    repeatCount = localAudioRequest.getRepeatInfo();
                }
                Intent updateIntent = new Intent(AudioUpdateIntent.INTENT_NAME);
                updateIntent.putExtra(AudioUpdateIntent.STATUS, state);
                updateIntent.putExtra(AudioUpdateIntent.SURA, sura);
                updateIntent.putExtra(AudioUpdateIntent.AYAH, ayah);
                updateIntent.putExtra(AudioUpdateIntent.REPEAT_COUNT, repeatCount);
                updateIntent.putExtra(AudioUpdateIntent.REQUEST, localAudioRequest);
                broadcastManager.sendBroadcast(updateIntent);
            }
        } else if (ACTION_PLAYBACK.equals(action)) {
            AudioRequest updatedAudioRequest = intent.getParcelableExtra(EXTRA_PLAY_INFO);
            if (updatedAudioRequest != null) {
                audioRequest = updatedAudioRequest;
                SuraAyah start = updatedAudioRequest.getStart();
                audioEventPresenter.onAyahPlayback(start);
                boolean basmallah = !updatedAudioRequest.isGapless() &&
                        start.requiresBasmallah();
                audioQueue = new AudioQueue(
                        quranInfo, updatedAudioRequest,
                        new AudioPlaybackInfo(start, 1, 1, basmallah)
                );
                Timber.d("audio request has changed...");
                player.stop();
                state = State.Stopped;
                Timber.d("stop if playing...");
            }
            processTogglePlaybackRequest();
        } else if (ACTION_PLAY.equals(action)) {
            processPlayRequest();
        } else if (ACTION_PAUSE.equals(action)) {
            processPauseRequest();
        } else if (ACTION_SKIP.equals(action)) {
            processSkipRequest();
        } else if (ACTION_STOP.equals(action)) {
            processStopRequest();
        } else if (ACTION_REWIND.equals(action)) {
            processRewindRequest();
        } else if (ACTION_UPDATE_REPEAT.equals(action)) {
            AudioRequest playInfo = intent.getParcelableExtra(EXTRA_PLAY_INFO);
            AudioQueue localAudioQueue = audioQueue;
            if (playInfo != null && localAudioQueue != null) {
                audioQueue = localAudioQueue.withUpdatedAudioRequest(playInfo);
                audioRequest = playInfo;
            }
        } else {
            MediaButtonReceiver.handleIntent(mediaSession, intent);
        }
    }

    private void updateGaplessData(String databasePath, int sura) {
        if (timingDisposable != null) {
            timingDisposable.dispose();
        }
        timingDisposable = Single.fromCallable(() -> {
            SparseIntArray map = new SparseIntArray();
            SuraTimingDatabaseHandler db = SuraTimingDatabaseHandler.getDatabaseHandler(databasePath);
            Cursor cursor = null;
            try {
                cursor = db.getAyahTimings(sura);
                Timber.d("got cursor of data");
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        int ayah = cursor.getInt(1);
                        int time = cursor.getInt(2);
                        map.put(ayah, time);
                    } while (cursor.moveToNext());
                }
            } catch (SQLException se) {
                Timber.e(se);
            } finally {
                DatabaseUtils.closeCursor(cursor);
            }
            return map;
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((map, throwable) -> {
                    gaplessSura = sura;
                    gaplessSuraData = map != null ? map : new SparseIntArray();
                });
    }

    private int getSeekPosition(boolean isRepeating) {
        if (audioRequest == null) {
            return -1;
        }
        AudioQueue localAudioQueue = audioQueue;
        if (localAudioQueue != null && gaplessSura == localAudioQueue.getCurrentSura()) {
            int ayah = localAudioQueue.getCurrentAyah();
            int time = gaplessSuraData.get(ayah);
            if (ayah == 1 && !isRepeating) {
                return gaplessSuraData.get(0);
            }
            return time;
        }
        return -1;
    }

    private void updateAudioPlayPosition() {
        Timber.d("updateAudioPlayPosition");
        AudioQueue localAudioQueue = audioQueue;
        MediaPlayer localPlayer = player;

        if (localPlayer != null) {
            int sura = localAudioQueue.getCurrentSura();
            int ayah = localAudioQueue.getCurrentAyah();
            int updatedAyah = ayah;
            int maxAyahs = quranInfo.getNumberOfAyahs(sura);
            if (sura != gaplessSura) {
                return;
            }
            setState(PlaybackStateCompat.STATE_PLAYING);
            int pos = localPlayer.getCurrentPosition();
            int ayahTime = gaplessSuraData.get(ayah);
            Timber.d("updateAudioPlayPosition: %d:%d, currently at %d vs expected at %d",
                    sura, ayah, pos, ayahTime);
            int iterAyah = ayah;
            if (ayahTime > pos) {
                while (--iterAyah > 0) {
                    ayahTime = gaplessSuraData.get(iterAyah);
                    if (ayahTime <= pos) {
                        updatedAyah = iterAyah;
                        break;
                    } else {
                        updatedAyah--;
                    }
                }
            } else {
                while (++iterAyah <= maxAyahs) {
                    ayahTime = gaplessSuraData.get(iterAyah);
                    if (ayahTime > pos) {
                        updatedAyah = iterAyah - 1;
                        break;
                    } else {
                        updatedAyah++;
                    }
                }
            }
            Timber.d("updateAudioPlayPosition: %d:%d, decided ayah should be: %d",
                    sura, ayah, updatedAyah);
            if (updatedAyah != ayah) {
                ayahTime = gaplessSuraData.get(ayah);
                if (Math.abs(pos - ayahTime) < 150) {
                    // shouldn't change ayahs if the delta is just 150ms...
                    serviceHandler.sendEmptyMessageDelayed(MSG_UPDATE_AUDIO_POS, 150);
                    return;
                }
                boolean success = localAudioQueue.playAt(sura, updatedAyah, false);
                int nextSura = localAudioQueue.getCurrentSura();
                int nextAyah = localAudioQueue.getCurrentAyah();

                if (!success) {
                    processStopRequest();
                    return;
                } else if (nextSura != sura || nextAyah != updatedAyah) {
                    // remove any messages currently in the queue
                    serviceHandler.removeMessages(MSG_UPDATE_AUDIO_POS);

                    // if the ayah hasn't changed, we're repeating the ayah,
                    // otherwise, we're repeating a range. this variable is
                    // what determines whether or not we replay the basmallah.
                    boolean ayahRepeat = ayah == nextAyah && sura == nextSura;
                    if (ayahRepeat) {
                        // jump back to the ayah we should repeat and play it
                        pos = getSeekPosition(true);
                        localPlayer.seekTo(pos);
                    } else {
                        // we're repeating into a different sura
                        boolean flag = sura != localAudioQueue.getCurrentSura();
                        playAudio(flag);
                    }
                    return;
                }

                // moved on to the next ayah
                updateNotification();
            } else {
                // if we have end of sura info and we bypassed the end of sura
                // line, switch the sura.
                ayahTime = gaplessSuraData.get(999);
                if (ayahTime >= 1 && ayahTime <= pos) {
                    boolean success = localAudioQueue.playAt(sura + 1, 1, false);
                    if (success && localAudioQueue.getCurrentSura() == sura) {
                        // remove any messages currently in the queue
                        serviceHandler.removeMessages(MSG_UPDATE_AUDIO_POS);

                        // jump back to the ayah we should repeat and play it
                        pos = getSeekPosition(false);
                        localPlayer.seekTo(pos);
                    } else if (!success) {
                        processStopRequest();
                    } else {
                        playAudio(true);
                    }
                    return;
                }
            }
            notifyAyahChanged();
            if (maxAyahs >= updatedAyah + 1) {
                int t = gaplessSuraData.get(updatedAyah + 1) - localPlayer.getCurrentPosition();
                Timber.d("updateAudioPlayPosition postingDelayed after: %d", t);
                if (t < 100) {
                    t = 100;
                } else if (t > 10000) {
                    t = 10000;
                }
                serviceHandler.sendEmptyMessageDelayed(MSG_UPDATE_AUDIO_POS, t);
            } else if (maxAyahs == updatedAyah) {
                serviceHandler.sendEmptyMessageDelayed(MSG_UPDATE_AUDIO_POS, 150);
            }
            // if we're on the last ayah, don't do anything - let the file
            // complete on its own to avoid getCurrentPosition() bugs.
        }
    }

    private void processTogglePlaybackRequest() {
        if (state == State.Paused || state == State.Stopped) {
            processPlayRequest();
        } else {
            processPauseRequest();
        }
    }

    private void processPlayRequest() {
        AudioRequest localAudioRequest = audioRequest;
        AudioQueue localAudioQueue = audioQueue;
        if (localAudioRequest == null || localAudioQueue == null) {
            // no audio request, what can we do?
            relaxResources(true, true);
            return;
        }
        tryToGetAudioFocus();

        // actually play the file
        if (state == State.Stopped) {
            if (localAudioRequest.isGapless()) {
                String dbPath = localAudioRequest.getAudioPathInfo().getGaplessDatabase();
                if (dbPath != null) {
                    updateGaplessData(dbPath, localAudioQueue.getCurrentSura());
                }
            }

            // If we're stopped, just go ahead to the next file and start playing
            playAudio(localAudioQueue.getCurrentSura() == 9 && localAudioQueue.getCurrentAyah() == 1);
        } else if (state == State.Paused) {
            // If we're paused, just continue playback and restore the
            // 'foreground service' state.
            state = State.Playing;
            if (!isSetupAsForeground) {
                setUpAsForeground();
            }
            configAndStartMediaPlayer(false);
            notifyAudioStatus(AudioUpdateIntent.PLAYING);
        }
    }

    private void processPauseRequest() {
        if (state == State.Playing) {
            // Pause media player and cancel the 'foreground service' state.
            state = State.Paused;
            serviceHandler.removeMessages(MSG_UPDATE_AUDIO_POS);
            player.pause();
            setState(PlaybackStateCompat.STATE_PAUSED);
            // on jellybean and above, stay in the foreground and
            // update the notification.
            relaxResources(false, false);
            pauseNotification();
            notifyAudioStatus(AudioUpdateIntent.PAUSED);
        } else if (state == State.Stopped) {
            // if we get a pause while we're already stopped, it means we likely woke up because
            // of AudioIntentReceiver, so just stop in this case.
            setState(PlaybackStateCompat.STATE_STOPPED);
            stopSelf();
        }
    }

    private void processRewindRequest() {
        if (state == State.Playing || state == State.Paused) {
            setState(PlaybackStateCompat.STATE_REWINDING);
            MediaPlayer localPlayer = player;
            AudioQueue localAudioQueue = audioQueue;
            AudioRequest localAudioRequest = audioRequest;

            int seekTo = 0;
            int pos = localPlayer.getCurrentPosition();
            if (localAudioRequest.isGapless()) {
                seekTo = getSeekPosition(true);
                pos -= seekTo;
            }
            if (pos > 1500 && !playerOverride) {
                localPlayer.seekTo(seekTo);
                state = State.Playing; // in case we were paused
            } else {
                tryToGetAudioFocus();
                int sura = localAudioQueue.getCurrentSura();
                localAudioQueue.playPreviousAyah(true);
                if (localAudioRequest.isGapless() && sura == localAudioQueue.getCurrentSura()) {
                    int timing = getSeekPosition(true);
                    if (timing > -1) {
                        localPlayer.seekTo(timing);
                    }
                    updateNotification();
                    state = State.Playing; // in case we were paused
                    return;
                }
                playAudio();
            }
        }
    }

    private void processSkipRequest() {
        if (audioRequest == null) {
            return;
        }
        if (state == State.Playing || state == State.Paused) {
            setState(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT);
            if (playerOverride) {
                playAudio(false);
            } else {
                MediaPlayer localPlayer = player;
                AudioQueue localAudioQueue = audioQueue;
                AudioRequest localAudioRequest = audioRequest;

                int sura = localAudioQueue.getCurrentSura();
                tryToGetAudioFocus();
                localAudioQueue.playNextAyah(true);
                if (localAudioRequest.isGapless() && sura == localAudioQueue.getCurrentSura()) {
                    int timing = getSeekPosition(false);
                    if (timing > -1) {
                        localPlayer.seekTo(timing);
                        state = State.Playing; // in case we were paused
                    }
                    updateNotification();
                    return;
                }
                playAudio();
            }
        }
    }

    private void processStopRequest(boolean force) {
        setState(PlaybackStateCompat.STATE_STOPPED);
        serviceHandler.removeMessages(MSG_UPDATE_AUDIO_POS);
        if (state == State.Preparing) {
            shouldStop = true;
            relaxResources(false, true);
        }
        if (force || state != State.Stopped) {
            state = State.Stopped;

            // let go of all resources...
            relaxResources(true, true);
            giveUpAudioFocus();

            // service is no longer necessary. Will be started again if needed.
            serviceHandler.removeCallbacksAndMessages(null);
            stopSelf();

            // stop async task if it's running
            if (timingDisposable != null) {
                timingDisposable.dispose();
            }

            // tell the UI we've stopped
            audioEventPresenter.onAyahPlayback(null);
            notifyAudioStatus(AudioUpdateIntent.STOPPED);
        }
    }

    private void notifyAyahChanged() {
        AudioQueue localAudioQueue = audioQueue;
        AudioRequest localAudioRequest = audioRequest;

        audioEventPresenter.onAyahPlayback(
                new SuraAyah(localAudioQueue.getCurrentSura(), localAudioQueue.getCurrentAyah())
        );
        Intent updateIntent = new Intent(AudioUpdateIntent.INTENT_NAME);
        updateIntent.putExtra(AudioUpdateIntent.STATUS, AudioUpdateIntent.PLAYING);
        updateIntent.putExtra(AudioUpdateIntent.SURA, localAudioQueue.getCurrentSura());
        updateIntent.putExtra(AudioUpdateIntent.AYAH, localAudioQueue.getCurrentAyah());
        broadcastManager.sendBroadcast(updateIntent);
        MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, getTitle())
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, localAudioRequest.getQari().getName());
        MediaPlayer localPlayer = player;
        if (localPlayer != null && localPlayer.isPlaying()) {
            metadataBuilder.putLong(
                    MediaMetadataCompat.METADATA_KEY_DURATION,
                    localPlayer.getDuration()
            );
        }

        if (displayIcon != null) {
            metadataBuilder.putBitmap(
                    MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON,
                    displayIcon
            );
        }
        mediaSession.setMetadata(metadataBuilder.build());
    }

    private void notifyAudioStatus(int status) {
        Intent updateIntent = new Intent(AudioUpdateIntent.INTENT_NAME);
        updateIntent.putExtra(AudioUpdateIntent.STATUS, status);
        broadcastManager.sendBroadcast(updateIntent);
    }

    /**
     * Releases resources used by the service for playback. This includes the
     * "foreground service" status and notification, the wake locks and
     * possibly the MediaPlayer.
     *
     * @param releaseMediaPlayer Indicates whether the Media Player should also
     *                          be released or not
     */
    private void relaxResources(boolean releaseMediaPlayer, boolean stopForeground) {
        if (stopForeground) {
            // stop being a foreground service
            stopForeground(true);
            isSetupAsForeground = false;
        }

        // stop and release the Media Player, if it's available
        MediaPlayer localPlayer = player;
        if (releaseMediaPlayer && localPlayer != null) {
            try {
                localPlayer.reset();
                localPlayer.release();
            } catch (IllegalStateException e) {
                // nothing to do here  ¯\_(ツ)_/¯
            }
            player = null;
            mediaSession.setActive(false);
        }

        // we can also release the Wifi lock, if we're holding it
        if (wifiLock.isHeld()) {
            wifiLock.release();
        }
    }

    private void giveUpAudioFocus() {
        if (audioFocus == AudioFocus.Focused && audioFocusHelper.abandonFocus()) {
            audioFocus = AudioFocus.NoFocusNoDuck;
        }
    }

    private void configAndStartMediaPlayer(boolean canSeek) {
        Timber.d("configAndStartMediaPlayer()");

        MediaPlayer player = this.player;
        if (player == null) return;

        switch (audioFocus) {
            case AudioFocus.NoFocusNoDuck:
                if (player.isPlaying()) {
                    player.pause();
                }
                return;
            case AudioFocus.NoFocusCanDuck:
                player.setVolume(DUCK_VOLUME, DUCK_VOLUME);
                break;
            default:
                player.setVolume(1.0f, 1.0f);
                break;
        }

        if (shouldStop) {
            processStopRequest();
            shouldStop = false;
            return;
        }

        if (playerOverride) {
            if (!player.isPlaying()) {
                player.start();
                state = State.Playing;
            }
            return;
        }

        Timber.d("checking if playing...");
        AudioRequest audioRequest = this.audioRequest;
        if (!player.isPlaying()) {
            if (canSeek && audioRequest.isGapless()) {
                int timing = getSeekPosition(false);
                if (timing != -1) {
                    Timber.d("got timing: %d, seeking and updating later...", timing);
                    player.seekTo(timing);
                } else {
                    Timber.d("no timing data yet, will try again...");
                    serviceHandler.sendEmptyMessageDelayed(MSG_START_AUDIO, 200);
                }
                return;
            } else if (audioRequest.isGapless()) {
                serviceHandler.sendEmptyMessageDelayed(MSG_UPDATE_AUDIO_POS, 200);
            }
            player.start();
            state = State.Playing;
        }
    }

    private void tryToGetAudioFocus() {
        if (audioFocus != AudioFocus.Focused && audioFocusHelper.requestFocus()) {
            audioFocus = AudioFocus.Focused;
        }
    }

    private String getTitle() {
        AudioQueue audioQueue = this.audioQueue;
        if (audioQueue == null) {
            return "";
        } else {
            return quranDisplayData.getSuraAyahString(
                this,
                audioQueue.getCurrentSura(),
                audioQueue.getCurrentAyah()
            );
        }
    }

    private void playAudio(boolean playRepeatSeparator) {
        if (!isSetupAsForeground) {
            setUpAsForeground();
        }
        state = State.Stopped;
        relaxResources(false, false);
        playerOverride = false;
        try {
            AudioQueue localAudioQueue = audioQueue;
            AudioRequest localAudioRequest = audioRequest;

            String url = audioQueue.getUrl();
            if (localAudioRequest == null || localAudioQueue == null || url == null) {
                Intent updateIntent = new Intent(AudioUpdateIntent.INTENT_NAME);
                updateIntent.putExtra(AudioUpdateIntent.STATUS, AudioUpdateIntent.STOPPED);
                audioEventPresenter.onAyahPlayback(null);
                broadcastManager.sendBroadcast(updateIntent);
                processStopRequest(true);
                return;
            }

            boolean isStreaming = url.startsWith("http:") || url.startsWith("https:");
            if (!isStreaming) {
                File file = new File(url);
                if (!file.exists()) {
                    Intent updateIntent = new Intent(AudioUpdateIntent.INTENT_NAME);
                    updateIntent.putExtra(AudioUpdateIntent.STATUS, AudioUpdateIntent.STOPPED);
                    updateIntent.putExtra(EXTRA_PLAY_INFO, audioRequest);
                    audioEventPresenter.onAyahPlayback(null);
                    broadcastManager.sendBroadcast(updateIntent);
                    processStopRequest(true);
                    return;
                }
            }
            int overrideResource = 0;
            if (playRepeatSeparator) {
                int sura = localAudioQueue.getCurrentSura();
                int ayah = localAudioQueue.getCurrentAyah();
                if (sura != 9 && ayah > 1) {
                    overrideResource = R.raw.bismillah;
                } else if (sura == 9 && (ayah > 1 || localAudioRequest.needsIsti3athaAudio())) {
                    overrideResource = R.raw.isti3atha;
                }
            }
            Timber.d("okay, we are preparing to play - streaming is: %b", isStreaming);

            MediaPlayer localPlayer = createMediaPlayerIfNeeded();
            setState(PlaybackStateCompat.STATE_CONNECTING);
            try {
                boolean playUrl = true;
                if (overrideResource != 0) {
                    AssetFileDescriptor afd = getResources().openRawResourceFd(overrideResource);
                    if (afd != null) {
                        localPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                        afd.close();
                        playerOverride = true;
                        playUrl = false;
                    }
                }
                if (playUrl) {
                    overrideResource = 0;
                    localPlayer.setDataSource(url);
                }
            } catch (IllegalStateException ie) {
                Timber.d("IllegalStateException() while setting data source, trying to reset...");
                if (overrideResource != 0) {
                    playAudio(false);
                    return;
                }
                try {
                    localPlayer.reset();
                } catch (IllegalStateException ilse) {
                    processStopRequest(true);
                    return;
                }
                localPlayer.setDataSource(url);
            }
            state = State.Preparing;

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build();
            localPlayer.setAudioAttributes(audioAttributes);

            Timber.d("preparingAsync()...");
            Timber.d("prepareAsync: %d, %s", overrideResource, url);
            localPlayer.prepareAsync();

            if (isStreaming) {
                wifiLock.acquire();
            } else if (wifiLock.isHeld()) {
                wifiLock.release();
            }
        } catch (IOException ex) {
            Timber.e("IOException playing file: %s", ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void setState(int state) {
        long position = 0;
        MediaPlayer localPlayer = player;
        if (localPlayer != null && localPlayer.isPlaying()) {
            position = localPlayer.getCurrentPosition();
        }
        PlaybackStateCompat.Builder builder = new PlaybackStateCompat.Builder();
        builder.setState(state, position, 1.0f);
        builder.setActions(
                PlaybackStateCompat.ACTION_PLAY |
                PlaybackStateCompat.ACTION_STOP |
                PlaybackStateCompat.ACTION_REWIND |
                PlaybackStateCompat.ACTION_FAST_FORWARD |
                PlaybackStateCompat.ACTION_PAUSE |
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT
        );
        mediaSession.setPlaybackState(builder.build());
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {
        MediaPlayer player = this.player;
        if (player == null) return;
        Timber.d(
                "seek complete! %d vs %d",
                mediaPlayer.getCurrentPosition(), player.getCurrentPosition()
        );
        player.start();
        state = State.Playing;
        serviceHandler.sendEmptyMessageDelayed(MSG_UPDATE_AUDIO_POS, 200);
    }

    @Override
    public void onCompletion(MediaPlayer player) {
        if (playerOverride) {
            playAudio(false);
        } else {
            AudioQueue localAudioQueue = audioQueue;
            AudioRequest localAudioRequest = audioRequest;
            if (localAudioQueue == null || localAudioRequest == null) return;
            int beforeSura = localAudioQueue.getCurrentSura();
            if (localAudioQueue.playNextAyah(false)) {
                if (localAudioRequest.isGapless() && beforeSura == localAudioQueue.getCurrentSura()) {
                    player.seekTo(gaplessSuraData[localAudioQueue.getCurrentAyah()]);
                } else {
                    boolean flag = beforeSura != localAudioQueue.getCurrentSura();
                    playAudio(flag);
                }
            } else {
                processStopRequest(true);
            }
        }
    }

    @Override
    public void onPrepared(MediaPlayer player) {
        Timber.d("okay, prepared!");

        if (shouldStop) {
            processStopRequest();
            shouldStop = false;
            return;
        }

        AudioQueue localAudioQueue = audioQueue;
        AudioRequest localAudioRequest = audioRequest;
        if (localAudioQueue == null || localAudioRequest == null) return;

        if (localAudioRequest.isGapless()) {
            if (gaplessSura != localAudioQueue.getCurrentSura()) {
                String dbPath = localAudioRequest.audioPathInfo.gaplessDatabase;
                if (dbPath != null) {
                    updateGaplessData(dbPath, localAudioQueue.getCurrentSura());
                }
            }
        }

        if (playerOverride || !localAudioRequest.isGapless()) {
            notifyAyahChanged();
        }
        updateNotification();
        configAndStartMediaPlayer(true);
    }

    private void updateNotification() {
        notificationBuilder.setContentText(getTitle());
        if (!didSetNotificationIconOnNotificationBuilder && notificationIcon != null) {
            notificationBuilder.setLargeIcon(notificationIcon);
            didSetNotificationIconOnNotificationBuilder = true;
        }
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    private void pauseNotification() {
        NotificationCompat.Builder builder = getPausedNotificationBuilder();
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private Bitmap generateNotificationIcon() {
        Context appContext = getApplicationContext();
        try {
            Resources resources = appContext.getResources();
            Bitmap logo = BitmapFactory.decodeResource(resources, QuranFileConstants.ICON_RESOURCE_ID);
            int iconWidth = logo.getWidth();
            int iconHeight = logo.getHeight();
            ColorDrawable cd = new ColorDrawable(
                    ContextCompat.getColor(
                            appContext,
                            R.color.audio_notification_background_color
                    )
            );
            Bitmap bitmap = Bitmap.createBitmap(iconWidth * 2, iconHeight * 2, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            cd.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            cd.draw(canvas);
            canvas.drawBitmap(
                    logo,
                    (float) (iconWidth / 2.0),
                    (float) (iconHeight / 2.0),
                    null
            );
            return bitmap;
        } catch (OutOfMemoryError oomError) {
            Timber.e(oomError);
            return null;
        }
    }

    private void setUpAsForeground() {
        notificationManager.cancel(QuranDownloadNotifier.DOWNLOADING_COMPLETE_NOTIFICATION);
        Context appContext = getApplicationContext();
        PendingIntent pi = notificationPendingIntent;

        int mutabilityFlag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? PendingIntent.FLAG_IMMUTABLE
                : 0;
        PendingIntent previousIntent = PendingIntent.getService(
                appContext,
                REQUEST_CODE_PREVIOUS,
                audioUtils.getAudioIntent(this, ACTION_REWIND),
                PendingIntent.FLAG_UPDATE_CURRENT | mutabilityFlag
        );
        PendingIntent nextIntent = PendingIntent.getService(
                appContext,
                REQUEST_CODE_SKIP,
                audioUtils.getAudioIntent(this, ACTION_SKIP),
                PendingIntent.FLAG_UPDATE_CURRENT | mutabilityFlag
        );
        PendingIntent pauseIntent = PendingIntent.getService(
                appContext,
                REQUEST_CODE_PAUSE,
                audioUtils.getAudioIntent(this, ACTION_PAUSE),
                PendingIntent.FLAG_UPDATE_CURRENT | mutabilityFlag
        );
        String audioTitle = getTitle();
        NotificationCompat.Builder currentNotificationBuilder = notificationBuilder;
        NotificationCompat.Builder updatedBuilder;
        if (currentNotificationBuilder == null) {
            Bitmap icon = notificationIcon;
            NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext, NOTIFICATION_CHANNEL_ID);
            builder
                    .setSmallIcon(R.drawable.ic_notification)
                    .setColor(notificationColor)
                    .setOngoing(true)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentIntent(pi)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .addAction(R.drawable.ic_previous, getString(R.string.previous), previousIntent)
                    .addAction(R.drawable.ic_pause, getString(R.string.pause), pauseIntent)
                    .addAction(R.drawable.ic_next, getString(R.string.next), nextIntent)
                    .setShowWhen(false)
                    .setWhen(0)
                    .setLargeIcon(icon)
                    .setStyle(new MediaStyle()
                            .setShowActionsInCompactView(0, 1, 2)
                            .setMediaSession(mediaSession.getSessionToken())
                    );
            didSetNotificationIconOnNotificationBuilder = icon != null;
            notificationBuilder = builder;
            updatedBuilder = builder;
        } else {
            updatedBuilder = currentNotificationBuilder;
        }

        updatedBuilder.setTicker(audioTitle);
        updatedBuilder.setContentText(audioTitle);
        startForeground(NOTIFICATION_ID, updatedBuilder.build());
        isSetupAsForeground = true;
    }

    private NotificationCompat.Builder getPausedNotificationBuilder() {
        Context appContext = getApplicationContext();

        int mutabilityFlag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? PendingIntent.FLAG_IMMUTABLE
                : 0;

        PendingIntent resumeIntent = PendingIntent.getService(
                appContext,
                REQUEST_CODE_RESUME,
                audioUtils.getAudioIntent(this, ACTION_PLAYBACK),
                PendingIntent.FLAG_UPDATE_CURRENT | mutabilityFlag
        );
        PendingIntent stopIntent = PendingIntent.getService(
                appContext,
                REQUEST_CODE_STOP,
                audioUtils.getAudioIntent(this, ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT | mutabilityFlag
        );
        PendingIntent pi = notificationPendingIntent;
        NotificationCompat.Builder localPausedNotificationBuilder = pausedNotificationBuilder;
        NotificationCompat.Builder pauseBuilder;
        if (localPausedNotificationBuilder == null) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext, NOTIFICATION_CHANNEL_ID);
            builder
                    .setSmallIcon(R.drawable.ic_notification)
                    .setColor(notificationColor)
                    .setOngoing(true)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentIntent(pi)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .addAction(com.quran.labs.androidquran.common.toolbar.R.drawable.ic_play, getString(R.string.play), resumeIntent)
                    .addAction(R.drawable.ic_stop, getString(R.string.stop), stopIntent)
                    .setShowWhen(false)
                    .setWhen(0)
                    .setLargeIcon(notificationIcon)
                    .setStyle(new MediaStyle()
                            .setShowActionsInCompactView(0, 1)
                            .setMediaSession(mediaSession.getSessionToken())
                    );
            pausedNotificationBuilder = builder;
            pauseBuilder = builder;
        } else {
            pauseBuilder = localPausedNotificationBuilder;
        }

        pauseBuilder.setContentText(getTitle());
        return pauseBuilder;
    }

    private PendingIntent notificationPendingIntent() {
        Context appContext = getApplicationContext();
        int mutabilityFlag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? PendingIntent.FLAG_IMMUTABLE
                : 0;
        return PendingIntent.getActivity(
                appContext,
                REQUEST_CODE_MAIN,
                new Intent(appContext, PagerActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | mutabilityFlag
        );
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Timber.e("Error: what=%s, extra=%s", what, extra);
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Timber.e("MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK %d", what);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Timber.e("MEDIA_ERROR_SERVER_DIED %d", what);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Timber.e("MEDIA_ERROR_UNKNOWN %d", what);
                break;
            default:
                Timber.e("default error %d", what);
                break;
        }

        return false;
    }
}
