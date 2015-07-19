package com.inspirethis.mike.spotifystreamer;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.inspirethis.mike.spotifystreamer.Util.Utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * TopTenListViewAdapter class, displays results of Top Ten query for given Artist on Spotify API
 * Instantiates TrackPlayerFragment
 * Created by mike on 6/15/15.
 */
public class TopTenSearchFragment extends Fragment {

    //private final String LOG_TAG = TopTenSearchFragment.class.getSimpleName();

    private TopTenListViewAdapter mToptenAdapter;
    private ArrayList<TrackItem> mTrackItems;
    private int mCurrentIndex;
    private String mArtist;
    private String mID;
    //private boolean mTwoPane;

    public TopTenSearchFragment() {
    }

    private void performSearch(String search) {
        if (Utility.isConnected(getActivity().getApplicationContext())) {
            fetchTopTen(search);
        } else
            Toast.makeText(getActivity(), getResources().getString(R.string.network_connection_needed), Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null || !savedInstanceState.containsKey("track items")) {
            mTrackItems = new ArrayList<>();
        } else {
            mTrackItems = savedInstanceState.getParcelableArrayList("track items");
        }
        // Add this line in order for this fragment to handle menu events.
        setHasOptionsMenu(true);

        Bundle bundle = getArguments();
        if (bundle != null) {
            mArtist = bundle.getString("artist_name");
            mID = bundle.getString("artist_id");
            Log.d("", "mID value in topTenSearchFragment: " + mID);
            if (mTrackItems.size() == 0)
                    performSearch(mID);
        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mTrackItems != null) {
            outState.putParcelableArrayList("track items", mTrackItems);
            outState.putInt("index", mCurrentIndex);
        }

        super.onSaveInstanceState(outState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mToptenAdapter = new TopTenListViewAdapter(getActivity(), R.layout.topten_list_item, mTrackItems);

        View rootView = inflater.inflate(R.layout.fragment_topten_search, container, false);

        ListView listView = (ListView) rootView.findViewById(R.id.listview_tracks);
        listView.setAdapter(mToptenAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

                mCurrentIndex = position;
                TrackItem trackItem = mToptenAdapter.getItem(mCurrentIndex);

                Bundle bundle = new Bundle();
                bundle.putInt("current_index", mCurrentIndex); //// TODO: 7/2/15 create static TAG for keys. store these in Constants class
                bundle.putParcelableArrayList("track_items", mTrackItems);


                if (getActivity().findViewById(R.id.track_player_container) != null) {
                    //mTwoPane = true;
                    // adding DialogFragment for view overlay on two pane view
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
                } else {
                    //mTwoPane = false;

                    Intent trackPlayer = new Intent(getActivity(), TrackPlayerActivity.class).putExtras(bundle);
                    startActivity(trackPlayer);
                }
            }
        });
        return rootView;
    }


    private void fetchTopTen(String id) {
Log.d("", " in fetchTopTen: id: " + id);
        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put(SpotifyService.COUNTRY, MainActivity.COUNTRY_CODE);

        SpotifyApi api = new SpotifyApi();
        SpotifyService spotify = api.getService();


        spotify.getArtistTopTrack(id, queryMap, new Callback<Tracks>() {
            @Override
            public void success(final Tracks tracks, Response response) {

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mToptenAdapter != null)
                            mToptenAdapter.clear();
                        if (tracks.tracks.isEmpty()) {
                            Toast.makeText(getActivity(), getResources().getString(R.string.no_tracks_found), Toast.LENGTH_SHORT).show();

                        } else {
                            for (Track t : tracks.tracks) {
                                Log.d("", "track URI: " + t.uri);

                                String url_small_image = "";
                                String url_large_image = "";

                                if (t.album.images.size() > 0) {
                                    for (Image img : t.album.images) {
                                        if (img.width < 250)
                                            url_small_image = img.url;
                                        if (img.width > 600)
                                            url_large_image = img.url;
                                    }

                                    if (url_small_image.equals(""))
                                        url_small_image = t.album.images.get(0).url;
                                    if (url_large_image.equals(""))
                                        url_large_image = t.album.images.get(0).url;
                                    mTrackItems.add(new TrackItem(t.id, mArtist, t.album.name, t.name, url_small_image, url_large_image));
                                    Log.d("", "mTrackItems size: " + mTrackItems.size());
                                }
                            }
                        }
                    }

                });
            }


            @Override
            public void failure(final RetrofitError error) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getActivity(), getResources().getString(R.string.error_top_ten_search), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

    }

}
