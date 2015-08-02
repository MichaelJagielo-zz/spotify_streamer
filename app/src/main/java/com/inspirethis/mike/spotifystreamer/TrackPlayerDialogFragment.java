package com.inspirethis.mike.spotifystreamer;

import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.inspirethis.mike.spotifystreamer.Util.Constants;
import com.inspirethis.mike.spotifystreamer.Util.Utility;
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
 *
 */
public class TrackPlayerDialogFragment extends DialogFragment implements SeekBar.OnSeekBarChangeListener {
    private TrackItem mTrackItem;
    private TrackItem mCurrentTrackItem;
    private Track mTrack;
    private int mCurrentIndex;
    private ArrayList<TrackItem> mTrackItems;

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

    // for keeping track of when user navigates back from MainActivity through "Now Playing" button
    private boolean mNavBack;

    private int mSavedPosition;
    private int mMaxPosition;
    private int mCurrentPosition;
    private String mCurrentTime;
    private String mFinalTime;

    //private View mView;

    private static int mTrackEndedFlag = 0;
    boolean mBroadcastIsRegistered;

    // constant for broadcast of seekBar position
    public static final String BROADCAST_SEEKBAR = "com.inspirethis.mike.spotifystreamer.sendseekbar";
    Intent seekBarIntent;
    Intent mServiceIntent;

    // Progress dialogue and broadcast receiver variables
    boolean mBufferBroadcastIsRegistered;
    private ProgressDialog progressDialog = null;

    //private final String LOG_TAG = TrackPlayerDialogFragment.class.getSimpleName();

