package com.inspirethis.mike.spotifystreamer;

import android.app.Notification;
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
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.inspirethis.mike.spotifystreamer.Util.Constants;

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
//    private PhoneStateListener mPhoneStateListener;
//    private TelephonyManager mTelephonyManager;
    public static boolean SERVICE_RUNNING;
    public static boolean TRACK_PLAYING;
    public static boolean TRACK_PAUSED;
    // field for notification ID
    private static final int NOTIF_ID = 0;

    // seekBar variables
    int mMediaPosition;
    int mMediaMax;

    private boolean mBroadcastReceiverRegistered;
   // private boolean mHeadsetReceiverRegistered;


    private final Handler mHandler = new Handler();
    public static int songEnded = 0;
    public static final String BROADCAST_ACTION = "com.inspirethis.mike.spotifystreamer.seekprogress";

    // Set up broadcast identifier and seekBarIntent
    public static final String BROADCAST_BUFFER = "com.inspirethis.mike.spotifystreamer.broadcastbuffer";

    public static final String TRACK_RUNNING = "track_running";
    public static final String TRACK_COMPLETED = "track_completed";

    Intent mBufferIntent;
    Intent mSeekIntent;

 //   private int mHeadsetSwitch = 1;

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "Creating MusicService");
        mBroadcastReceiverRegistered = false;
        //mHeadsetReceiverRegistered = false;

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
//        registerReceiver(headsetReceiver, new IntentFilter(
//                Intent.ACTION_HEADSET_PLUG));
//        //mHeadsetReceiverRegistered = true;

        songEnded = 0;
        TRACK_PLAYING = false;  // TODO: these arent needed
        TRACK_PAUSED = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Context context = getApplicationContext();

        //Log.d(LOG_TAG, "** onStartCommand: intent action: " + intent.getAction());
        if (intent.getAction().equals(Constants.ACTION.STARTFOREGROUND_ACTION)) {

            SERVICE_RUNNING = true;
            Intent notificationIntent = new Intent(context, TrackPlayerFragment.class);
            notificationIntent.setAction(Constants.ACTION.MAIN_ACTION);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);

//            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
//                    notificationIntent, 0);

//            Intent previousIntent = new Intent(context, MusicService.class);
//            previousIntent.setAction(Constants.ACTION.PREV_ACTION);
//            PendingIntent ppreviousIntent = PendingIntent.getService(this, 0,
//                    previousIntent, 0);
//
//            Intent playIntent = new Intent(context, MusicService.class);
//            playIntent.setAction(Constants.ACTION.PLAY_ACTION);
//            PendingIntent pplayIntent = PendingIntent.getService(this, 0,
//                    playIntent, 0);
//
//            Intent nextIntent = new Intent(context, MusicService.class);
//            nextIntent.setAction(Constants.ACTION.NEXT_ACTION);
//            PendingIntent pnextIntent = PendingIntent.getService(this, 0,
//                    nextIntent, 0);

            setUp(); // set up listeners, register receivers


//            int icon = R.mipmap.greyscale_thumb;
//            CharSequence tickerText = getResources().getString(R.string.ticket_text);
//
//            CharSequence contentTitle = getResources().getString(R.string.streamer);
//            CharSequence contentText = getResources().getString(R.string.content_text);

            Notification notification = new NotificationCompat.Builder(context).build();
//                    .setAutoCancel(true)
//                    .setContentTitle(contentTitle)
//                    .setTicker(tickerText)
//                    .setContentText(contentText)
//                    .setSmallIcon(icon)
////                    .setLargeIcon(
////                            Bitmap.createScaledBitmap(icon, 128, 128, false))
//                    .setContentIntent(pendingIntent).build();
                    //.setOngoing(true)
//                    .addAction(android.R.drawable.ic_media_previous,
//                            "Prev", ppreviousIntent)
//                    .addAction(android.R.drawable.ic_media_play, "Play",
//                            pplayIntent)
//                    .addAction(android.R.drawable.ic_media_next, "Next",
//                            pnextIntent).build();

