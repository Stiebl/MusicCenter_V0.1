package com.priewasserstieblehner.musiccenter.media;

import android.content.ContentUris;
import android.graphics.drawable.Drawable;
import android.net.Uri;

/**
 * Created by jakob_000 on 21.10.2015.
 */
public class Song extends MyMedia {
    private Artist artist;
    private Album album;

    public Song(long songID, String songTitle, Uri songUri, Artist songArtist, Album songAlbum) {
        super(MediaType.SONG.SONG, songID, songTitle, songUri);
        artist = songArtist;
        album = songAlbum;
        if(artist != null) artist.addSong(this);
        if(album != null) album.addSong(this);
    }

    public Song(long songId, String songTitle, Artist songArtist, Album songAlbum) {
        super(MediaType.SONG, songId, songTitle);
        Uri songUri = ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                songId);
        setUri(songUri);
        artist = songArtist;
        album = songAlbum;
        if(artist != null) artist.addSong(this);
        if(album != null) album.addSong(this);
    }

    public String getArtist(){return artist != null ? artist.toString() : "unknown Artist";}
    public String getAlbum(){return album != null ? album.toString() : "unknown Album";}
    public Drawable getAlbumArt() {return album != null ? album.getAlbumArt() : null;}
}
