package com.inspirethis.mike.spotifystreamer;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.TracksPager;
import retrofit.RetrofitError;

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
    private boolean mTwoPane;

    public TopTenSearchFragment() {
    }

    private void performSearch(String search) {
        if (isConnected()) {
            FetchTracksTask task = new FetchTracksTask();
            task.execute(search);
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
            mArtist = bundle.getString("artist");
            if (mTrackItems.size() == 0)
                performSearch(mArtist);
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
                bundle.putInt("index", mCurrentIndex); //// TODO: 7/2/15 create static TAG for keys. store these in Constants class
                bundle.putParcelableArrayList("tracks_list", mTrackItems);


                if (getActivity().findViewById(R.id.track_player_container) != null) {
                    mTwoPane = true;
                    // adding DialogFragment for view overlay on two pane view
                    TrackPlayerDialogFragment trackPlayerFragment = (TrackPlayerDialogFragment) getFragmentManager().findFragmentById(R.id.track_player_container);
                    if (trackPlayerFragment == null) {
                        final FragmentTransaction ft = getFragmentManager().beginTransaction();
                        trackPlayerFragment = new TrackPlayerDialogFragment();
                        trackPlayerFragment.setArguments(bundle);
                        ft.add(R.id.two_pane_layout, trackPlayerFragment, "trackPlayerFragmentOverlay");
                        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                        ft.addToBackStack("trackPlayerFragmentOverlay");
                        ft.commit();
                    }
                } else {
                    mTwoPane = false;

                    Intent trackPlayer = new Intent(getActivity(), TrackPlayerActivity.class).putExtras(bundle);
                    startActivity(trackPlayer);
                }
            }
        });
        return rootView;
    }

    public class FetchTracksTask extends AsyncTask<String, Void, List<Track>> {
        private final String LOG_TAG = FetchTracksTask.class.getSimpleName();

        @Override
        protected List<Track> doInBackground(String... params) {

            if (params.length == 0 || params[0].equals("")) {
                return null;
            }

            TracksPager tracksPager = null;

            try {
                SpotifyApi api = new SpotifyApi();
                SpotifyService spotify = api.getService();
                tracksPager = spotify.searchTracks(params[0]);

            } catch (RetrofitError error) {

                SpotifyError spotifyError = SpotifyError.fromRetrofitError(error);
                Log.d(LOG_TAG, spotifyError.getMessage());
                return null;
            }

            return tracksPager.tracks.items;
        }

        @Override
        protected void onPostExecute(List<Track> result) {
            if (result != null) {
                if (result.size() == 0)
                    Toast.makeText(getActivity(), getResources().getString(R.string.no_artists_found), Toast.LENGTH_SHORT).show();
                if (mToptenAdapter != null)
                    mToptenAdapter.clear();
                for (Track t : result) {
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
                    }
                }
            }
        }
    }

    private boolean isConnected() {
        return isWifiConnected() || isCellularConnected();
    }

    private boolean isWifiConnected() {
        ConnectivityManager connMgr = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected() && networkInfo.getType() == ConnectivityManager.TYPE_WIFI);
    }

    private boolean isCellularConnected() {
        ConnectivityManager connMgr = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected() && networkInfo.getType() == ConnectivityManager.TYPE_MOBILE);
    }
}
