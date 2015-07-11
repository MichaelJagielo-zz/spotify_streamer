package com.inspirethis.mike.spotifystreamer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.inspirethis.mike.spotifystreamer.Util.Constants;

/*
* Main Activity for Spotify Streamer App
* Instantiates ArtistSearchFragment
*/
public class MainActivity extends Activity implements ArtistSearchFragment.Callback {
    private boolean mTwoPane;
    public static String COUNTRY_CODE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getActionBar().setDisplayHomeAsUpEnabled(true);

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

        switch(item.getItemId()) {
            // todo: Respond to the action bar's Up/Home button ??
            case R.id.action_quit :
                //NavUtils.navigateUpFromSameTask(this); ??
                if (MusicService.SERVICE_RUNNING) {
                    Intent quitIntent = new Intent(this, MusicService.class);
                    quitIntent.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
                    startService(quitIntent);
                }
                Log.d("", "quitting activity");
                this.finish();
                return true;
            case R.id.action_nowplaying :
                // TODO: 7/8/15 enable / disable this menu tiem according to if track has been selected.
                Intent nowPlaying = new Intent(this, TrackPlayerActivity.class);
                startActivity(nowPlaying);
                return true;

            case R.id.action_settings :
                View checkBoxView = View.inflate(this, R.layout.checkbox, null);
                final CheckBox checkBox = (CheckBox) checkBoxView.findViewById(R.id.checkbox);
                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        Log.d("", "OnCheckedChangeListener: clicked, save to shared prefs..");
                        // Save to shared preferences
                    }
                });
                checkBox.setText(" Show Notifications on Lock Screen ");

                Button countryCodeButton = (Button) checkBoxView.findViewById(R.id.country_button);
                countryCodeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        // TODO: 7/8/15 create dialog menu for two items: to allow user to select country code, and toggle showing notifications on lock screen
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
                                    // TODO: 7/11/15 set shared prefs
                                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.notifications_shown), Toast.LENGTH_SHORT).show();
                                } else
                                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.notifications_not_shown), Toast.LENGTH_SHORT).show();
                            }
                        }).show();

                break;
            case R.id.action_sharetrack :
                // // TODO: 7/8/15 add shareIntent to expose the external Spotify URL for the current track
        }
        return super.onOptionsItemSelected(item);
    }

    private void showCountryPickerDialog() {
        Log.d("", "showCountryPickerDialog: clicked, display list items..");
        final CharSequence[] items = {"one", "two", "three"};
        AlertDialog.Builder unitSelection = new AlertDialog.Builder(this);
        unitSelection.setTitle(getResources().getString(R.string.select_country_code));
        unitSelection.setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                Toast.makeText(getApplicationContext(), items[item], Toast.LENGTH_SHORT).show();
                // TODO: 7/11/15 set country in shared prefs
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