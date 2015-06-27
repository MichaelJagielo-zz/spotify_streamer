package com.inspirethis.mike.spotifystreamer;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Track;
import retrofit.RetrofitError;

/**
 * TrackPlayerFragment class, displays given track which is currently playing
 * injecting views with ButterKnife
 * Created by mike on 6/16/15.
 */
public class TrackPlayerFragment extends Fragment implements OnSeekBarChangeListener {

    private TrackItem mTrackItem;
    private TrackItem mCurrentTrackItem;
    private Track mTrack;

    private int mCurrentIndex;
    private ArrayList<TrackItem> mTrackItems;

    //private MediaPlayer mPlayer;

    @InjectView(R.id.tvArtistName)
    TextView artist_name;

    @InjectView(R.id.tvAlbumName)
    TextView album_name;

    @InjectView(R.id.trackImage)
    ImageView track_image;

    @InjectView(R.id.tvTrackName)
    TextView track_name;

    @InjectView(R.id.buttonPlay)
    ImageButton btnPlayPause;

    @InjectView(R.id.buttonNext)
    ImageButton btnNext;

    @InjectView(R.id.buttonPrevious)
    ImageButton btnPrevious;

    @InjectView(R.id.seekBar)
    SeekBar mSeekBar;

    @InjectView(R.id.tvCurrentTime)
    TextView tvCurrentTime;

    @InjectView(R.id.tvFinalTime)
    TextView tvFinalTime;


    Intent mServiceIntent;


    private boolean mBoolMusicPlaying;
    TelephonyManager telephonyManager;
    PhoneStateListener listener;

    private static int mTrackEndedFlag = 0;
    boolean mBroadcastIsRegistered;

    // constant for broadcast of seekBar position
    public static final String BROADCAST_SEEKBAR = "com.inspirethis.mike.spotifystreamer.sendseekbar";
    Intent seekBarIntent;

    // Progress dialogue and broadcast receiver variables
    boolean mBufferBroadcastIsRegistered;
    private ProgressDialog pdBuff = null;


