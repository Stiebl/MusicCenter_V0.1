package com.priewasserstieblehner.musiccenter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.ContextMenu;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

import android.net.Uri;
import android.content.ContentResolver;
import android.database.Cursor;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.os.IBinder;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.widget.SeekBar;
import android.widget.TextView;

import com.priewasserstieblehner.musiccenter.media.Playlist;
import com.priewasserstieblehner.musiccenter.media.PlaylistAdapter;
import com.priewasserstieblehner.musiccenter.playerhandling.MusicService;
import com.priewasserstieblehner.musiccenter.playerhandling.MusicService.*;
import com.priewasserstieblehner.musiccenter.media.Album;
import com.priewasserstieblehner.musiccenter.media.AlbumAdapter;
import com.priewasserstieblehner.musiccenter.media.Artist;
import com.priewasserstieblehner.musiccenter.media.ArtistAdapter;
import com.priewasserstieblehner.musiccenter.media.MediaAdapter;
import com.priewasserstieblehner.musiccenter.media.MyMedia;
import com.priewasserstieblehner.musiccenter.media.Song;
import com.priewasserstieblehner.musiccenter.media.Video;
import com.priewasserstieblehner.musiccenter.playerhandling.OverlayAct;

// Anleitung unter: http://code.tutsplus.com/tutorials/create-a-music-player-on-android-project-setup--mobile-22764


