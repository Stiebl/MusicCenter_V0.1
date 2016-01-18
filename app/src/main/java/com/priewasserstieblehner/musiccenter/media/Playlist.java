package com.priewasserstieblehner.musiccenter.media;

import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by mjoedpc on 06.01.2016.
 */
public class Playlist implements Serializable {

    private ArrayList<MyMedia> myMedias;
    private String name;

    public Playlist(String name) {
        this.name = name;
        myMedias = new ArrayList<MyMedia>();
    }

    public void addMedia(MyMedia media) {
        if (!myMedias.contains(media)) {
            myMedias.add(media);
        }
    }

    public String toString() {
        return name;
    }

    public ArrayList<MyMedia> getMedias(){
        return myMedias;
    }

    public void removeMedia(MyMedia myMedia) {
        Log.i("MUSICCENTER_PLAYLIST", "remove song: " + myMedia.getTitle() + " from this playlist(" + name);
        if (myMedias.contains(myMedia)) {
            Log.i("MUSICCENTER_PLAYLIST", "removing!");
            myMedias.remove(myMedia);
        }
        Log.i("MUSICCENTER_PLAYLIST", "removed! successful: " + !myMedias.contains(myMedia));
    }
}
