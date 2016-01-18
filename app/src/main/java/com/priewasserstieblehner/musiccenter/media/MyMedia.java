package com.priewasserstieblehner.musiccenter.media;

import android.net.Uri;

import java.io.Serializable;

/**
 * Created by jakob_000 on 19.12.2015.
 */
public class MyMedia implements Serializable {
    private long id;
    private String title;
    private String uri;
    private MediaType type;

    public MyMedia(MediaType type, long id, String title, Uri uri) {
        this(type, id, title);
        this.uri = uri.toString();
    }

    public MyMedia(MediaType type, long id, String title) {
        this.type = type;
        this.id = id;
        this.title = title;
    }

    public void setUri(Uri uri) { this.uri = uri.toString(); }

    public long getId() { return id; }

    public String getTitle() { return title; }

    public Uri getUri() { return Uri.parse(uri); }

    public MediaType getType() { return type; }

}