public class MusicCenter extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    //Intent searchIntent = new Intent(this, SearchMusic.class);
    //Intent playOffIntent = new Intent(this, PlayOffline.class);

    // actual mode
    private Mode mode;
    // Maps for songs, artists, Album
    private SortedMap<String, Song> songs;
    private SortedMap<String, Artist> artists;
    private SortedMap<String, Album> albums;
    private SortedMap<String, Video> videos;
    private SortedMap<String, Playlist> playLists;

    // offline songliste
    private ArrayList<MyMedia> playList;
    private ArrayList<MyMedia> newPlayList;
    private ListView listView;

    // history
    //private ArrayList<String> history;

    // current playlist
    String currPlaylist;


    // player wiedergabe service
    private MusicService musicSrv;
    private Intent playIntent;
    private boolean musicBound=false;
    private ImageButton playBtn;
    private SeekBar seekBar;
    private TextView curTime;
    private TextView endTime;
    private boolean dragging;

    private int backCount;

    private static final int SHOW_PROGRESS = 0;
    private NavigationView navigationView;
    private MenuItem all;
    private DrawerLayout drawer;

    // YouTube
    private YouTubeConnector youTubeConnector;
    private Handler handler;
    private OverlayAct overlay;

    // forward usw
    //private MusicController controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_music_center);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        all = navigationView.getMenu().getItem(0);
        all.setChecked(true);
        // offline player implementation
        listView = (ListView)findViewById(R.id.listing);
        songs = new TreeMap<String, Song>();
        artists = new TreeMap<String, Artist>();
        albums = new TreeMap<String, Album>();
        videos = new TreeMap<String, Video>();


        File file = new File(getFilesDir(), "musiccenterplaylists");

        try {
            FileInputStream f = new FileInputStream(file);
            ObjectInputStream s = new ObjectInputStream(f);
            playLists = (TreeMap<String,Playlist>)s.readObject();
            s.close();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (playLists == null) {
            playLists = new TreeMap<String, Playlist>();
        }



        //playList = new ArrayList<Song>();
        getSongList();
        getVideoList();

        mode = Mode.ALL_MEDIA;
        playList = null;
        playList = new ArrayList<MyMedia>();
        for(Song s : songs.values()) playList.add(s);
        for(Video v : videos.values()) playList.add(v);
        setMediaList();

        setController();

        listView.setLongClickable(true);

        registerForContextMenu(listView);

        // init Youtube
        youTubeConnector = new YouTubeConnector(this);
        handler = new Handler();

        backCount = 0;
    }

    // service mit app mitstarten
    @Override
    protected void onStart() {
        super.onStart();
        //addToHistory("onStart called");
        // set focus back to main app
        findViewById(R.id.drawer_layout).requestFocus();
        if(playIntent==null){
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
        mHandler.sendEmptyMessageDelayed(SHOW_PROGRESS, 500);
    }

    //connect to the service
    private ServiceConnection musicConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicBinder binder = (MusicBinder)service;
            //get service
            musicSrv = binder.getService();
            musicSrv.setMeciaCenter(MusicCenter.this);
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    private void setController() {
        // Init Media controll
        playBtn = (ImageButton) findViewById(R.id.btn_play);
        seekBar = (SeekBar) findViewById(R.id.progress);
        seekBar.setMax(1000);
        seekBar.setEnabled(true);
        seekBar.setIndeterminate(false);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(!fromUser) return;
                if(musicSrv==null || !musicBound || musicSrv.isStopped()) {
                    seekBar.setProgress(0);
                    return;
                }
                long duration = getDuration();
                long newpos = (duration * progress) / 1000L;
                seekTo( (int) newpos);
                setTime(newpos, duration);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                dragging = true;

                mHandler.removeMessages(SHOW_PROGRESS);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                dragging = false;
                updatePlayBtn();
            }
        });
        curTime = (TextView) findViewById(R.id.curTime);
        endTime = (TextView) findViewById(R.id.endTime);
    }

    @Override
    public void onBackPressed() {
        //addToHistory("Back pressed");
        if(drawer.isDrawerOpen(navigationView)) {
            drawer.closeDrawer(navigationView);
            backCount = 0;
            return;
        }
        Log.i("Back pressed in mode", mode.name());
        switch(mode) {
            case ALL_MEDIA:
                backCount++;
                if(backCount >= 2) super.onBackPressed();
                break;
            case SONGS:
            case ARTISTS:
            case ALBUMS:
            case VIDEO:
            case SEARCH:
                all = navigationView.getMenu().getItem(0);
                onNavigationItemSelected(all);
                //navigationView.setCheckedItem(0);
                all.setChecked(true);
                //navigationSelected(R.id.nav_all);
                backCount = 0;
                break;

            case ARTIST_SONGS:
                navigationSelected(R.id.nav_artists);
                backCount = 0;
                break;

            case ALBUM_SONGS:
                navigationSelected(R.id.nav_albums);
                backCount = 0;
                break;

            case PLAYLIST_SONGS:
                navigationSelected(R.id.nav_playlists);
                backCount = 0;
                break;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.music_center, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_shuffle:
                //shuffle
                musicSrv.setShuffle();
                break;
            case R.id.action_end:
                finish();
                break;
            case R.id.action_settings:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean navigationSelected(int id) {
        switch(id) {
            case R.id.nav_all:
                playList = new ArrayList<MyMedia>();
                for(Song s : songs.values()) playList.add(s);
                for(Video v : videos.values()) playList.add(v);
                setMediaList();
                mode = Mode.ALL_MEDIA;
                break;
            case R.id.nav_songs:
                playList = new ArrayList<MyMedia>();
                for(Song s : songs.values()) playList.add(s);
                setMediaList();
                mode = Mode.SONGS;
                break;
            case R.id.nav_artists:
                ArrayList<Artist> artList = new ArrayList<Artist>();
                for(Artist a : artists.values()) artList.add(a);
                setArtistList(artList);
                mode = Mode.ARTISTS;
                break;
            case R.id.nav_albums:
                ArrayList<Album> albumList = new ArrayList<Album>();
                for(Album a : albums.values()) albumList.add(a);
                setAlbumList(albumList);
                mode = Mode.ALBUMS;
                break;
            case R.id.nav_videos:
                playList = new ArrayList<MyMedia>();
                for(Video v : videos.values()) playList.add(v);
                setMediaList();
                mode = Mode.VIDEO;
                break;
            case R.id.nav_playlists:
                ArrayList<Playlist> playlistList = new ArrayList<Playlist>();

                for(Playlist a : playLists.values()) {
                    playlistList.add(a);
                }
                setPlaylistList(playlistList);
                mode = Mode.PLAYLISTS;
                break;
            case R.id.nav_manage:
            case R.id.nav_send:
                break;
            default:
                Log.i("Error on navigation", Integer.toString(id));
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        return navigationSelected(id);
    }

    private void getSongList() {
        //retrieve song info
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] proj = new String[] {MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media._ID,
                                        MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM};
        Cursor musicCursor = musicResolver.query(musicUri, proj, null, null, null);

        if(musicCursor!=null && musicCursor.moveToFirst()){
            //get columns
            //int isNotific = musicCursor.getColumnIndex((MediaStore.Audio.Media.IS_NOTIFICATION));
            int titleColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.ARTIST);
            int albumColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.ALBUM);
            //add songs to list
            do {
                //if(musicCursor.getInt(isNotific) == 0) {
                    long thisId = musicCursor.getLong(idColumn);
                    String thisTitle = musicCursor.getString(titleColumn);
                    Artist thisArtist = putArtist(musicCursor.getString(artistColumn));
                    Album thisAlbum = putAlbum(musicCursor.getString(albumColumn));
                    Song thisSong = new Song(thisId, thisTitle, thisArtist, thisAlbum);
                    songs.put(thisTitle, thisSong);
                //}
            } while (musicCursor.moveToNext());

            // add album art to albums
            Uri albumUri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
            proj = new String[] {MediaStore.Audio.Albums.ALBUM, MediaStore.Audio.Albums.ALBUM_ART};
            Cursor albumCursor = musicResolver.query(albumUri, proj, null, null, null);
            if(albumCursor != null && albumCursor.moveToFirst()) {
                int albumName = albumCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM);
                int albumArt = albumCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART);

                do {
                    String thisAlbum = albumCursor.getString(albumName);
                    String thisAlbumArt = albumCursor.getString(albumArt);
                    Album albObj = albums.get(thisAlbum);
                    if(albObj != null) albObj.addAlbumArt(thisAlbumArt);
                } while(albumCursor.moveToNext());
            }
        }
    }

    private void getVideoList() {
        //retrieve song info
        ContentResolver videoResolver = getContentResolver();
        Uri videoURI = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] proj = new String[] {MediaStore.Video.Media.TITLE, MediaStore.Video.Media._ID};
        Cursor videoCursor = videoResolver.query(videoURI, proj, null, null, null);

        if(videoCursor!=null && videoCursor.moveToFirst()){
            //get columns
            //int isNotific = musicCursor.getColumnIndex((MediaStore.Audio.Media.IS_NOTIFICATION));
            int titleColumn = videoCursor.getColumnIndex
                    (MediaStore.Video.Media.TITLE);
            int idColumn = videoCursor.getColumnIndex
                    (MediaStore.Video.Media._ID);

            //add videos to list
            do {
                //if(musicCursor.getInt(isNotific) == 0) {
                long thisId = videoCursor.getLong(idColumn);
                String thisTitle = videoCursor.getString(titleColumn);
                Video thisVideo = new Video(thisId, thisTitle);

                videos.put(thisTitle, thisVideo);
                //}
            } while (videoCursor.moveToNext());
        }
    }

    public Artist putArtist(String artist) {
        if(artists.containsKey(artist)) return artists.get(artist);
        Artist newArtist = new Artist(artist);
        artists.put(artist, newArtist);
        return newArtist;
    }

    public Album putAlbum(String album) {
        if(albums.containsKey(album)) return albums.get(album);
        Album newAlbum = new Album(album);
        albums.put(album, newAlbum);
        return newAlbum;
    }


    public void doSearch(View view) {
        newPlayList = new ArrayList<MyMedia>();
        EditText searchTxt = (EditText) findViewById(R.id.search_src_text);
        String searchStr = searchTxt.getText().toString();
        for(String str : songs.keySet()) {
            if(str.toLowerCase().contains(searchStr.toLowerCase())) {
                newPlayList.add(songs.get(str));
                Log.i("offline Match", str);
            }
        }
        searchOnYoutube(searchStr);

    }

    private void searchOnYoutube(final String keywords){
        new Thread(){
            public void run(){
                final ArrayList<MyMedia> youTubeVideos = youTubeConnector.search(keywords);

                handler.post(new Runnable(){
                    public void run(){
                        if(youTubeVideos != null) for(MyMedia v : youTubeVideos) newPlayList.add(v);
                        mode = Mode.SEARCH;
                        setMediaList(newPlayList);
                    }
                });
            }
        }.start();
    }

    public void itemPicked(View view) {
        backCount = 0;
        switch (mode) {
            case ARTISTS:
                Artist artist = artists.get(view.getTag().toString());
                if(artist == null) return;
                setMediaList(artist.getMedias());
                mode = Mode.ARTIST_SONGS;
                break;
            case ALBUMS:
                Album album = albums.get(view.getTag().toString());
                if(album == null) return;
                setMediaList(album.getMedias());
                mode = Mode.ALBUM_SONGS;
                break;
            case PLAYLISTS:
                if (view.getTag().toString() != null) {
                    Log.i("MUSICCENTER", "chosen playlist: " + view.getTag().toString());
                    Playlist playlist = playLists.get(view.getTag().toString());
                    if(playlist == null) return;
                    setMediaList(playlist.getMedias());
                    mode = Mode.PLAYLIST_SONGS;
                    currPlaylist = view.getTag().toString();
                }
                break;
            default:
                try {
                    playMedia(Integer.parseInt(view.getTag().toString()));
                    //controller.show(0);
                }
                catch (NumberFormatException e) {
                    Log.i("Item picked error", view.getTag().toString());
                }
                break;
        }
    }

    public void start() {
        if(musicSrv.isStopped()) playMedia(0);
        else musicSrv.go();

        mHandler.sendEmptyMessageDelayed(SHOW_PROGRESS, 500);
    }

    public void pause() {
        musicSrv.pausePlayer();
    }

    public int getDuration() {
        if(musicSrv!=null && musicBound && !musicSrv.isStopped())
        return musicSrv.getDur();
        else return 0;
    }

    public int getCurrentPosition() {
        if(musicSrv!=null && musicBound && !musicSrv.isStopped())
        return musicSrv.getPosn();
        else return 0;
    }

    public void seekTo(int pos) {
        if(musicSrv!=null && musicBound && !musicSrv.isStopped()) musicSrv.seek(pos);
    }

    public boolean isPlaying() {
        if(musicSrv!=null && musicBound) return musicSrv.isPng();
        else return false;
    }

    private void playNext(){
        if(!musicSrv.isStopped()) musicSrv.playNext();
    }

    //play previous
    private void playPrev(){
        if(!musicSrv.isStopped()) musicSrv.playPrev();
    }


    private void setMediaList() {
        MediaAdapter mediaAdt = new MediaAdapter(this, playList);
        listView.setAdapter(mediaAdt);
    }

    private void setMediaList(ArrayList<MyMedia> medias) {
        playList = medias;
        setMediaList();
    }

    private void setArtistList(ArrayList<Artist> artists) {
        ArtistAdapter artistAdt = new ArtistAdapter(this, artists);
        listView.setAdapter(artistAdt);
    }

    private void setAlbumList(ArrayList<Album> albums) {
        AlbumAdapter albumAdt = new AlbumAdapter(this, albums);
        listView.setAdapter(albumAdt);
    }

    private void setPlaylistList(ArrayList<Playlist> playlists) {
        PlaylistAdapter playlistAdt = new PlaylistAdapter(this, playlists);
        listView.setAdapter(playlistAdt);
    }

    private void playMedia(int mediaId) {
        boolean wasPlaying = isPlaying();
        if(mediaId >= playList.size() || mediaId < 0 ) return;
        musicSrv.setMediaList(playList);
        musicSrv.setMedia(mediaId);
        musicSrv.playMedia();
        if(!wasPlaying) mHandler.sendEmptyMessageDelayed(SHOW_PROGRESS, 500);
    }

    public void play(View view) {
        backCount = 0;
        if(isPlaying()) {
            pause();
            updatePlayBtn();
        }
        else {
            start();
            updatePlayBtn();
        }
    }

    public void next(View view) {
        backCount = 0;
        playNext();
    }

    public void prev(View view) {
        backCount = 0;
        playPrev();
    }

    public void updatePlayBtn() {
        mHandler.removeMessages(SHOW_PROGRESS);
        int resId = android.R.drawable.ic_media_play;
        if(isPlaying()) {
            resId = android.R.drawable.ic_media_pause;
            mHandler.sendEmptyMessageDelayed(SHOW_PROGRESS, 500);
        }
        playBtn.setImageResource(resId);
        musicSrv.setPlayBtnImg(resId);
    }

    public Mode getMode() { return mode; }

    private int setProgress() {
        long prog = getCurrentPosition();
        long dur = getDuration();
        if(dur > 0) {
            long pos = 1000L * prog / dur;
            seekBar.setProgress((int) pos);
            setTime(prog, dur);
        }
        return (int) prog;
    }

    private void setTime(long prog, long dur) {

        prog /= 1000;
        dur /= 1000;
        curTime.setText(String.format("%02d:%02d", prog / 60, prog % 60));

        endTime.setText(String.format("%02d:%02d", dur / 60, dur % 60));
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int pos = setProgress();
            //Log.i("Progress set", Integer.toString(pos));
            if (!dragging && isPlaying()) {
                msg = obtainMessage(SHOW_PROGRESS);
                sendMessageDelayed(msg, 1000 - (pos % 1000));
            }
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        if(musicSrv != null) musicSrv.setMainVisible(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(musicSrv != null) musicSrv.setMainVisible(true);
    }


    // handle app closing
    @Override
    public void finish() {
        Log.i("MUSICCENTER", "finish called");
        unbindService(musicConnection);
        musicBound = false;
        stopService(playIntent);
        if(musicSrv != null) overlay = musicSrv.getOverlayAct();
        if(overlay != null) {
            overlay.moveTaskToBack(true);
            overlay.finish();
        }
        playIntent = null;
        musicSrv = null;
        super.finish();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        Log.i("MUSICCENTER", "oncreatecontextmenu reached!");

        if (mode == Mode.ALBUM_SONGS || mode == Mode.ARTIST_SONGS || mode == Mode.SONGS || mode == Mode.ALL_MEDIA || mode == Mode.SEARCH || mode == Mode.VIDEO) {
            menu.add(Menu.NONE, 0, 0, "New Playlist");

            for (Playlist currplay : playLists.values()) {
                menu.add(currplay.toString());
            }
        } else if (mode == Mode.PLAYLIST_SONGS) {
            menu.add(Menu.NONE, 0, 0, "Remove from Playlist");
        } else if (mode == Mode.PLAYLISTS) {
            menu.add(Menu.NONE, 0, 0, "Remove Playlist");
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Log.i("MUSICCENTER", "item selected: " + String.valueOf(item.getItemId()));

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int index = info.position;
        final View view = info.targetView;

        if (item.toString() == "New Playlist") {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Playlists");

            final EditText input = new EditText(this);

            input.setInputType(InputType.TYPE_CLASS_TEXT);

            builder.setView(input);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Log.i("MUSICCENTER", "put to place: " + Integer.toString(playLists.size()));
                    if (input.getText().toString() != "Remove from Playlist" && input.getText().toString() != "New Playlist" && input.getText().toString() != "Remove Playlist") {
                        playLists.put(input.getText().toString(), new Playlist(input.getText().toString()));
                        playLists.get(input.getText().toString()).addMedia(playList.get(Integer.parseInt(view.getTag().toString())));
                    } else {
                        //show error?
                    }


                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            builder.show();


        } else if (item.toString() == "Remove from Playlist") {


            Log.i("MUSICCENTER", "remove song: " + playList.get(Integer.parseInt(view.getTag().toString())).getTitle() + " from playlist: " + currPlaylist);

            if (playLists.get(currPlaylist) != null && currPlaylist != null) {
                playLists.get(currPlaylist).removeMedia(playList.get(Integer.parseInt(view.getTag().toString())));
                setMediaList(playLists.get(currPlaylist).getMedias());
            }


        } else if (item.toString() == "Remove Playlist") {
            Log.i("MUSICCENTER", "remove playlist: " + view.getTag().toString());
            playLists.remove(view.getTag().toString());

            ArrayList<Playlist> playlistList = new ArrayList<Playlist>();
            for(Playlist a : playLists.values()) playlistList.add(a);
            setPlaylistList(playlistList);

        } else {
            //Log.i("MUSICCENTER", "song to put into playlist: " + playList.get(Integer.parseInt(view.getTag().toString())).getTitle() + " into playlist: " + item.toString());

            if (playLists.get(item.toString()) != null) {
                playLists.get(item.toString()).addMedia(playList.get(Integer.parseInt(view.getTag().toString())));
            }
        }


        File file = new File(getFilesDir(), "musiccenterplaylists");

        FileOutputStream f = null;
        try {
            f = new FileOutputStream(file);
            ObjectOutputStream s = new ObjectOutputStream(f);
            s.writeObject(playLists);
            s.flush();
            s.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }




}
