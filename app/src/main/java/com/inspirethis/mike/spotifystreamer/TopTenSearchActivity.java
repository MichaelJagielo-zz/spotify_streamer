package com.inspirethis.mike.spotifystreamer;

import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.MenuItem;

public class TopTenSearchActivity extends FragmentActivity {

    private String mArtistId;
    private String mArtistName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_top_ten_search);

        // on return to this Activity
        if (savedInstanceState != null) {
            mArtistId = savedInstanceState.getString("artist_id");
            mArtistName = savedInstanceState.getString("artist_name");

        } else {
            // first instance of Activity, bundle args from MainActivity
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                mArtistId = extras.getString("artist_id");
                mArtistName = extras.getString("artist_name");
            }
        }

        Bundle arguments = new Bundle();
        arguments.putString("artist_id", mArtistId);
        arguments.putString("artist_name", mArtistName);

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        TopTenSearchFragment topTenSearchFragment = new TopTenSearchFragment();
        topTenSearchFragment.setArguments(arguments);
        ft.add(R.id.top_ten_container, topTenSearchFragment, "TopTenSearchFragment");
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // used when Android system stops activity, to restore state
        outState.putString("artist_id", mArtistId);
        outState.putString("artist_name", mArtistName);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("", "onOptionsItemSelected: --------------------- clicked: ---------------------------------- ");
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
