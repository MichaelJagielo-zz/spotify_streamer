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

import kaaes.spotify.webapi.android.models.Track;

/*
* Main Activity for Spotify Streamer App
* Instantiates ArtistSearchFragment
*/
public class MainActivity extends Activity implements ArtistSearchFragment.Callback, TrackPlayerDialogFragment.ActionBarCallback { //, TrackPlayerActivity.TrackPlayerActivityCallback {
    private boolean mTwoPane;
    public static String COUNTRY_CODE;
    //public static boolean SHOW_NOTIFICATIONS;
    private SharedPreferences mSettings;
    private static MenuItem mNowPlaying;
    private CheckBox mDontShowAgain;
    private TrackPlayerDialogFragment mTrackPlayerDialogFragment;

//    // Progress dialogue and broadcast receiver variables
//    boolean mBufferBroadcastIsRegistered;
//    private ProgressDialog pdBuff = null;


    // used when user navigates back to player fragment
    private ArrayList<TrackItem> mTrackItems;
    private int mCurrentIndex;

    private SharedPreferences mSharedPreferences;

    SharedPreferences.Editor mEditor;

    private TrackItem mTrackItem;
    private TrackItem mCurrentTrackItem;
    private Track mTrack;


    // for keeping track of when user navigates back from MainActivity through "Now Playing" button
    private boolean mNavBack;
    private boolean mPaused;
    private boolean mEnabled;

