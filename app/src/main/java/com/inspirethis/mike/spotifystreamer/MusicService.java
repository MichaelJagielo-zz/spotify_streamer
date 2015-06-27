package com.inspirethis.mike.spotifystreamer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

/*
*
* Music Service abstracts the process of playing streaming audio media
* from TrackPlayerFragment
*
* credit: tutorials found at: http://www.glowingpigs.com/index.php/extras
*/
public class MusicService extends Service implements OnCompletionListener,
        OnPreparedListener, OnErrorListener, OnSeekCompleteListener,
        OnInfoListener, OnBufferingUpdateListener {

    private final String LOG_TAG = MusicService.class.getSimpleName();
    private MediaPlayer mMediaPlayer = new MediaPlayer();
    private String mURL;
    private static final int NOTIFICATION_ID = 1;
    private boolean bIsPausedInCall = false;
    private PhoneStateListener mPhoneStateListener;
    private TelephonyManager mTelephonyManager;

    // seekBar variables
    int mMediaPosition;
    int mMediaMax;
    String mSeekPositionString;
    int mSeekPositionInt;
    boolean bNewTrack;

    private final Handler mHandler = new Handler();
    private static int songEnded = 0;
    public static final String BROADCAST_ACTION = "com.inspirethis.mike.spotifystreamer.seekprogress";

    // Set up broadcast identifier and seekBarIntent
    public static final String BROADCAST_BUFFER = "com.inspirethis.mike.spotifystreamer.broadcastbuffer";

    public static final String TRACK_RUNNING = "track_running";
    public static final String TRACK_COMPLETED = "track_completed";

    Intent mBufferIntent;
    Intent mSeekIntent;

    private int mHeadsetSwitch = 1;

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "Creating MusicService");

        mBufferIntent = new Intent(BROADCAST_BUFFER);

        mSeekIntent = new Intent(BROADCAST_ACTION);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnBufferingUpdateListener(this);
        mMediaPlayer.setOnSeekCompleteListener(this);
        mMediaPlayer.setOnInfoListener(this);
        mMediaPlayer.reset();

        // Register headset receiver
        registerReceiver(headsetReceiver, new IntentFilter(
                Intent.ACTION_HEADSET_PLUG));
        songEnded = 0;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // receiver for seekBar change
        registerReceiver(broadcastReceiver, new IntentFilter(
                TrackPlayerFragment.BROADCAST_SEEKBAR));

        // Manage incoming phone calls during playback.
        // Pause MediaPlayer on incoming calls, resume on hangup.

        // instantiate telephony manager
        Log.d(LOG_TAG, "Starting TELEPHONY_SERVICE");
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);


        Log.d(LOG_TAG, "Starting PhoneStateListener");
        mPhoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {

                Log.d(LOG_TAG, "Starting CallStateChange");
                switch (state) {
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (mMediaPlayer != null) {
                            pauseMedia();
                            bIsPausedInCall = true;
                        }

                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        // Phone idle state, start MediaPlayer
                        if (mMediaPlayer != null) {
                            if (bIsPausedInCall) {
                                bIsPausedInCall = false;
                                playMedia();
                            }
                        }
                        break;
                }
            }
        };

        // Register the listener with the telephony manager
        mTelephonyManager.listen(mPhoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE);

        initNotification();

        mURL = intent.getExtras().getString("sentAudioLink");
        int position = intent.getIntExtra("pausePosition", 0);
        boolean newTrack = intent.getBooleanExtra("newTrack", true);
        if (position != 0) {
            mMediaPosition = position;
        } else if (newTrack)
            mMediaPosition = 0;

        Log.d("MusicService: ", mURL);
        mMediaPlayer.reset();

        // Set up the MediaPlayer data source using intent extra
        if (!mMediaPlayer.isPlaying()) {
            try {
                mMediaPlayer
                        .setDataSource(mURL);

                // Send message to Activity to display progress dialogue
                sendBufferingBroadcast();
                // Prepare MediaPlayer
                mMediaPlayer.prepareAsync();

            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (IOException e) {
            }
        }
        // seekBar handler
        setupHandler();

        return START_STICKY;
    }

    // seekBar info to activity
    private void setupHandler() {
        mHandler.removeCallbacks(sendUpdatesToUI);
        mHandler.postDelayed(sendUpdatesToUI, 100); // 1/10 second
    }

    private Runnable sendUpdatesToUI = new Runnable() {
        public void run() {
            logMediaPosition(TRACK_RUNNING);
            mHandler.postDelayed(this, 100);
        }
    };

    private void logMediaPosition(String state) {

        Log.d(LOG_TAG, "method: logMediaPosition");
        if (mMediaPlayer.isPlaying() && state.equals(TRACK_RUNNING)) {
            mMediaPosition = mMediaPlayer.getCurrentPosition();

            mMediaMax = mMediaPlayer.getDuration();

            mSeekIntent.putExtra("totalDuration", String.valueOf(mMediaMax));
            mSeekIntent.putExtra("currentDuration", String.valueOf(mMediaPosition));
            mSeekIntent.putExtra("song_ended", String.valueOf(songEnded));
            Log.d("", "sending broadcast from logMediaPosition: song_ended: " + String.valueOf(songEnded));
            sendBroadcast(mSeekIntent);
        } else if (state.equals(TRACK_COMPLETED)) {
            // report track completed to TrackPlayerFragment
            songEnded = 1;
            mSeekIntent.putExtra("totalDuration", String.valueOf(mMediaMax));
            mSeekIntent.putExtra("currentDuration", String.valueOf(mMediaMax));
            mSeekIntent.putExtra("song_ended", String.valueOf(songEnded));
            Log.d("", "sending broadcast from logMediaPosition: song_ended: " + String.valueOf(songEnded));
            sendBroadcast(mSeekIntent);
        }
    }

    // Receive seekBar position if changed by user in TrackPlayerFragment UI
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateSeekPos(intent);
        }
    };

    // Update seek position from TrackPlayerFragment
    public void updateSeekPos(Intent intent) {
        int seekPosition = intent.getIntExtra("seekpos", 0);
        if (mMediaPlayer.isPlaying()) {
            mHandler.removeCallbacks(sendUpdatesToUI);
            mMediaPlayer.seekTo(seekPosition);
            setupHandler();
        }
    }

    // If headset gets unplugged, stop music and service.
    private BroadcastReceiver headsetReceiver = new BroadcastReceiver() {
        private boolean headsetConnected = false;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("state")) {
                if (headsetConnected && intent.getIntExtra("state", 0) == 0) {
                    headsetConnected = false;
                    mHeadsetSwitch = 0;
                } else if (!headsetConnected
                        && intent.getIntExtra("state", 0) == 1) {
                    headsetConnected = true;
                    mHeadsetSwitch = 1;
                }
            }
            switch (mHeadsetSwitch) {
                case (0):
                    headsetDisconnected();
                    break;
                case (1):
                    break;
            }
        }
    };

    private void headsetDisconnected() {
        stopMedia();
        stopSelf();
    }


    // stop media player and release.
    // stop mPhoneStateListener, notification, receivers
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
            mMediaPlayer.release();
        }

        if (mPhoneStateListener != null) {
            mTelephonyManager.listen(mPhoneStateListener,
                    PhoneStateListener.LISTEN_NONE);
        }

        // Cancel the notification
        cancelNotification();

        // Unregister headsetReceiver
        unregisterReceiver(headsetReceiver);

        // Unregister seekbar receiver
        unregisterReceiver(broadcastReceiver);

        // Stop the seekbar mHandler from sending updates to UI
        mHandler.removeCallbacks(sendUpdatesToUI);

        // Service ends, need to tell activity to display "Play" button
        resetButtonPlayStopBroadcast();
    }

    // Send a message to TrackPlayerFragment that audio is being prepared and buffering
    // started.
    private void sendBufferingBroadcast() {
        Log.d(LOG_TAG, "Buffer Started sent");
        mBufferIntent.putExtra("buffering", "1");
        sendBroadcast(mBufferIntent);
    }

    // Send a message to TrackPlayerFragment that audio is prepared and ready to start
    // playing.
    private void sendBufferCompleteBroadcast() {
        Log.d(LOG_TAG, "Buffer Complete sent");
        mBufferIntent.putExtra("buffering", "0");
        sendBroadcast(mBufferIntent);
    }

    // Send a message to TrackPlayerFragment to reset the play button.
    private void resetButtonPlayStopBroadcast() {
        Log.d(LOG_TAG, "Buffer reset play/stop button sent");
        mBufferIntent.putExtra("buffering", "2");
        sendBroadcast(mBufferIntent);
    }

    @Override
    public void onBufferingUpdate(MediaPlayer arg0, int arg1) {
    }

    @Override
    public boolean onInfo(MediaPlayer arg0, int arg1, int arg2) {
        return false;
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        if (!mMediaPlayer.isPlaying()) {
            playMedia();
            Log.d(LOG_TAG, "Seek Complete");
        }
    }


    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Toast.makeText(this,
                        "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK " + extra,
                        Toast.LENGTH_SHORT).show();
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Toast.makeText(this, "MEDIA ERROR SERVER DIED " + extra,
                        Toast.LENGTH_SHORT).show();
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Toast.makeText(this, "MEDIA ERROR UNKNOWN " + extra,
                        Toast.LENGTH_SHORT).show();
                break;
        }
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer arg0) {
        // Send a message to activity to end progress dialogue
        sendBufferCompleteBroadcast();
        playMedia();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d("", "calling onCompletion: ");
        songEnded = 1;
        logMediaPosition(TRACK_COMPLETED);
        // when track ends, notify TrackPlayerFragment to display Play button
        stopMedia();
        // service method, calling stop
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    public void playMedia() {
        songEnded = 0;
        if (!mMediaPlayer.isPlaying()) {
            mMediaPlayer.seekTo(mMediaPosition);
            mMediaPlayer.start();
        }
    }

    // for Telephony Manager
    public void pauseMedia() {
        Log.d(LOG_TAG, "Pause Media");
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
        }
    }

    public void stopMedia() {
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
        }
    }

    // Create Notification
    private void initNotification() {

        //TODO: add media control buttons to allow user to update track from notification
        String notificationService = Context.NOTIFICATION_SERVICE;

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(notificationService);

        int icon = R.mipmap.greyscale_thumb;
        CharSequence tickerText = "Spotify Streamer, enjoy some preview tracks!";
        Context context = getApplicationContext();
        CharSequence contentTitle = "Spotify Streamer"; //TODO: move these strings to folder
        CharSequence contentText = "sample notification add pause / play functionality";

        Intent notificationIntent = new Intent(this, TrackPlayerFragment.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, 0);

        // as of API level 11
        Notification notification = new Notification.Builder(context)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setTicker(tickerText)
                .setSmallIcon(icon)
                .setContentIntent(contentIntent)
                        //.setLargeIcon(aBitmap) // TODO: set large icon
                .build();
        notification.flags = Notification.FLAG_ONGOING_EVENT;
        mNotificationManager.notify(NOTIFICATION_ID, notification);
    }

    // Cancel Notification
    private void cancelNotification() {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
        mNotificationManager.cancel(NOTIFICATION_ID);
    }
}
