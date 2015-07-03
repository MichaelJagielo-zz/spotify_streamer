package com.inspirethis.mike.spotifystreamer;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.inspirethis.mike.spotifystreamer.Util.Constants;
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
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link TrackPlayerDialogFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link TrackPlayerDialogFragment#newInstance} factory method to
 * create an instance of this fragment.
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

    Intent mServiceIntent;

    private boolean bMusicPlaying;
    private boolean bMusicWasPlaying;
    TelephonyManager telephonyManager;
    PhoneStateListener listener;

    private static int mTrackEndedFlag = 0;
    boolean mBroadcastIsRegistered;

    // constant for broadcast of seekBar position
    public static final String BROADCAST_SEEKBAR = "com.inspirethis.mike.spotifystreamer.sendseekbar";
    Intent seekBarIntent;

    // Progress dialogue and broadcast receiver variables
    boolean mBufferBroadcastIsRegistered;
    private ProgressDialog progressDialog = null;

    private int mCurrentPosition;

    private final String LOG_TAG = TrackPlayerDialogFragment.class.getSimpleName();


    //private OnFragmentInteractionListener mListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment TrackPlayerDialogFragment.
     */
    // TODO: Remove.
    public static TrackPlayerDialogFragment newInstance(String param1, String param2) {
        TrackPlayerDialogFragment fragment = new TrackPlayerDialogFragment();
        Bundle args = new Bundle();
//        args.putString(ARG_PARAM1, param1);
//        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public TrackPlayerDialogFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null || !savedInstanceState.containsKey("list")) {
            Bundle bundle = getArguments();
            if (bundle != null) {
                mCurrentIndex = bundle.getInt("index");
                mTrackItems = bundle.getParcelableArrayList("tracks_list");
                mCurrentTrackItem = mTrackItems.get(mCurrentIndex);
                mCurrentPosition = bundle.getInt("current_position");
                bMusicPlaying = bundle.getBoolean("is_playing");
                Log.d(LOG_TAG, "bundle not null, mCurrentPosition: " + mCurrentPosition);
                Log.d(LOG_TAG, "bundle not null, bMusicPlaying: " + bMusicPlaying);
            }
        } else {
            mTrackItems = savedInstanceState.getParcelableArrayList("list");
            mCurrentIndex = savedInstanceState.getInt("current_index");
            mCurrentTrackItem = mTrackItems.get(mCurrentIndex);
            bMusicPlaying = false;
            Log.d(LOG_TAG, "bundle null, bMusicPlaying: " + bMusicPlaying);
        }

        if (mCurrentTrackItem != null) {

            if (bMusicPlaying) {
                Log.d(LOG_TAG, "bMusicPlaying true: " + bMusicPlaying + " playing track");
                // resume to current position
                //playTrack(mTrack.preview_url, Constants.ACTION.RESUME_ACTION);

            } else {
                FetchTrackTask task = new FetchTrackTask();
                task.execute(mCurrentTrackItem.getSpotifyId());
            }

        } else
            Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.track_not_found), Toast.LENGTH_SHORT).show();

    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        menu.add(getResources().getString(R.string.menu_item_1));
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                Intent quitIntent = new Intent(getActivity(), MusicService.class);
                quitIntent.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
                getActivity().startService(quitIntent);
                getActivity().finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
        Log.d("", "updateUI  mTrackEndedFlag: " + mTrackEndedFlag);
        Log.d("", "updateUI: mSeekBar.getMax(): " + mSeekBar.getMax());

        Log.d("", "updateUI: mSeekBar.getProgress(): " + mSeekBar.getProgress());

        mSeekBar.setMax(seekMax);
        mSeekBar.setProgress(seekProgress);

        if (mTrackEndedFlag == 1) {
            btnPlayPause.setBackgroundResource(android.R.drawable.ic_media_pause);
        }
    }


    @OnClick(R.id.buttonPlay)
    public void playPause() {
        Log.d(LOG_TAG, "****** bMusicPlaying " + bMusicPlaying + " mTrackEndedFlag: " + mTrackEndedFlag + " mSeekBar.getProgress(): " + mSeekBar.getProgress());
        if (!bMusicPlaying) {
            bMusicPlaying = true;
            // track is not playing, get ready to play
            btnPlayPause.setBackgroundResource(android.R.drawable.ic_media_pause);
            Log.d(LOG_TAG, "****** mTrackEndedFlag: " + mTrackEndedFlag + "mSeekBar.getProgress(): " + mSeekBar.getProgress());
            // resuming an existing track play
            if (mSeekBar.getProgress() > 0 && mTrackEndedFlag != 1) {
                playTrack(mTrack.preview_url, Constants.ACTION.RESUME_ACTION);
            }

            // start new track
            else
                playTrack(mTrack.preview_url, Constants.ACTION.PLAY_ACTION);

            // replaying an existing track that has ended
        } else if (mTrackEndedFlag == 1) {
            playTrack(mTrack.preview_url, Constants.ACTION.PLAY_ACTION); //// TODO: 6/30/15 reset action needed?

        } else if (bMusicPlaying) {
            // track is playing, pausing track
            btnPlayPause.setBackgroundResource(android.R.drawable.ic_media_play);
            bMusicPlaying = false;
            playTrack(mTrack.preview_url, Constants.ACTION.PAUSE_ACTION);
        }
    }


    @OnClick(R.id.buttonNext)
    public void getNext() {
        mSeekBar.setProgress(0);
        //unregisterReceiver();

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
        //unregisterReceiver();

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
        outState.putInt("current_position", mSeekBar.getProgress());
        Log.d(LOG_TAG, "onSaveInstanceState: bMusicPlaying: " + bMusicPlaying);
        outState.putBoolean("is_playing", bMusicPlaying);

        super.onSaveInstanceState(outState);
    }

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

