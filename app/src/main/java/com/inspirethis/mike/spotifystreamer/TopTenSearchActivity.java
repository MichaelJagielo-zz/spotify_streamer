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
        String artist = extras.getString("artist");


        if (savedInstanceState == null) {
            Bundle arguments = new Bundle();
            arguments.putString("artist", artist);
            FragmentTransaction ft = getFragmentManager().beginTransaction();

            TopTenSearchFragment topTenSearchFragment = new TopTenSearchFragment();
            topTenSearchFragment.setArguments(arguments);
            ft.add(R.id.top_ten_container, topTenSearchFragment, "TopTenSearchFragment");
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.commit();
        }
    }

//    @Override TODO: tie menus to MainActivity
//    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//        menu.clear();
//        menu.add(getResources().getString(R.string.menu_item_1));
//        super.onCreateOptionsMenu(menu, inflater);
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case 0:
//                //stopMusicService();
//                System.exit(0);
//                this.finish();
//                return true;
//            default:
//                return super.onOptionsItemSelected(item);
//        }
//    }

}
