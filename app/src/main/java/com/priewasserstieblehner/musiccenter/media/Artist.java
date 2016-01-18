package com.priewasserstieblehner.musiccenter.media;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by jakob_000 on 22.10.2015.
 */
public class Artist implements Serializable {
    private ArrayList<MyMedia> medias;
    private String artist;

    public Artist(String artist){
        this.artist = artist;
        medias = new ArrayList<MyMedia>();
    }

    public void addSong(Song song) {
        if(!medias.contains(song)) medias.add(song);
    }

    public String toString(){return artist;}

    public ArrayList<MyMedia> getMedias(){
        Collections.sort(medias, new Comparator<MyMedia>() {
            public int compare(MyMedia a, MyMedia b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });
        return medias;
    }
}
