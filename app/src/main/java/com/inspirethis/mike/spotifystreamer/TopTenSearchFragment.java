package com.inspirethis.mike.spotifystreamer;

import android.app.Fragment;
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
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.TracksPager;

/**
 * Created by mike on 6/15/15.
 */
public class TopTenSearchFragment extends Fragment {

    private TopTenListViewAdapter mToptenAdapter;
    private ArrayList<TrackItem> trackItems;

    public TopTenSearchFragment() {}

    private void performSearch(String search) {
        FetchTracksTask task = new FetchTracksTask();
        Log.d("", "performing search: " + search);
        task.execute(search);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //if (savedInstanceState != null) {}

        // Add this line in order for this fragment to handle menu events.
        //setHasOptionsMenu(true);
        trackItems = new ArrayList<>();

        Bundle bundle = getArguments();
        if(bundle != null) {
            final String artist = bundle.getString("artist");
            Log.d("top 10: ", artist);
            performSearch(artist);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mToptenAdapter = new TopTenListViewAdapter(getActivity(), R.layout.topten_item_list, trackItems);

        View rootView = inflater.inflate(R.layout.fragment_topten_search, container, false);

        ListView listView = (ListView) rootView.findViewById(R.id.listview_tracks);
        listView.setAdapter(mToptenAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                //TODO: save selected artist data here, artist name, album, track, artist image paths to play selection

                String track = mToptenAdapter.getItem(position).getTrack();
                Toast.makeText(getActivity(), track, Toast.LENGTH_SHORT).show();
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

            SpotifyApi api = new SpotifyApi();
            SpotifyService spotify = api.getService();

            final TracksPager tracksPager = spotify.searchTracks(params[0]);
            return tracksPager.tracks.items;
        }

        @Override
        protected void onPostExecute(List<Track> result) {
            if (result != null) {
                if (result.size() == 0)
                    Toast.makeText(getActivity(), getResources().getString(R.string.no_artists_found), Toast.LENGTH_SHORT).show();
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

                        trackItems.add(new TrackItem(t.album.name, t.name, url_small_image, url_large_image));
                    }
                }
            }
        }
    }
}
