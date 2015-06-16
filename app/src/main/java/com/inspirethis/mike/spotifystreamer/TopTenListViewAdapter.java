package com.inspirethis.mike.spotifystreamer;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Created by mike on 6/15/15.
 */
public class TopTenListViewAdapter extends ArrayAdapter<TrackItem> {

    Context context;

    public TopTenListViewAdapter(Context context, int resourceId,
                                 List<TrackItem> items) {
        super(context, resourceId, items);
        this.context = context;
    }

    /*private view holder class*/
    private class ViewHolder {
        ImageView imageView;
        TextView albumTitle;
        TextView trackTitle;

    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        TrackItem trackItem = getItem(position);

        LayoutInflater mInflater = (LayoutInflater) context
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.topten_item_list, null);
            holder = new ViewHolder();

            holder.albumTitle = (TextView) convertView.findViewById(R.id.album);
            holder.trackTitle = (TextView) convertView.findViewById(R.id.track);
            holder.imageView = (ImageView) convertView.findViewById(R.id.list_image);
            convertView.setTag(holder);
        } else
            holder = (ViewHolder) convertView.getTag();

        holder.albumTitle.setText(trackItem.getAlbum());
        holder.trackTitle.setText(trackItem.getTrack());

        if (trackItem.getImage_path_small() != null && !trackItem.getImage_path_small().toString().equals(""))
            Picasso.with(context).load(trackItem.getImage_path_small()).into(holder.imageView);
        else
            holder.imageView.setImageResource(R.mipmap.greyscale_thumb);

        return convertView;
    }
}
