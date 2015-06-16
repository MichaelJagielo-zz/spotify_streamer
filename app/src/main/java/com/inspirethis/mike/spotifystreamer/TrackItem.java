package com.inspirethis.mike.spotifystreamer;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by mike on 6/15/15.
 */
public class TrackItem implements Parcelable {
    private String spotifyId;
    private String name;
    private String album;
    private String track;
    private String image_path_small;
    private String image_path_large;

    public TrackItem(String spotifyId, String name, String album, String track, String image_path_small, String image_path_large) {
        this.spotifyId = spotifyId;
        this.name = name;
        this.album = album;
        this.track = track;
        this.image_path_small = image_path_small;
        this.image_path_large = image_path_large;
    }

    public String getSpotifyId() {
        return spotifyId;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getAlbum() {
        return album;
    }
    public void setAlbum(String spotifyId) {
        this.album = album;
    }
    public String getImage_path_small() {
        return image_path_small;
    }
    public String getImage_path_large() {
        return image_path_large;
    }
    public void setImage_path_small(String image_path_small) {
        this.image_path_small = image_path_small;
    }
    public void setImage_path_large(String image_path_large) {
        this.image_path_large = image_path_large;
    }
    public String getTrack() {
        return track;
    }
    public void setTrack(String track) {
        this.track = track;
    }
    @Override
    public String toString() {
        return track + "\n" + image_path_small;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
//        dest.writeInt(map.size());
//        for (String key : map.keySet()) {
//            dest.writeString(key);
//            dest.writeString(map.get(key));
//        }
        dest.writeString(spotifyId);
        dest.writeString(name);
        dest.writeString(album);
        dest.writeString(track);
        dest.writeString(image_path_small);
        dest.writeString(image_path_large);
    }

    private void readFromParcel(Parcel in ) {
//        int size = in.readInt();
//        for(int i = 0; i < size; i++) {
//            String key = in.readString();
//            String value = in.readString();
//            map.put(key, value);
//        }
        spotifyId = in.readString();
        name = in.readString();
        album = in.readString();
        track = in.readString();
        image_path_small = in.readString();
        image_path_large = in.readString();

    }

    @Override
    public int describeContents() {
        return 0;
    }
}
