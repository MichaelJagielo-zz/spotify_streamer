package com.inspirethis.mike.spotifystreamer;

import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class TopTenSearchActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_top_ten_search);

        Bundle extras = getIntent().getExtras();
        String artist_id = extras.getString("artist_id");
        String artist_name = extras.getString("artist_name");


        if (savedInstanceState == null) {
            Bundle arguments = new Bundle();
            arguments.putString("artist_id", artist_id);
            arguments.putString("artist_name", artist_name);
            FragmentTransaction ft = getFragmentManager().beginTransaction();

            TopTenSearchFragment topTenSearchFragment = new TopTenSearchFragment();
            topTenSearchFragment.setArguments(arguments);
            ft.add(R.id.top_ten_container, topTenSearchFragment, "TopTenSearchFragment");
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.commit();
        }
    }

}
