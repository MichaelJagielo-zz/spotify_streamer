package com.inspirethis.mike.spotifystreamer;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * TrackPlayerFragment class, displays given track which is currently playing
 * injecting views with ButterKnife
 * Created by mike on 6/16/15.
 */
public class TrackPlayerFragment extends Fragment {
    private TrackItem mTrackItem;

    @InjectView(R.id.tvArtistName)
    TextView artist_name;

    @InjectView(R.id.tvAlbumName)
    TextView album_name;

    @InjectView(R.id.trackImage)
    ImageView track_image;

    @InjectView(R.id.tvTrackName)
    TextView track_name;


    public TrackPlayerFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null || !savedInstanceState.containsKey("saved_track")) {
            Bundle bundle = getArguments();
            if (bundle != null)
                mTrackItem = bundle.getParcelable("track item");
        } else
            mTrackItem = savedInstanceState.getParcelable("saved_track");

        if (mTrackItem != null)
            initPlayer(mTrackItem.getSpotifyId());
        else
            Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.track_not_found), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable("track item", mTrackItem);
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_track_player, null);
        ButterKnife.inject(this, rootView);

        artist_name.setText(mTrackItem.getName());
        album_name.setText(mTrackItem.getAlbum());
        track_name.setText(mTrackItem.getTrack());

        if (mTrackItem.getImage_path_large() != null && !mTrackItem.getImage_path_large().equals(""))
            Picasso.with(getActivity().getApplicationContext()).load(mTrackItem.getImage_path_large()).into(track_image);
         else
            track_image.setImageResource(R.mipmap.greyscale_thumb);
        return rootView;
    }

    private void initPlayer(String track) {
    // TODO: implement player here

    }
}
