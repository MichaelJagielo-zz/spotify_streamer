package com.inspirethis.mike.spotifystreamer;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.widget.CheckBox;
import android.widget.CompoundButton;
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
public class MainActivity extends Activity implements ArtistSearchFragment.Callback, TrackPlayerDialogFragment.ActionBarCallback { //, TrackPlayerActivity.TrackPlayerActivityCallback {
    private boolean mTwoPane;
    public static String COUNTRY_CODE;
    public static boolean SHOW_NOTIFICATIONS;
    private SharedPreferences mSettings;
    private static MenuItem mNowPlaying;

    private boolean mEnabled;

    // used when user navigates back to player fragment
    private ArrayList<TrackItem> mTrackItems;
    private int mCurrentIndex;

    private SharedPreferences mSharedPreferences;

    SharedPreferences.Editor mEditor;

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
            mTwoPane = false;
        }

        mSettings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        COUNTRY_CODE = mSettings.getString("country_code", "US"); // TODO: 7/11/15 create constant tags for keys 
        SHOW_NOTIFICATIONS = mSettings.getBoolean("show_notifications", true);

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
    public void setEnabled(boolean bool) {
        mEnabled = bool;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mNowPlaying = menu.getItem(3);
        mNowPlaying.setEnabled(mEnabled);
        return true;

    }

    public static void setNowPlayingItem(boolean bool) {
        mNowPlaying.setEnabled(bool);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("", "onOptionsItemsSelected: item " + item.toString() + "  " + item.getItemId());

        switch (item.getItemId()) {
            case R.id.action_quit:
                //NavUtils.navigateUpFromSameTask(this);
                if (MusicService.SERVICE_RUNNING) {
                    Intent quitIntent = new Intent(this, MusicService.class);
                    quitIntent.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
                    startService(quitIntent);
                }
                clearPreferences();

                this.finish();
                return true;
            case R.id.action_nowplaying:
                mSharedPreferences = getApplicationContext().getSharedPreferences("tracks_info", Context.MODE_PRIVATE);
                Log.d("", "onClick nowPlaying button, mSharedPreferences null: --------       -------------------  " + (mSharedPreferences != null) );
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
                }

                mCurrentIndex = mSharedPreferences.getInt("current_index", 0);

                if (mTrackItems != null) {
                    Log.d("", "onClick nowPlaying button, mTrackItems not null");

                    Bundle bundle = new Bundle();
                    bundle.putParcelableArrayList("track_items", mTrackItems);
                    bundle.putInt("current_index", mCurrentIndex);
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
                        TrackPlayerDialogFragment trackPlayerFragment = (TrackPlayerDialogFragment) getFragmentManager().findFragmentById(R.id.track_player_container);
                        if (trackPlayerFragment == null) {
                            final FragmentTransaction ft = getFragmentManager().beginTransaction();
                            trackPlayerFragment = new TrackPlayerDialogFragment();
                            trackPlayerFragment.setArguments(bundle);
                            ft.replace(R.id.two_pane_layout, trackPlayerFragment, "trackPlayerFragmentOverlay");
                            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                            ft.addToBackStack(null);
                            ft.commit();
                        }
                    }
                } else
                    Toast.makeText(getApplicationContext(), "we cant do this, sorry..", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.action_settings:
                View checkBoxView = View.inflate(this, R.layout.checkbox, null);
                final CheckBox checkBox = (CheckBox) checkBoxView.findViewById(R.id.checkbox);
                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        Log.d("", "OnCheckedChangeListener: clicked, save to shared prefs..");
                        // Save to shared preferences
                        SharedPreferences.Editor editor = mSettings.edit();
                        editor.putBoolean("show_notifications", isChecked);
                        editor.commit();
                    }
                });
                checkBox.setText(" Show Notifications on Lock Screen "); //// TODO: 7/11/15 add string to res folder remove unneeded logs
                Button countryCodeButton = (Button) checkBoxView.findViewById(R.id.country_button);
                countryCodeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showCountryPickerDialog();
                    }
                });
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Settings: ");
                builder.setView(checkBoxView)
                        .setCancelable(false)
                        .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                if (checkBox.isChecked()) {
                                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.notifications_shown), Toast.LENGTH_SHORT).show();
                                } else
                                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.notifications_not_shown), Toast.LENGTH_SHORT).show();
                            }
                        }).show();
                break;
            case R.id.action_sharetrack:
                // // TODO: 7/8/15 add shareIntent to expose the external Spotify URL for the current track
        }

        return super.

                onOptionsItemSelected(item);

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
        AlertDialog.Builder unitSelection = new AlertDialog.Builder(this);
        unitSelection.setTitle(getResources().getString(R.string.select_country_code));
        unitSelection.setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
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

}