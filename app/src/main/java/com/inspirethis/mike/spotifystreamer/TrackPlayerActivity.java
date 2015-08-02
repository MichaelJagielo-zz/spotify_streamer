package com.inspirethis.mike.spotifystreamer;

import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;

import java.util.ArrayList;

public class TrackPlayerActivity extends FragmentActivity {

    private ArrayList<TrackItem> mTrackItems;
    private int mCurrentIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_player);

        if (savedInstanceState != null) {
            mTrackItems = savedInstanceState.getParcelableArrayList("track_items");
            mCurrentIndex = savedInstanceState.getInt("current_index");
        } else {
            Bundle extras = getIntent().getExtras();
                // instantiate fragment for phone
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                TrackPlayerFragment trackPlayerFragment = new TrackPlayerFragment();
                trackPlayerFragment.setArguments(extras);
                ft.add(R.id.playTrack, trackPlayerFragment, "trackPlayerFragment");
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_track_player, menu);
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // used when Android system stops activity, to restore state
        outState.putParcelableArrayList("track_items", mTrackItems);
        outState.putInt("current_index", mCurrentIndex);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mTrackItems = savedInstanceState.getParcelableArrayList("track_items");
        mCurrentIndex = savedInstanceState.getInt("current_index");
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}