//            startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE,
//                    notification);
            startForeground(NOTIF_ID, notification);

            // seekBar handler
            setupHandler();

        } else if (intent.getAction().equals(Constants.ACTION.PLAY_ACTION)) {
            Log.d(LOG_TAG, "action Play");
            // start new track
                mURL = intent.getExtras().getString("sentAudioLink");
            mMediaPosition = 0;
            if (mURL != null)
                setPlayer(mURL);
            else {
                //TODO: toast user: track not found please try again.
            }



        } else if (intent.getAction().equals(Constants.ACTION.PAUSE_ACTION)) {
            Log.d(LOG_TAG, "action pause");
            // pause player
            mMediaPlayer.pause();
            TRACK_PLAYING = false;
            TRACK_PAUSED = true;

        } else if (intent.getAction().equals(Constants.ACTION.RESUME_ACTION)) {
            Log.i(LOG_TAG, "action resume");
            // resume player
            mMediaPlayer.start();
            TRACK_PLAYING = true;
            TRACK_PAUSED = false;

        } else if (intent.getAction().equals(Constants.ACTION.STOPFOREGROUND_ACTION)) {
            Log.i(LOG_TAG, "Received Stop Foreground Intent");
            //TRACK_PLAYING = false;
            stopForeground(true);
            stopSelf();
        }

        return START_STICKY;
    }

    private void setPlayer(String url) {
        mMediaPlayer.reset();

        try {
            mMediaPlayer.setDataSource(url);

            // Send message to Activity to display progress dialogue
            sendBufferingBroadcast();
            // Prepare MediaPlayer
            mMediaPlayer.prepare();
            mMediaPlayer.start();

            TRACK_PLAYING = true;
            TRACK_PAUSED = false;

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
        }
    }


    private void setUp() {
        // receiver for seekBar change
        registerReceiver(broadcastReceiver, new IntentFilter(
                TrackPlayerFragment.BROADCAST_SEEKBAR));
        mBroadcastReceiverRegistered = true;

        // Manage incoming phone calls during playback.
        // Pause MediaPlayer on incoming calls, resume on hangup.

        // instantiate telephony manager
        //Log.d(LOG_TAG, "Starting TELEPHONY_SERVICE");
        //mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);


//        Log.d(LOG_TAG, "Starting PhoneStateListener");
//        mPhoneStateListener = new PhoneStateListener() {
//            @Override
//            public void onCallStateChanged(int state, String incomingNumber) {
//
//                Log.d(LOG_TAG, "Starting CallStateChange");
//                switch (state) {
//                    case TelephonyManager.CALL_STATE_OFFHOOK:
//                    case TelephonyManager.CALL_STATE_RINGING:
//                        if (mMediaPlayer != null) {
//                            pauseMedia();
//                            bIsPausedInCall = true;
//                        }
//
//                        break;
//                    case TelephonyManager.CALL_STATE_IDLE:
//                        // Phone idle state, start MediaPlayer
//                        if (mMediaPlayer != null) {
//                            if (bIsPausedInCall) {
//                                bIsPausedInCall = false;
//                                playMedia();
//                            }
//                        }
//                        break;
//                }
//            }
//        };

//        // Register the listener with the telephony manager
//        mTelephonyManager.listen(mPhoneStateListener,
//                PhoneStateListener.LISTEN_CALL_STATE);
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
        if (mMediaPlayer.isPlaying() && state.equals(TRACK_RUNNING)) {
            mMediaPosition = mMediaPlayer.getCurrentPosition();
            mMediaMax = mMediaPlayer.getDuration();
            mSeekIntent.putExtra("totalDuration", String.valueOf(mMediaMax));
            mSeekIntent.putExtra("currentDuration", String.valueOf(mMediaPosition));
            mSeekIntent.putExtra("song_ended", String.valueOf(songEnded));
            sendBroadcast(mSeekIntent);
        } else if (state.equals(TRACK_COMPLETED)) {
            // report track completed to TrackPlayerFragment
            mSeekIntent.putExtra("totalDuration", String.valueOf(mMediaMax));
            mSeekIntent.putExtra("currentDuration", String.valueOf(mMediaMax));
            mSeekIntent.putExtra("song_ended", String.valueOf(songEnded));
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

//    // If headset gets unplugged, stop music and service.
//    private BroadcastReceiver headsetReceiver = new BroadcastReceiver() {
//        private boolean headsetConnected = false;
//
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            if (intent.hasExtra("state")) {
//                if (headsetConnected && intent.getIntExtra("state", 0) == 0) {
//                    headsetConnected = false;
//                    mHeadsetSwitch = 0;
//                } else if (!headsetConnected
//                        && intent.getIntExtra("state", 0) == 1) {
//                    headsetConnected = true;
//                    mHeadsetSwitch = 1;
//                }
//            }
//            switch (mHeadsetSwitch) {
//                case (0):
//                    headsetDisconnected();
//                    break;
//                case (1):
//                    break;
//            }
//        }
//    };

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

//        if (mPhoneStateListener != null) {
//            mTelephonyManager.listen(mPhoneStateListener,
//                    PhoneStateListener.LISTEN_NONE);
//        }

        // Cancel the notification
        //cancelNotification();

//        if (mHeadsetReceiverRegistered)
//            // Unregister headsetReceiver
//            unregisterReceiver(headsetReceiver);

        if (mBroadcastReceiverRegistered)
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
        // when track ends, notify TrackPlayerFragment to display Play button
        logMediaPosition(TRACK_COMPLETED);
        mMediaPlayer.reset();
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

//    // for Telephony Manager
//    public void pauseMedia() {
//        Log.d(LOG_TAG, "Pause Media");
//        if (mMediaPlayer.isPlaying()) {
//            mMediaPlayer.pause();
//        }
//    }

    public void stopMedia() {
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
        }
    }

//    // Create Notification
//    private void initNotification() {
//
//        //TODO: add media control buttons to allow user to update track from notification
//        String notificationService = Context.NOTIFICATION_SERVICE;
//
//        NotificationManager mNotificationManager = (NotificationManager) getSystemService(notificationService);
//
//        int icon = R.mipmap.greyscale_thumb;
//        CharSequence tickerText = getResources().getString(R.string.ticket_text);
//        Context context = getApplicationContext();
//        CharSequence contentTitle = getResources().getString(R.string.streamer);
//        CharSequence contentText = getResources().getString(R.string.content_text);
//
////        Intent notificationIntent = new Intent(this, TrackPlayerFragment.class);
////        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
////                notificationIntent, 0);
//
//        Intent notificationIntent = new Intent(this, TrackPlayerFragment.class);
//        notificationIntent.setAction(Constants.ACTION.MAIN_ACTION);
//        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
//                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
//                notificationIntent, 0);
//
//        Intent previousIntent = new Intent(this, MusicService.class);
//        previousIntent.setAction(Constants.ACTION.PREV_ACTION);
//        PendingIntent ppreviousIntent = PendingIntent.getService(this, 0,
//                previousIntent, 0);
//
//        Intent playIntent = new Intent(this, MusicService.class);
//        playIntent.setAction(Constants.ACTION.PLAY_ACTION);
//        PendingIntent pplayIntent = PendingIntent.getService(this, 0,
//                playIntent, 0);
//
//        Intent nextIntent = new Intent(this, MusicService.class);
//        nextIntent.setAction(Constants.ACTION.NEXT_ACTION);
//        PendingIntent pnextIntent = PendingIntent.getService(this, 0,
//                nextIntent, 0);
//
//        // as of API level 11
//        Notification notification = new Notification.Builder(context)
//                .setContentTitle(contentTitle)
//                .setContentText(contentText)
//                .setTicker(tickerText)
//                .setSmallIcon(icon)
//                .setContentIntent(pendingIntent)
//                        //.setLargeIcon(aBitmap) // TODO: set large icon
//                .setContentIntent(pendingIntent)
//                .setOngoing(true)
//                .addAction(android.R.drawable.ic_media_previous,
//                        "Previous", ppreviousIntent)
//                .addAction(android.R.drawable.ic_media_play, "Play",
//                        pplayIntent)
//                .addAction(android.R.drawable.ic_media_next, "Next",
//                        pnextIntent)
//                .build();
//        notification.flags = Notification.FLAG_ONGOING_EVENT;
//        mNotificationManager.notify(NOTIFICATION_ID, notification);
//
//
//    }

//    // Cancel Notification
//    private void cancelNotification() {
//        String ns = Context.NOTIFICATION_SERVICE;
//        NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
//        mNotificationManager.cancel(NOTIFICATION_ID);
//    }
}