    public TrackPlayerDialogFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // is the first run
        if (savedInstanceState == null) {
            Bundle bundle = getArguments();
            if (bundle != null) {
                mCurrentIndex = bundle.getInt("current_index");
                mCurrentPosition = bundle.getInt("current_position");
                mMaxPosition = bundle.getInt("max_position");
                mCurrentTime = bundle.getString("current_time");
                mFinalTime = bundle.getString("final_time");

                mTrackItems = bundle.getParcelableArrayList("track_items");
                mCurrentTrackItem = mTrackItems.get(mCurrentIndex);
                mNavBack = bundle.getBoolean("nav_back", false); // if true dont play track
                // get started playing that first track
                if (mCurrentTrackItem != null && !mNavBack) {
                    FetchTrackTask task = new FetchTrackTask();
                    task.execute(mCurrentTrackItem.getSpotifyId());
                } else if (mNavBack) {
                    if (mCurrentTrackItem == null)
                        Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.track_not_found), Toast.LENGTH_SHORT).show();
                    else if (MusicService.TRACK_PLAYING) {
                        playTrack(null, Constants.ACTION.RESUME_ACTION);
                    }
                    if (MusicService.songEnded == 1) {
                        FetchTrackTask task = new FetchTrackTask();
                        task.execute(mCurrentTrackItem.getSpotifyId());
                    }
                } else {
                    Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.track_not_found), Toast.LENGTH_SHORT).show();
                }
            }
            // subsequent run, get last settings
        } else {
            mTrackItems = savedInstanceState.getParcelableArrayList("track_items");
            mCurrentIndex = savedInstanceState.getInt("current_index");
            mCurrentTrackItem = mTrackItems.get(mCurrentIndex);
            mSavedPosition = savedInstanceState.getInt("saved_position");
            mMaxPosition = savedInstanceState.getInt("max_position");
            mCurrentTime = savedInstanceState.getString("current_time");
            mFinalTime = savedInstanceState.getString("final_time");
            mNavBack = savedInstanceState.getBoolean("nav_back");

            // if Now Playing button was clicked
            SharedPreferences prefs = getActivity().getApplicationContext().getSharedPreferences("tracks_info", Context.MODE_PRIVATE);
            if (prefs != null) {
                mNavBack = true;
                // get saved track data
                mCurrentPosition = prefs.getInt("current_position", 0);
                mCurrentTime = prefs.getString("current_time", "");
                mFinalTime = prefs.getString("final_time", "");
            }
            if (mCurrentTrackItem == null)
                Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.track_not_found), Toast.LENGTH_SHORT).show();
        }
        registerBufferReceiver();
        registerReceiver();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        menu.add(getResources().getString(R.string.quit));
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                if (MusicService.SERVICE_RUNNING) {
                    Intent quitIntent = new Intent(getActivity(), MusicService.class);
                    quitIntent.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
                    getActivity().startService(quitIntent);
                }
                clearPreferences();
                getActivity().finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void clearPreferences() {
        SharedPreferences prefs = getActivity().getApplicationContext().getSharedPreferences("tracks_info", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.commit();
    }

    // Broadcast Receiver to update position of seekBar from service
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
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

        if (mTrackEndedFlag == 1 && !mNavBack) {
            btnPlayPause.setBackgroundResource(android.R.drawable.ic_media_play);
            mSeekBar.setProgress(0);
            tvCurrentTime.setText(milliSecondsToTimer(0));
        } else
            btnPlayPause.setBackgroundResource(android.R.drawable.ic_media_pause);
    }

    @OnClick(R.id.buttonPlay)
    public void playPause() {
        if (!MusicService.TRACK_PLAYING) {
            // track is not playing, get ready to play
            btnPlayPause.setBackgroundResource(android.R.drawable.ic_media_pause);
            // resuming an existing track play
            if (mSeekBar.getProgress() > 0 && mTrackEndedFlag != 1) {
                playTrack(null, Constants.ACTION.RESUME_ACTION);
                saveTracksInfo();
            }
            // start new track
            else
                playTrack(mTrack, Constants.ACTION.PLAY_ACTION);

            // replaying an existing track that has ended
        } else if (mTrackEndedFlag == 1) {
            if (mTrack != null)
                playTrack(mTrack, Constants.ACTION.PLAY_ACTION);
            else {
                SharedPreferences prefs = getActivity().getApplicationContext().getSharedPreferences("tracks_info", Context.MODE_PRIVATE);
                if (prefs != null) {
                    GsonBuilder gsonb = new GsonBuilder();
                    Gson gson = gsonb.create();
                    int size = prefs.getInt("list_size", 0);
                    if (size != 0) {
                        ArrayList<TrackItem> list = new ArrayList(prefs.getInt("list_size", 0));
                        for (int i = 0; i < size; i++) {
                            String objectString = prefs.getString(String.valueOf(i), "");
                            if (!objectString.equals(""))
                                list.add(gson.fromJson(objectString, TrackItem.class));
                        }
                        mTrackItems = list;
                    }
                }
                mCurrentIndex = prefs.getInt("current_index", 0);
                mCurrentTrackItem = mTrackItems.get(mCurrentIndex);

                FetchTrackTask task = new FetchTrackTask();
                task.execute(mCurrentTrackItem.getSpotifyId());
            }
        } else if (MusicService.TRACK_PLAYING) {
            // track is playing, pausing track
            btnPlayPause.setBackgroundResource(android.R.drawable.ic_media_play);
            // url arg not needed as it resides in MusicService
            playTrack(null, Constants.ACTION.PAUSE_ACTION);

        } else if (MusicService.TRACK_PAUSED) {
            btnPlayPause.setBackgroundResource(android.R.drawable.ic_media_pause);
            playTrack(null, Constants.ACTION.RESUME_ACTION);
        }
    }

    @OnClick(R.id.buttonNext)
    public void getNext() {
        mSeekBar.setProgress(0);

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
            saveTracksInfo();

        } else
            Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.track_not_found), Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.buttonPrevious)
    public void getPrevious() {
        mSeekBar.setProgress(0);

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
            saveTracksInfo();

        } else
            Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.track_not_found), Toast.LENGTH_SHORT).show();
    }

    // helper method to store current tracks information
    private void saveTracksInfo() {
        if (mTrackItems != null) {
            SharedPreferences prefs = getActivity().getApplicationContext().getSharedPreferences("tracks_info", Context.MODE_PRIVATE);
            SharedPreferences.Editor e = prefs.edit();
            Gson gson = new Gson();
            for (int i = 0; i < mTrackItems.size(); i++) {
                e.putString(String.valueOf(i), gson.toJson(mTrackItems.get(i)));
            }
            e.putInt("list_size", mTrackItems.size());
            e.putInt("current_index", mCurrentIndex);
            e.putInt("current_position", mSeekBar.getProgress());
            e.putInt("max_position", mSeekBar.getMax());
            e.putString("current_time", tvCurrentTime.getText().toString());
            e.putString("final_time", tvFinalTime.getText().toString());
            e.putBoolean("nav_back", mNavBack);
            e.commit();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("track_items", mTrackItems);
        outState.putInt("current_index", mCurrentIndex);
        outState.putInt("saved_position", mSeekBar.getProgress());
        outState.putInt("max_position", mSeekBar.getMax());
        outState.putString("current_time", tvCurrentTime.getText().toString());
        outState.putString("final_time", tvFinalTime.getText().toString());
        outState.putBoolean("nav_back", mNavBack);
    }

    // helper method called with previous or next button click
    private void resetTrackData() {
        mSeekBar.setProgress(0);
        tvCurrentTime.setText("");
        tvFinalTime.setText("");

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
        View rootView = inflater.inflate(R.layout.fragment_track_player_overlay, null);
        ButterKnife.inject(this, rootView);

        setHasOptionsMenu(true);

        artist_name.setText(mCurrentTrackItem.getName());
        album_name.setText(mCurrentTrackItem.getAlbum());
        track_name.setText(mCurrentTrackItem.getTrack());

        btnNext.setBackgroundResource(android.R.drawable.ic_media_next);
        btnPrevious.setBackgroundResource(android.R.drawable.ic_media_previous);

        // reloading trackPlayer info after fragment went down on device rotate or back button / rebuilding fragment
        if (MusicService.TRACK_PLAYING) {
            btnPlayPause.setBackgroundResource(android.R.drawable.ic_media_pause);
        } else if (MusicService.TRACK_PAUSED) {
            btnPlayPause.setBackgroundResource(android.R.drawable.ic_media_play);
            mSeekBar.setMax(mMaxPosition);
            mSeekBar.setProgress(mCurrentPosition);
            tvFinalTime.setText(mFinalTime);
            tvCurrentTime.setText(mCurrentTime);
        }

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
        }
        mNavBack = false;
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
                if (!MusicService.SERVICE_RUNNING) {
                    initService();
                }
                playTrack(mTrack, Constants.ACTION.PLAY_ACTION);
            } else {
                Toast.makeText(getActivity(), getResources().getString(R.string.track_not_found), Toast.LENGTH_SHORT).show();
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


    private void registerReceiver() {
            getActivity().registerReceiver(mBroadcastReceiver, new IntentFilter(
                    MusicService.BROADCAST_ACTION));
    }

    private void registerBufferReceiver() {
            getActivity().registerReceiver(mBroadcastBufferReceiver, new IntentFilter(
                    MusicService.BROADCAST_BUFFER));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unregisterReceivers();
    }


    private void unregisterReceivers() {
        // unregister mBroadcastReceiver for seekBar
        try {
            getActivity().unregisterReceiver(mBroadcastReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            getActivity().unregisterReceiver(mBroadcastBufferReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initService() {
        mTrackEndedFlag = 0;
        if (Utility.isConnected(getActivity().getApplicationContext())) {
            Intent startIntent = new Intent(getActivity().getApplicationContext(), MusicService.class);
            startIntent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
            getActivity().startService(startIntent);
        } else {
            Toast.makeText(getActivity().getApplicationContext(),
                    getResources().getString(R.string.network_connection_needed),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void playTrack(Track track, String action) {
        mTrackEndedFlag = 0;
        String url = null;
        if (track != null)
            url = track.preview_url;
        if (Utility.isConnected(getActivity().getApplicationContext())) {
            registerReceiver();
            Intent startIntent = new Intent(getActivity(), MusicService.class);
            if (url != null)
                startIntent.putExtra("sentAudioLink", url);
            startIntent.putExtra("largeImagePath", mCurrentTrackItem.getImage_path_large());
            startIntent.setAction(action);
            getActivity().startService(startIntent);

        } else {
            Toast.makeText(getActivity().getApplicationContext(),
                    getResources().getString(R.string.network_connection_needed),
                    Toast.LENGTH_LONG).show();
        }
    }

    // progress dialogue for buffering
    private void showProgressDialog(Intent bufferIntent) {
        String bufferValue = bufferIntent.getStringExtra("buffering");
        int bufferIntValue = Integer.parseInt(bufferValue);

        switch (bufferIntValue) {

            case 0:
                if (null != progressDialog) {
                    progressDialog.dismiss();
                }
                break;

            case 1:
                if (progressDialog == null) {
                    progressDialog = new ProgressDialog(getActivity(), ProgressDialog.THEME_HOLO_LIGHT);
                    progressDialog.setTitle(getResources().getString(R.string.loading_media));
                    progressDialog.show();
                }
                break;

            case 2:
                //btnPlayPause.setBackgroundResource(android.R.drawable.ic_media_play);
                break;
        }
    }

    private void startProgressDialog() {
        if (progressDialog == null && !MusicService.SERVICE_RUNNING) {
            progressDialog = new ProgressDialog(getActivity(), ProgressDialog.THEME_HOLO_LIGHT);
            progressDialog.setTitle(getResources().getString(R.string.loading_media));
            progressDialog.show();
        }
    }

    private void hideKeyboard() {
        getActivity().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    // Set up broadcast receiver
    private BroadcastReceiver mBroadcastBufferReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent bufferIntent) {
            showProgressDialog(bufferIntent);
        }
    };

    // unregister broadcast receiver
    // pass data to store
    @Override
    public void onPause() {
        super.onPause();
        saveTracksInfo();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceivers();
    }


    // register broadcast receiver
    @Override
    public void onResume() {
        super.onResume();
    }

    // methods of OnSeekBarChangeListener,
    // when user manually moves seekBar, broadcast new position to service
    @Override
    public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
        if (fromUser) {
            int seekPos = sb.getProgress();
            seekBarIntent.putExtra("seekpos", seekPos);
            getActivity().sendBroadcast(seekBarIntent);
        }
    }

    // alternatives to track seekBar if moved.
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

}
