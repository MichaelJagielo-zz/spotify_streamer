package com.inspirethis.mike.spotifystreamer;

/**
 * ArtistItem is list item object for displaying Artists in ArtistListViewAdapter
 * Created by mike on 6/12/15.
 */
public class ArtistItem {
    private String spotifyId;
    private String name;
    private String image_path;

    public ArtistItem(String spotifyId, String name, String image_path) {
        this.spotifyId = spotifyId;
        this.name = name;
        this.image_path = image_path;
    }
    public String getSpotifyId() {
        return spotifyId;
    }
    public void setSpotifyId(String spotifyId) {
        this.spotifyId = spotifyId;
    }
    public String getImage_path() {
        return image_path;
    }
    public void setImage_path(String image_path) {
        this.image_path = image_path;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    @Override
    public String toString() {
        return name + "\n" + image_path;
    }
}
