package com.inspirethis.mike.spotifystreamer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.inspirethis.mike.spotifystreamer.Util.Constants;
import com.inspirethis.mike.spotifystreamer.Util.Utility;

import java.util.ArrayList;

/*
* Main Activity for Spotify Streamer App
* Instantiates ArtistSearchFragment
*/
public class MainActivity extends Activity implements ArtistSearchFragment.Callback {
    private boolean mTwoPane;
    public static String COUNTRY_CODE;
    private SharedPreferences mSettings;
    private MenuItem mNowPlaying;
    private TrackPlayerDialogFragment mTrackPlayerDialogFragment;

    // used when user navigates back to player fragment
    private ArrayList<TrackItem> mTrackItems;
    private int mCurrentIndex;

    private SharedPreferences mSharedPreferences;

    // for keeping track of when user navigates back from MainActivity through "Now Playing" button
    private boolean mNavBack;
    private int mMaxPosition;
    private int mCurrentPosition;
    private String mCurrentTime;
    private String mFinalTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            mTrackItems = savedInstanceState.getParcelableArrayList("track_items");
            mCurrentIndex = savedInstanceState.getInt("current_index");
            mCurrentPosition = savedInstanceState.getInt("saved_position");
            mMaxPosition = savedInstanceState.getInt("max_position");
            mCurrentTime = savedInstanceState.getString("current_time");
            mFinalTime = savedInstanceState.getString("final_time");
            mNavBack = savedInstanceState.getBoolean("nav_back");
        } else {
            mSharedPreferences = getApplicationContext().getSharedPreferences("tracks_info", Context.MODE_PRIVATE);
            if (mSharedPreferences != null) {
                GsonBuilder gsonb = new GsonBuilder();
                Gson gson = gsonb.create();

                int size = mSharedPreferences.getInt("list_size", 0);
                if (size != 0) {
                    ArrayList<TrackItem> list = new ArrayList(mSharedPreferences.getInt("list_size", 0));
                    for (int i = 0; i < size; i++) {
                        String objectString = mSharedPreferences.getString(String.valueOf(i), "");
                        if (!objectString.equals(""))
                            list.add(gson.fromJson(objectString, TrackItem.class));
                    }
                    mTrackItems = list;
                }
                mCurrentIndex = mSharedPreferences.getInt("current_index", 0);
                mCurrentPosition = mSharedPreferences.getInt("current_position", 0);
                mMaxPosition = mSharedPreferences.getInt("max_position", 30);
                mCurrentTime = mSharedPreferences.getString("current_time", "");
                mFinalTime = mSharedPreferences.getString("final_time", "");
                mNavBack = mSharedPreferences.getBoolean("nav_back", true);
            }
        }

        if (findViewById(R.id.top_ten_container) != null) {
            mTwoPane = true;

            if (savedInstanceState == null) {
                getFragmentManager().beginTransaction()
                        .replace(R.id.top_ten_container, new TopTenSearchFragment())
                        .commit();
            }
        } else {
            mTwoPane = false;
        }
        mSettings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        COUNTRY_CODE = mSettings.getString("country_code", "US");
    }


    @Override
    public void onItemSelected(String artistId, String artistName) {
        Bundle args = new Bundle();
        args.putString("artist_id", artistId);
        args.putString("artist_name", artistName);
        Log.d("", "in MainActivity: artist_id: " + artistId);

        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.

            TopTenSearchFragment fragment = new TopTenSearchFragment();
            fragment.setArguments(args);

            getFragmentManager().beginTransaction()
                    .replace(R.id.top_ten_container, fragment)
                    .disallowAddToBackStack()
                    .commit();
        } else {
            Intent intent = new Intent(this, TopTenSearchActivity.class)
                    .putExtras(args);
            startActivity(intent);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mNowPlaying = menu.getItem(2);
        if (MusicService.TRACK_PLAYING || MusicService.TRACK_PAUSED)
            mNowPlaying.setEnabled(true);
        else
            mNowPlaying.setEnabled(false);
        return true;
    }

    private void quitService() {
        if (MusicService.SERVICE_RUNNING) {
            Intent quitIntent = new Intent(this, MusicService.class);
            quitIntent.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
            startService(quitIntent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_quit:
                quitService();
                clearPreferences();
                this.finish();
                return true;
            case R.id.action_nowplaying:
                mSharedPreferences = getApplicationContext().getSharedPreferences("tracks_info", Context.MODE_PRIVATE);
                if (mSharedPreferences != null) {
                    GsonBuilder gsonb = new GsonBuilder();
                    Gson gson = gsonb.create();

                    int size = mSharedPreferences.getInt("list_size", 0);
                    if (size != 0) {
                        ArrayList<TrackItem> list = new ArrayList(mSharedPreferences.getInt("list_size", 0));
                        for (int i = 0; i < size; i++) {
                            String objectString = mSharedPreferences.getString(String.valueOf(i), "");
                            if (!objectString.equals(""))
                                list.add(gson.fromJson(objectString, TrackItem.class));
                        }
                        mTrackItems = list;
                    }
                    mCurrentIndex = mSharedPreferences.getInt("current_index", 0);
                    mCurrentPosition = mSharedPreferences.getInt("current_position", 0);
                    mMaxPosition = mSharedPreferences.getInt("max_position", 30);
                    mCurrentTime = mSharedPreferences.getString("current_time", "");
                    mFinalTime = mSharedPreferences.getString("final_time", "");
                    mNavBack = mSharedPreferences.getBoolean("nav_back", true);
                }

                if (mTrackItems != null) {
                    Bundle bundle = new Bundle();
                    bundle.putParcelableArrayList("track_items", mTrackItems);
                    bundle.putInt("current_index", mCurrentIndex);
                    bundle.putInt("current_position", mCurrentPosition);
                    bundle.putInt("max_position", mMaxPosition);
                    bundle.putString("current_time", mCurrentTime);
                    bundle.putString("final_time", mFinalTime);
                    bundle.putBoolean("two_pane", mTwoPane);
                    bundle.putBoolean("nav_back", true);

                    if (!mTwoPane) {
                        // phone case
                        Intent nowPlaying = new Intent(this, TrackPlayerActivity.class);
                        nowPlaying.putExtras(bundle);
                        nowPlaying.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        startActivity(nowPlaying);
                    } else {
                        // tablet case, MasterDetail layout
                        mTrackPlayerDialogFragment = (TrackPlayerDialogFragment) getFragmentManager().findFragmentById(R.id.track_player_container);
                        if (mTrackPlayerDialogFragment == null) {
                            final FragmentTransaction ft = getFragmentManager().beginTransaction();
                            mTrackPlayerDialogFragment = new TrackPlayerDialogFragment();
                            mTrackPlayerDialogFragment.setArguments(bundle);
                            ft.replace(R.id.two_pane_layout, mTrackPlayerDialogFragment, "trackPlayerFragmentOverlay");
                            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                            ft.addToBackStack(null);
                            ft.commit();
                        }
                    }
                }
                return true;
            case R.id.action_settings:
                View view = View.inflate(this, R.layout.settings, null);
                Button countryCodeButton = (Button) view.findViewById(R.id.country_button);
                countryCodeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showCountryPickerDialog();
                    }
                });
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getResources().getString(R.string.settings));
                builder.setView(view)
                        .setCancelable(false)
                        .setPositiveButton(getResources().getString(R.string.done), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                            }
                        }).show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void clearPreferences() {
        mSharedPreferences = getApplicationContext().getSharedPreferences("tracks_info", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.clear();
        editor.commit();
    }

    private void showCountryPickerDialog() {
        Log.d("", "showCountryPickerDialog: clicked, display list items..");
        final CharSequence[] items = Utility.getAvailableCountries();
        int preselect = 0;
        for (int i = 0; i < items.length; i++) {
            if (items[i].equals(COUNTRY_CODE))
                preselect = i;
        }
        AlertDialog.Builder unitSelection = new AlertDialog.Builder(this);
        unitSelection.setTitle(getResources().getString(R.string.select_country_code));
        unitSelection.setSingleChoiceItems(items, preselect, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                Toast.makeText(getApplicationContext(), items[item], Toast.LENGTH_SHORT).show();
                COUNTRY_CODE = items[item].toString();
                SharedPreferences.Editor editor = mSettings.edit();
                editor.putString("country_code", COUNTRY_CODE);
                editor.commit();
            }
        });
        unitSelection.setPositiveButton("Done", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog alert = unitSelection.create();
        alert.show();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);


        if (mTrackItems != null) {
            outState.putParcelableArrayList("track_items", mTrackItems);
            outState.putInt("current_index", mCurrentIndex);
            outState.putInt("saved_position", mCurrentPosition);
            outState.putInt("max_position", mMaxPosition);
            outState.putString("current_time", mCurrentTime);
            outState.putString("final_time", mFinalTime);
            outState.putBoolean("nav_back", mNavBack);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (!mTwoPane) {
            Intent mainActivity = new Intent(Intent.ACTION_MAIN);
            mainActivity.addCategory(Intent.CATEGORY_HOME);
            mainActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(mainActivity);
        } else {
            super.onBackPressed();

            DialogFragment dialogFragment = (DialogFragment) this.getFragmentManager().findFragmentByTag("trackPlayerFragmentOverlay");
            if (dialogFragment != null) {
                dialogFragment.dismiss();
            } else if (isTaskRoot()) {
                Intent mainActivity = new Intent(Intent.ACTION_MAIN);
                mainActivity.addCategory(Intent.CATEGORY_HOME);
                mainActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(mainActivity);
            }
        }
    }
}