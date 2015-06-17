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
 * ArtistListViewAdapter is custom ArrayAdapter for displaying the list of ArtistS query
 * Created by mike on 6/12/15.
 */
public class ArtistListViewAdapter extends ArrayAdapter<ArtistItem> {

    Context context;

    public ArtistListViewAdapter(Context context, int resourceId,
                                 List<ArtistItem> items) {
        super(context, resourceId, items);
        this.context = context;
    }

    private class ViewHolder {
        ImageView imageView;
        TextView txtTitle;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        ArtistItem artistItem = getItem(position);

        LayoutInflater mInflater = (LayoutInflater) context
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.artist_list_item, null);
            holder = new ViewHolder();

            holder.txtTitle = (TextView) convertView.findViewById(R.id.artist);
            holder.imageView = (ImageView) convertView.findViewById(R.id.list_image);
            convertView.setTag(holder);
        } else
            holder = (ViewHolder) convertView.getTag();

        holder.txtTitle.setText(artistItem.getName());

        if (artistItem.getImage_path() != null && !artistItem.getImage_path().equals(""))
            Picasso.with(context).load(artistItem.getImage_path()).into(holder.imageView);
        else
            holder.imageView.setImageResource(R.mipmap.greyscale_thumb);

        return convertView;
    }
}