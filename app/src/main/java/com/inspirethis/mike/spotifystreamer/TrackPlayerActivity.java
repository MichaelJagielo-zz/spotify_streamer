package com.inspirethis.mike.spotifystreamer;

import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;

public class TrackPlayerActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_player);


        Bundle extras = getIntent().getExtras();
        int index = extras.getInt("index");
        ArrayList<TrackItem> list = extras.getParcelableArrayList("tracks_list");


        if (savedInstanceState == null) {
            Bundle bundle = new Bundle();
            bundle.putInt("index", index);
            bundle.putParcelableArrayList("tracks_list", list);

            FragmentTransaction ft = getFragmentManager().beginTransaction();
            // instantiate fragment for phone
            TrackPlayerFragment trackPlayerFragment = new TrackPlayerFragment();
            trackPlayerFragment.setArguments(bundle);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