    public TrackPlayerFragment() {
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBoolMusicPlaying = false;

        if (savedInstanceState == null || !savedInstanceState.containsKey("list")) {
            Bundle bundle = getArguments();
            if (bundle != null) {
                mCurrentIndex = bundle.getInt("index");
                mTrackItems = bundle.getParcelableArrayList("tracks_list");
                mCurrentTrackItem = mTrackItems.get(mCurrentIndex);
            }
        } else {
            mTrackItems = savedInstanceState.getParcelableArrayList("list");
            mCurrentIndex = savedInstanceState.getInt("current_index");
            mCurrentTrackItem = mTrackItems.get(mCurrentIndex);
        }

        if (mCurrentTrackItem != null) {
            FetchTrackTask task = new FetchTrackTask();
            task.execute(mCurrentTrackItem.getSpotifyId());

        } else
            Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.track_not_found), Toast.LENGTH_SHORT).show();

    }

    // Broadcast Receiver to update position of seekBar from service
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent serviceIntent) {
            updateUI(serviceIntent);
        }
    };

    private void updateUI(Intent serviceIntent) {

        String strSongEnded = serviceIntent.getStringExtra("song_ended");
        String totalDuration = serviceIntent.getStringExtra("totalDuration");
        String currentDuration = serviceIntent.getStringExtra("currentDuration");

        // Displaying Total Duration time
        tvFinalTime.setText(milliSecondsToTimer(Integer.parseInt(totalDuration)));
        // Displaying time completed playing
        tvCurrentTime.setText(milliSecondsToTimer(Integer.parseInt(currentDuration)));


        int seekProgress = Integer.parseInt(currentDuration);
        int seekMax = Integer.parseInt(totalDuration);
        mTrackEndedFlag = Integer.parseInt(strSongEnded);

        mSeekBar.setMax(seekMax);
        mSeekBar.setProgress(seekProgress);

        if (mTrackEndedFlag == 1) {
            btnPlayPause.setBackgroundResource(android.R.drawable.ic_media_pause);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }


    @OnClick(R.id.buttonPlay)
    public void playPause() {

        if (!mBoolMusicPlaying) {
            btnPlayPause.setBackgroundResource(android.R.drawable.ic_media_pause);
            // pausing an existing track play
            if (mSeekBar.getProgress() > 0)
                playTrack(mTrack.preview_url, mSeekBar.getProgress(), false);
            else
                playTrack(mTrack.preview_url, 0, true);
            mBoolMusicPlaying = true;
        } else {
            mBoolMusicPlaying = false;
            btnPlayPause.setBackgroundResource(android.R.drawable.ic_media_play);
            stopMusicService();
        }
    }

    @OnClick(R.id.buttonNext)
    public void getNext() {
        mSeekBar.setProgress(0);
        stopMusicService();

        if (mCurrentIndex + 1 > mTrackItems.size() - 1) {
            mCurrentIndex = 0;
        } else {
            mCurrentIndex = mCurrentIndex + 1;
        }

        mCurrentTrackItem = mTrackItems.get(mCurrentIndex);

        if (mCurrentTrackItem != null) {
            FetchTrackTask task = new FetchTrackTask();
            task.execute(mCurrentTrackItem.getSpotifyId());
            resetTrackData();
            btnPlayPause.setBackgroundResource(android.R.drawable.ic_media_play);

        } else
            Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.track_not_found), Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.buttonPrevious)
    public void getPrevious() {
        mSeekBar.setProgress(0);
        stopMusicService();

        if (mCurrentIndex - 1 < 0) {
            mCurrentIndex = mTrackItems.size() - 1;
        } else {
            mCurrentIndex = mCurrentIndex - 1;
        }

        mCurrentTrackItem = mTrackItems.get(mCurrentIndex);

        if (mCurrentTrackItem != null) {
            FetchTrackTask task = new FetchTrackTask();
            task.execute(mCurrentTrackItem.getSpotifyId());
            resetTrackData();
            btnPlayPause.setBackgroundResource(android.R.drawable.ic_media_play);

        } else
            Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.track_not_found), Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList("list", mTrackItems);
        outState.putInt("current_index", mCurrentIndex);

        super.onSaveInstanceState(outState);
    }

    private void resetTrackData() {
        mSeekBar.setProgress(0);
        tvCurrentTime.setText("0.00"); //TODO: add this string to file
        tvFinalTime.setText("00.30"); //TODO: get duration from intent rather than hard code

        artist_name.setText(mCurrentTrackItem.getName());
        album_name.setText(mCurrentTrackItem.getAlbum());
        track_name.setText(mCurrentTrackItem.getTrack());

        if (mCurrentTrackItem.getImage_path_large() != null && !mCurrentTrackItem.getImage_path_large().equals(""))
            Picasso.with(getActivity().getApplicationContext()).load(mCurrentTrackItem.getImage_path_large()).into(track_image);
        else
            track_image.setImageResource(R.mipmap.greyscale_thumb);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_track_player, null);
        ButterKnife.inject(this, rootView);

        artist_name.setText(mCurrentTrackItem.getName());
        album_name.setText(mCurrentTrackItem.getAlbum());
        track_name.setText(mCurrentTrackItem.getTrack());

        tvFinalTime.setText("00.30"); //TODO: same as above

        btnNext.setBackgroundResource(android.R.drawable.ic_media_next);
        btnPrevious.setBackgroundResource(android.R.drawable.ic_media_previous);

        if (mCurrentTrackItem.getImage_path_large() != null && !mCurrentTrackItem.getImage_path_large().equals(""))
            Picasso.with(getActivity().getApplicationContext()).load(mCurrentTrackItem.getImage_path_large()).into(track_image);
        else
            track_image.setImageResource(R.mipmap.greyscale_thumb);

        try {
            mServiceIntent = new Intent(getActivity().getApplicationContext(), MusicService.class);

            // seekBarIntent for broadcasting new position to service
            seekBarIntent = new Intent(BROADCAST_SEEKBAR);
            mSeekBar.setOnSeekBarChangeListener(this);

        } catch (Exception e) {
            e.printStackTrace();
            // TODO: replace this with log
            Toast.makeText(getActivity().getApplicationContext(),
                    e.getClass().getName() + " " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }

        return rootView;
    }


    public class FetchTrackTask extends AsyncTask<String, Void, Track> {
        private final String LOG_TAG = FetchTrackTask.class.getSimpleName();

        @Override
        protected Track doInBackground(String... params) {

            if (params.length == 0 || params[0].equals("")) {
                return null;
            }
            Track track = null;
            try {
                SpotifyApi api = new SpotifyApi();
                SpotifyService spotify = api.getService();
                track = spotify.getTrack(params[0]);

            } catch (RetrofitError error) {
                SpotifyError spotifyError = SpotifyError.fromRetrofitError(error);
                Log.d(LOG_TAG, spotifyError.getMessage());
                return null;
            }

            return track;
        }

        @Override
        protected void onPostExecute(Track result) {
            if (result != null) {
                mTrack = result;

                // instantiate MusicService upon fetch of Track
                btnPlayPause.setBackgroundResource(android.R.drawable.ic_media_pause);
                mBoolMusicPlaying = true;
                playTrack(mTrack.preview_url, 0, true);
            } else {
                Toast.makeText(getActivity(), getResources().getString(R.string.track_not_found), Toast.LENGTH_SHORT).show();
                //TODO: create track not available layout?

            }
        }
    }

    // credit source: http://stackoverflow.com/questions/21447798/how-to-display-current-time-of-song-in-textview
    private String milliSecondsToTimer(long milliseconds) {
        String finalTimerString = "";
        String secondsString = "";

        // Convert total duration into time
        int hours = (int) (milliseconds / (1000 * 60 * 60));
        int minutes = (int) (milliseconds % (1000 * 60 * 60)) / (1000 * 60);
        int seconds = (int) ((milliseconds % (1000 * 60 * 60)) % (1000 * 60) / 1000);
        // Add hours if there
        if (hours > 0) {
            finalTimerString = hours + ":";
        }

        // Prepending 0 to seconds if it is one digit
        if (seconds < 10) {
            secondsString = "0" + seconds;
        } else {
            secondsString = "" + seconds;
        }

        finalTimerString = finalTimerString + minutes + ":" + secondsString;

        // return timer string
        return finalTimerString;
    }

    private int getProgressPercentage(long currentDuration, long totalDuration) {
        Double percentage = (double) 0;

        long currentSeconds = (int) (currentDuration / 1000);
        long totalSeconds = (int) (totalDuration / 1000);

        // calculating percentage
        percentage = (((double) currentSeconds) / totalSeconds) * 100;

        // return percentage
        return percentage.intValue();
    }

    private void stopMusicService() {
        // unregister broadcastReceiver for seekBar
        mBoolMusicPlaying = false;
        if (mBroadcastIsRegistered) {
            try {
                getActivity().unregisterReceiver(broadcastReceiver);
                mBroadcastIsRegistered = false;
            } catch (Exception e) {
                 Log.d("", "Error in Activity", e); // TODO update log statement
                e.printStackTrace();
                //Toast.makeText(getActivity().getApplicationContext(), "Error stopping MusicService", Toast.LENGTH_LONG).show();
            }
        }

        try {
            getActivity().stopService(mServiceIntent);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // start MusicService
    private void playTrack(String url) {
        if (isConnected()) {
            mServiceIntent.putExtra("sentAudioLink", url);

            try {
                getActivity().startService(mServiceIntent);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // register receiver for seekBar
            getActivity().registerReceiver(broadcastReceiver, new IntentFilter(
                    MusicService.BROADCAST_ACTION));

            mBroadcastIsRegistered = true;

        } else {
            Toast.makeText(getActivity().getApplicationContext(),
                    getResources().getString(R.string.network_connection_needed),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void playTrack(String url, int position, boolean newTrack) {

        if (isConnected()) {

            mServiceIntent.putExtra("sentAudioLink", url);
            mServiceIntent.putExtra("pausePosition", position);
            mServiceIntent.putExtra("newTrack", newTrack);

            try {
                getActivity().startService(mServiceIntent);
            } catch (Exception e) {
                e.printStackTrace();
            }

            getActivity().registerReceiver(broadcastReceiver, new IntentFilter(
                    MusicService.BROADCAST_ACTION));
            mBroadcastIsRegistered = true;
        } else {
            Toast.makeText(getActivity().getApplicationContext(),
                    getResources().getString(R.string.network_connection_needed),
                    Toast.LENGTH_LONG).show();
        }
    }

    // progress dialogue for buffering...
    private void showPD(Intent bufferIntent) {
        String bufferValue = bufferIntent.getStringExtra("buffering");
        int bufferIntValue = Integer.parseInt(bufferValue);

        // When the broadcasted "buffering" value is 1, show "Buffering"
        // progress dialogue.
        // When the broadcasted "buffering" value is 0, dismiss the progress
        // dialogue.

        switch (bufferIntValue) {
            case 0:
                // Log.v(TAG, "BufferIntValue=0 RemoveBufferDialogue");
                // txtBuffer.setText("");
                if (pdBuff != null) {
                    pdBuff.dismiss();
                }
                break;

            case 1:
                //BufferDialogue(); //TODO call to buffer dialog here
                break;

            // Listen for "2" to reset the button to a play button
            case 2:
                btnPlayPause.setBackgroundResource(android.R.drawable.ic_media_play);
                break;

        }
    }

    // Progress dialogue...
//    private void BufferDialogue() {
//
//        pdBuff = ProgressDialog.show(getActivity().getApplicationContext(), "Buffering...",
//                "Acquiring song...", true);
//    }

    // Set up broadcast receiver
    private BroadcastReceiver broadcastBufferReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent bufferIntent) {
            showPD(bufferIntent);
        }
    };

    // unregister broadcast receiver
    @Override
    public void onPause() {
        if (mBufferBroadcastIsRegistered) {
            getActivity().unregisterReceiver(broadcastBufferReceiver);
            mBufferBroadcastIsRegistered = false;
        }
        super.onPause();
    }


    // register broadcast receiver
    @Override
    public void onResume() {
        if (!mBufferBroadcastIsRegistered) {
            getActivity().registerReceiver(broadcastBufferReceiver, new IntentFilter(
                    MusicService.BROADCAST_BUFFER));
            mBufferBroadcastIsRegistered = true;
        }
        super.onResume();
    }

    // methods of OnSeekBarChangeListener,
    // when user manually moves seekBar, broadcast new position to service
    @Override
    public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
        // TODO Auto-generated method stub
        if (fromUser) {
            int seekPos = sb.getProgress();
            seekBarIntent.putExtra("seekpos", seekPos);
            getActivity().sendBroadcast(seekBarIntent);
        }
    }

    // alternatives to track seekBar if moved.
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub
    }

    private boolean isConnected() {
        return isWifiConnected() || isCellularConnected();
    }

    private boolean isWifiConnected() {
        ConnectivityManager connMgr = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected() && networkInfo.getType() == ConnectivityManager.TYPE_WIFI);
    }

    private boolean isCellularConnected() {
        ConnectivityManager connMgr = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected() && networkInfo.getType() == ConnectivityManager.TYPE_MOBILE);
    }
}
