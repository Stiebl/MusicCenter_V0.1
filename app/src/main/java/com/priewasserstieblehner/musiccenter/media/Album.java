package com.priewasserstieblehner.musiccenter.media;

import android.graphics.drawable.Drawable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by jakob_000 on 22.10.2015.
 */
public class Album implements Serializable {

    private ArrayList<MyMedia> myMedias;
    private String album;
    private String albumArtPath;
    private Drawable albumArt;

    public Album(String album) {
        this.album = album;
        myMedias = new ArrayList<MyMedia>();
    }

    public void addAlbumArt(String albumArtPath) {
        this.albumArtPath = albumArtPath;
        albumArt = Drawable.createFromPath(albumArtPath);
    }

    public Drawable getAlbumArt() {return albumArt;}

    public void addSong(Song song) {
        if(!myMedias.contains(song)) myMedias.add(song);
    }

    public String toString(){return album;}
    public ArrayList<MyMedia> getMedias(){
        Collections.sort(myMedias, new Comparator<MyMedia>() {
            public int compare(MyMedia a, MyMedia b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });
        return myMedias;
    }
}
