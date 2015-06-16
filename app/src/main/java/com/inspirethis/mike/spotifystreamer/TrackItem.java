package com.inspirethis.mike.spotifystreamer;

/**
 * Created by mike on 6/15/15.
 */
public class TrackItem {
    private String album;
    private String track;
    private String image_path_small;
    private String image_path_large;

    public TrackItem(String album, String track, String image_path_small, String image_path_large) {
        this.album = album;
        this.track = track;
        this.image_path_small = image_path_small;
        this.image_path_large = image_path_large;
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
}
