package com.leaves.app.shareme.bean;

import java.io.Serializable;

/**
 * Created by Leaves on 2016/11/13.
 */

public class Media implements Serializable {
    public static final int VIDEO = 0;
    public static final int AUDIO = 1;
    private String image;
    private String src;
    private String title;
    private long duration;
    private long albumId;
    private String artist;
    private int type;


    public Media(String image, String src, String title, long duration, String artist, int type) {
        this.image = image;
        this.src = src;
        this.title = title;
        this.duration = duration;
        this.artist = artist;
        this.type = type;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getAlbumId() {
        return albumId;
    }

    public void setAlbumId(long albumId) {
        this.albumId = albumId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
