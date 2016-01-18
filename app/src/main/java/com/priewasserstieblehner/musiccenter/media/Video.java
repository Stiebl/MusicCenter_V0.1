package com.priewasserstieblehner.musiccenter.media;

import android.content.ContentUris;
import android.net.Uri;

/**
 * Created by jakob_000 on 19.12.2015.
 */
public class Video extends MyMedia{

    public Video(long id, String title) {
        super(MediaType.VIDEO, id, title);
        Uri videoURI = ContentUris.withAppendedId(
                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                id);
        setUri(videoURI);
    }

    public Video(long id, String title, Uri uri) {
        super(MediaType.VIDEO, id, title, uri);
    }
}