//TODO: get parent view reset on each call inflate trackplayer

        View rootView = inflater.inflate(R.layout.fragment_track_player_overlay, null);
        //rootView.setBackground(new ColorDrawable(Color.TRANSPARENT));
        ButterKnife.inject(this, rootView);

        setHasOptionsMenu(true);

        artist_name.setText(mCurrentTrackItem.getName());
        album_name.setText(mCurrentTrackItem.getAlbum());
        track_name.setText(mCurrentTrackItem.getTrack());

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

                if (!MusicService.SERVICE_RUNNING) {
                    initService(mTrack.preview_url);
                } else
                    bMusicPlaying = true;
                playTrack(mTrack.preview_url, Constants.ACTION.PLAY_ACTION);

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
        bMusicPlaying = false;
        if (mBroadcastIsRegistered) {
            try {
                getActivity().unregisterReceiver(broadcastReceiver);
                mBroadcastIsRegistered = false;
            } catch (Exception e) {
                Log.d(LOG_TAG, getResources().getString(R.string.broadcast_register_error), e);
                e.printStackTrace();
            }
        }

        try {
            getActivity().stopService(mServiceIntent);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void unregisterReceiver() {
        // unregister broadcastReceiver for seekBar
        bMusicPlaying = false;
        if (mBroadcastIsRegistered) {
            try {
                getActivity().unregisterReceiver(broadcastReceiver);
                mBroadcastIsRegistered = false;
            } catch (Exception e) {
                Log.d(LOG_TAG, getResources().getString(R.string.broadcast_register_error), e);
                e.printStackTrace();
            }
        }

        try {
            Intent startIntent = new Intent(getActivity(), MusicService.class);
            startIntent.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
            getActivity().startService(startIntent);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initService(String url) {
        mTrackEndedFlag = 0;
        if (isConnected()) {

            bMusicPlaying = true;

            Intent startIntent = new Intent(getActivity().getApplicationContext(), MusicService.class);
            startIntent.putExtra("sentAudioLink", url);
            startIntent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
            getActivity().startService(startIntent);

            getActivity().registerReceiver(broadcastReceiver, new IntentFilter(
                    MusicService.BROADCAST_ACTION));
            mBroadcastIsRegistered = true;

        } else {
            Toast.makeText(getActivity().getApplicationContext(),
                    getResources().getString(R.string.network_connection_needed),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void playTrack(String url, String action) {
        mTrackEndedFlag = 0;
        if (isConnected()) {
            //bMusicPlaying = true;
            Intent startIntent = new Intent(getActivity(), MusicService.class);
            startIntent.putExtra("sentAudioLink", url);
            startIntent.setAction(action);
            getActivity().startService(startIntent);
        } else {
            Toast.makeText(getActivity().getApplicationContext(),
                    getResources().getString(R.string.network_connection_needed),
                    Toast.LENGTH_LONG).show();
        }

        getActivity().registerReceiver(broadcastReceiver, new IntentFilter(MusicService.BROADCAST_ACTION));
        mBroadcastIsRegistered = true;

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
                progressDialog = new ProgressDialog(getActivity(), ProgressDialog.THEME_HOLO_DARK);
                progressDialog.setTitle(getResources().getString(R.string.loading_media));
                progressDialog.show();
                break;

            case 2:
                btnPlayPause.setBackgroundResource(android.R.drawable.ic_media_play);
                break;
        }
    }


    // Set up broadcast receiver
    private BroadcastReceiver broadcastBufferReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent bufferIntent) {
            showProgressDialog(bufferIntent);
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


// TODO: remove listener interface if not needed
//    @Override
//    public void onAttach(Activity activity) {
//        super.onAttach(activity);
//        try {
//            mListener = (OnFragmentInteractionListener) activity;
//        } catch (ClassCastException e) {
//            throw new ClassCastException(activity.toString()
//                    + " must implement OnFragmentInteractionListener");
//        }
//    }

    @Override
    public void onDetach() {
        super.onDetach();
        //mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

}
