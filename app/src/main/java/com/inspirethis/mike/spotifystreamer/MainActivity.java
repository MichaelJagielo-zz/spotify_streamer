package com.inspirethis.mike.spotifystreamer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.inspirethis.mike.spotifystreamer.Util.Constants;

/*
* Main Activity for Spotify Streamer App
* Instantiates ArtistSearchFragment
*/
public class MainActivity extends Activity implements ArtistSearchFragment.Callback {
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        if (findViewById(R.id.top_ten_container) != null) {
            mTwoPane = true;


            if (savedInstanceState == null) {
                getFragmentManager().beginTransaction()
                        .replace(R.id.top_ten_container, new TopTenSearchFragment())
                        .commit();
            }

        } else {
            mTwoPane = false; // this is a one pane UI for phones
        }
    }

    @Override
    public void onItemSelected(String artist) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle args = new Bundle();
            args.putString("artist", artist);

            TopTenSearchFragment fragment = new TopTenSearchFragment();
            fragment.setArguments(args);

            getFragmentManager().beginTransaction()
                    .replace(R.id.top_ten_container, fragment)
                    .disallowAddToBackStack()
                    .commit();
        } else {
            Intent intent = new Intent(this, TopTenSearchActivity.class)
                    .putExtra("artist", artist);
            startActivity(intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("", "onOptionsItemsSelected: item " + item.toString() + "  " + item.getItemId());
        switch (item.toString()) {
            // Respond to the action bar's Up/Home button

            case "Quit":
                //NavUtils.navigateUpFromSameTask(this);
                if (MusicService.SERVICE_RUNNING) {
                    Intent quitIntent = new Intent(this, MusicService.class);
                    quitIntent.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
                    startService(quitIntent);
                }
                Log.d("", "quitting activity");
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}