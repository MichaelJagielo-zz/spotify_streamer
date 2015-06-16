package com.inspirethis.mike.spotifystreamer;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by mike on 6/16/15.
 */
public class TrackPlayerFragment extends Fragment {
    private String mTrack;

    public TrackPlayerFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if(bundle != null) {
            //final String track = bundle.getString("track"); //todo: pass parcelable object here
            TrackItem trackItem = bundle.getParcelable("track item");
            //Log.d("track: ", track);
            playTrack(trackItem.getSpotifyId());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //return super.onCreateView(inflater, container, savedInstanceState);

        View rootView = inflater.inflate(R.layout.fragment_track_player, container, false);

        return rootView;
    }

    private void playTrack(String track) {

    }
}