    private int mSavedPosition;
    private int mMaxPosition;
    private int mCurrentPosition;
    private String mCurrentTime;
    private String mFinalTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mEnabled = false;
        if (savedInstanceState != null) {
            Log.d("", "inside savedInstanceState: ");
            mTrackItems = savedInstanceState.getParcelableArrayList("track_items");
            mCurrentIndex = savedInstanceState.getInt("current_index");
            mCurrentPosition = savedInstanceState.getInt("saved_position");
            mMaxPosition = savedInstanceState.getInt("max_position");
            mCurrentTime = savedInstanceState.getString("current_time");
            mFinalTime = savedInstanceState.getString("final_time");
            mPaused = savedInstanceState.getBoolean("paused");
            mNavBack = savedInstanceState.getBoolean("nav_back");
            mEnabled = savedInstanceState.getBoolean("enabled");
        } else {

            mSharedPreferences = getApplicationContext().getSharedPreferences("tracks_info", Context.MODE_PRIVATE);
            Log.d("", "onClick nowPlaying button, mSharedPreferences null: --------       -------------------  " + (mSharedPreferences == null));
            if (mSharedPreferences != null) {
                Log.d("", "inside mSharedPreferences: ---------------------- ------------- ---------------- ---------------- ");
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
                mPaused = mSharedPreferences.getBoolean("paused", false);
                mNavBack = mSharedPreferences.getBoolean("nav_back", true); // should be true at this point in any case
                mEnabled = mSharedPreferences.getBoolean("enabled", false); // false if null
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
        COUNTRY_CODE = mSettings.getString("country_code", "US"); // TODO: 7/11/15 create constant tags for keys 
        //SHOW_NOTIFICATIONS = mSettings.getBoolean("show_notifications", true);

        Log.d("", " onCreate: MainActivity: set mEnabled: ----- ----- ------ ------ ** " + mEnabled);

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
    public void enable(boolean bool) {
        mEnabled = bool;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mNowPlaying = menu.getItem(3);
        mNowPlaying.setEnabled(mEnabled);
        return true;
    }

    public static void setNowPlayingItem(boolean bool) { // // TODO: 7/26/15 remove this method and callBack - use the arg passed in prefs.??
        mNowPlaying.setEnabled(bool);
    }

    private void quitService() {
        Log.d("", "quitting service: ----------------------------------------");
        if (MusicService.SERVICE_RUNNING) {
            Intent quitIntent = new Intent(this, MusicService.class);
            quitIntent.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
            startService(quitIntent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("", "onOptionsItemsSelected: item " + item.toString() + "  " + item.getItemId());

        switch (item.getItemId()) {
            case R.id.action_quit:
                //NavUtils.navigateUpFromSameTask(this);
                 quitService();
                clearPreferences();

                this.finish();
                return true;
            case R.id.action_nowplaying:
                mSharedPreferences = getApplicationContext().getSharedPreferences("tracks_info", Context.MODE_PRIVATE);
                Log.d("", "onClick nowPlaying button, mSharedPreferences null: --------       -------------------  " + (mSharedPreferences == null));
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
                    mPaused = mSharedPreferences.getBoolean("paused", false);
                    mNavBack = mSharedPreferences.getBoolean("nav_back", true);
                    mEnabled = mSharedPreferences.getBoolean("enabled", true);
                }

                if (mTrackItems != null) {
                    Log.d("", "onClick nowPlaying button, mTrackItems not null");

                    Bundle bundle = new Bundle();
                    bundle.putParcelableArrayList("track_items", mTrackItems);
                    bundle.putInt("current_index", mCurrentIndex);
                    bundle.putInt("current_position", mCurrentPosition);
                    bundle.putInt("max_position", mMaxPosition);
                    bundle.putString("current_time", mCurrentTime);
                    bundle.putString("final_time", mFinalTime);
                    bundle.putBoolean("paused", mPaused);
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
                } else
                    Toast.makeText(getApplicationContext(), "we cant do this, sorry..", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.action_settings:
                //// TODO: 7/26/15 Remove checkbox for notif to be shown. 
                View checkBoxView = View.inflate(this, R.layout.checkbox, null);
                final CheckBox checkBox = (CheckBox) checkBoxView.findViewById(R.id.checkbox);
                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        Log.d("", "OnCheckedChangeListener: clicked, save to shared prefs..");
                        // Save to shared preferences
                        SharedPreferences.Editor editor = mSettings.edit();
                        //editor.putBoolean("show_notifications", isChecked);
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
            case R.id.action_sharetrack://// TODO: 7/26/15 remove this option? 
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

    @Override
    protected void onSaveInstanceState(Bundle outState) { //// TODO: 7/26/15 remove this if not needed
        super.onSaveInstanceState(outState);


        if (mTrackItems != null) {
            Log.d("", "onSaveInstanceState: mTrackItems size:" + mTrackItems.size());
            Log.d("", "onSaveInstanceState: mEnabled: " + mEnabled);
            Log.d("", "onSaveInstanceState: mNavBack: " + mNavBack);

            outState.putParcelableArrayList("track_items", mTrackItems);
            outState.putInt("current_index", mCurrentIndex);
            outState.putInt("saved_position", mCurrentPosition);
            outState.putInt("max_position", mMaxPosition);
            outState.putString("current_time", mCurrentTime);
            outState.putString("final_time", mFinalTime);
            outState.putBoolean("paused", !MusicService.TRACK_PLAYING); //// TODO: 7/26/15 remove TRACK_PAUSED from MusicService if not needed
            outState.putBoolean("nav_back", mNavBack);
            outState.putBoolean("enabled", mEnabled);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    //// TODO: 7/26/15 add back the alert dialog to exit?

    //    @Override
//    public void onBackPressed() {
//
//        if (mTrackPlayerDialogFragment != null) {
//            // close dialogFragment
//            mTrackPlayerDialogFragment.dismiss();
//        } else {
//            AlertDialog.Builder adb = new AlertDialog.Builder(this);
//            LayoutInflater adbInflater = LayoutInflater.from(this);
//            View checkBoxView = adbInflater.inflate(R.layout.alert_checkbox, null);
//            mDontShowAgain = (CheckBox) checkBoxView.findViewById(R.id.skip);
//            adb.setView(checkBoxView);
//            adb.setTitle(getResources().getString(R.string.attention));
//            adb.setMessage(getResources().getString(R.string.sure_exit));
//            mDontShowAgain.setText(getResources().getString(R.string.dont_show_again));
//            adb.setPositiveButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
//
//                public void onClick(DialogInterface dialog, int which) {
//                    Boolean checked = false;
//                    if (mDontShowAgain.isChecked())
//                        checked = true;
//                    // remove values saved for showing the NowPLaying button
//                    clearPreferences();
//
//                    SharedPreferences.Editor editor = mSettings.edit();
//                    editor.putBoolean("boolKeyExit", checked);
//                    editor.commit();
//
//                    quitService();
//                    MainActivity.this.finish();
//                    return;
//                }
//            });
//            adb.setNegativeButton(getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
//                public void onClick(DialogInterface dialog, int which) {
//                    Boolean checked = false;
//                    if (mDontShowAgain.isChecked())
//                        checked = true;
//                    SharedPreferences.Editor editor = mSettings.edit();
//                    editor.putBoolean("boolKeyExit", checked);
//                    editor.commit();
//                    return;
//                }
//            });
//
//            Boolean bool = mSettings.getBoolean("boolKeyExit", false);
//            if (!bool)
//                adb.show();
//            else {
//                quitService();
//                MainActivity.this.finish();
//            }
//        }
//    }
}