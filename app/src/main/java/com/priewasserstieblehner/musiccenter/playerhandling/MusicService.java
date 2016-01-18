package com.priewasserstieblehner.musiccenter.playerhandling;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.Nullable;
import java.util.ArrayList;
import android.media.AudioManager;
import android.os.Binder;
import android.os.PowerManager;
import android.util.Log;
import java.util.Random;
import android.app.Notification;
import android.app.PendingIntent;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.VideoView;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;
import com.priewasserstieblehner.musiccenter.MusicCenter;
import com.priewasserstieblehner.musiccenter.R;
import com.priewasserstieblehner.musiccenter.media.MediaType;
import com.priewasserstieblehner.musiccenter.media.MyMedia;
import com.priewasserstieblehner.musiccenter.media.Song;
import com.priewasserstieblehner.musiccenter.media.YouTubeVideo;

/**
 * Created by jakob_000 on 22.10.2015.
 */
public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {

    private boolean overlayVisible;
    private boolean mainVisible;

    private enum YouTubeState {
        PLAYING, PAUSED, STOPPED, BUFFERING;
    }

    private YouTubeState yTState;

    //media player
    private MediaPlayer player;
    //song list
    private ArrayList<MyMedia> medias;
    //current position
    private int mediaPos;

    // binder class for application
    private final IBinder musicBind = new MusicBinder();

    // Notivication utilities
    private Notification.Builder builder;

    // notification information
    private String mediaTitle ="";
    private static final int NOTIFY_ID=1;

    // shuffle and random
    private boolean shuffle=false;
    private Random rand;
    private MusicCenter musicCenter;
    private boolean isStopped = true;
    private PendingIntent pendInt;
    //private MusicController musicController;
    private WindowManager windowManager;
    private WindowManager.LayoutParams olParams;
    private View overlayView;

    private ImageButton playBtn;
    private ImageView albumArt;

    // Video Playback in overlay
    private VideoView videoView;

    private View btn_ol;

    private MyMedia currMedia;
    private int curVideoPos;


    private YouTubePlayerView youTubePlayerView;
    private YouTubePlayer youTubePlayer;

    private static MusicService instance;

    private boolean overlayInitialized;
    private OverlayAct overlayAct;

    public void onCreate(){
        //create the service
        super.onCreate();

        Log.i("SERVICE", "onCreate called");
        instance = this;
        //initialize position
        mediaPos =0;

        overlayInitialized = false;
        //create player
        player = new MediaPlayer();
        initMusicPlayer();

        Intent notIntent = new Intent(this, MusicCenter.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        pendInt = PendingIntent.getActivity(this, 0,
                notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        builder = new Notification.Builder(this);

        rand=new Random();

        yTState = YouTubeState.STOPPED;
    }

    private void initMusicPlayer(){
        //set player properties
        player.setWakeMode(getApplicationContext(),
                PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
    }

    private void initOverlay() {
        if(musicCenter == null || overlayInitialized) return;
        Intent overL = new Intent(this, OverlayAct.class);
        overL.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(overL);
    }

    public synchronized void setMediaList(ArrayList<MyMedia> theMedia){
        medias =theMedia;
    }

    public void setMeciaCenter(final MusicCenter musicCenter) {
        this.musicCenter = musicCenter;
    }

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return musicBind;
    }

    @Override
    public boolean onUnbind(Intent intent){
        player.stop();
        player.release();
        // remove system overlay
        if(overlayInitialized && overlayVisible) windowManager.removeView(overlayView);
        overlayInitialized = false;
        return false;
    }

    public void setPlayBtnImg(int resId) {
        if(playBtn != null) playBtn.setImageResource(resId);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        switch(currMedia.getType()) {
            case SONG:
                mp.reset();
                if (mediaPos < medias.size() - 1 || shuffle) playNext();
                else {
                    mp.stop();
                    isStopped = true;
                    setNotification("Playlist ended", mediaTitle, android.R.drawable.ic_menu_more);
                    //mp.reset();
                }
                break;
            case VIDEO:
                if(mediaPos < medias.size() - 1 || shuffle) playNext();
                else {
                    videoView.stopPlayback();
                    isStopped = true;
                    setNotification("Playlist ended", mediaTitle, android.R.drawable.ic_menu_more);
                }
                break;
        }

        musicCenter.updatePlayBtn();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        isStopped = true;
        switch (currMedia.getType()) {
            case SONG: mp.reset(); break;
            case VIDEO: videoView.stopPlayback(); break;
        }
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        switch (currMedia.getType()) {
            case SONG:
                mp.start();
                //videoView.pause();
                if (albumArt != null && currMedia != null) {
                    Drawable artDrawable = ((Song)currMedia).getAlbumArt();
                    if (artDrawable != null) albumArt.setImageDrawable(artDrawable);
                    else albumArt.setImageResource(android.R.drawable.ic_menu_gallery);
                }
                break;
            case VIDEO:
                videoView.start();
                //player.pause();
                break;
        }
        isStopped = false;
        setNotification("Playing", mediaTitle, android.R.drawable.ic_media_play);
        musicCenter.updatePlayBtn();

        //musicController.show(0);
    }


    public void playMedia(){
        MyMedia oldCurr = currMedia;
        currMedia = medias.get(mediaPos);
        mediaTitle = currMedia.getTitle();
        if(oldCurr != null && currMedia != null  && oldCurr.getType() != currMedia.getType()) {
            player.pause();
            videoView.pause();
            if(yTState != YouTubeState.STOPPED && youTubePlayer != null) {
                yTState = YouTubeState.STOPPED;
                youTubePlayer.release();
                youTubePlayer = null;
            }
        }
        setAlbumArtOrVideo();
        switch(currMedia.getType()) {
            case SONG:
                player.reset();
                //set uri
                Uri trackUri = currMedia.getUri();
                try {
                    player.setDataSource(getApplicationContext(), trackUri);
                } catch (Exception e) {
                    Log.e("MUSIC SERVICE", "Error setting data source", e);
                }
                player.prepareAsync();
                break;
            case VIDEO:
                if(!overlayInitialized) initOverlay();
                else playVideo();
                break;
            case YOUTUBE:
                yTState = YouTubeState.PLAYING;
                if(!overlayInitialized) initOverlay();
                else playYoutubeVideo();
                break;
        }
    }

    private void playVideo() {
        videoView.setVideoURI(currMedia.getUri());
    }

    private void playYoutubeVideo() {
        if(youTubePlayer != null) {
            yTState = YouTubeState.STOPPED;
            youTubePlayer.release();
        }

        final YouTubeVideo video = (YouTubeVideo) currMedia;
        String key = overlayView.getResources().getString(R.string.app_key);
        youTubePlayerView.initialize(key, new YouTubePlayer.OnInitializedListener() {
            @Override
            public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean b) {
                MusicService.this.youTubePlayer = youTubePlayer;
                youTubePlayer.loadVideo(video.getYouTubeId());
                youTubePlayer.setShowFullscreenButton(false);
                youTubePlayer.setPlaybackEventListener(new YouTubePlayer.PlaybackEventListener() {
                    @Override
                    public void onPlaying() {
                        yTState = YouTubeState.PLAYING;
                        setNotification("Playing", mediaTitle, android.R.drawable.ic_media_play);
                        isStopped = false;
                        musicCenter.updatePlayBtn();
                    }

                    @Override
                    public void onPaused() {

                        yTState = YouTubeState.PAUSED;
                        setNotification("Paused", mediaTitle, android.R.drawable.ic_media_pause);
                    }

                    @Override
                    public void onStopped() {
                        Log.i("YOUTUBE", "Stopped");
                        yTState = YouTubeState.STOPPED;
                    }

                    @Override
                    public void onBuffering(boolean b) {
                        yTState = YouTubeState.BUFFERING;
                    }

                    @Override
                    public void onSeekTo(int i) {

                    }
                });
                isStopped = false;
            }

            @Override
            public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {
                Log.i("YOUTUBE", "Failed to initialize");
            }
        });
    }

    public void setMedia(int mediaIndex){
        mediaPos =mediaIndex;
    }

    public int getPosn(){
        switch(currMedia.getType()) {
            case SONG: return player.getCurrentPosition();
            case VIDEO: return videoView.getCurrentPosition();
            case YOUTUBE: return yTState != YouTubeState.STOPPED && youTubePlayer != null ? youTubePlayer.getCurrentTimeMillis() : 0;
            default: return 0;
        }
    }

    public int getDur(){
        int res = 0;
        if(currMedia == null) return 0;
        switch(currMedia.getType()) {
            case SONG: res = player.getDuration(); break;
            case VIDEO: res = videoView.getDuration(); break;
            case YOUTUBE: res = yTState != YouTubeState.STOPPED && youTubePlayer != null ? youTubePlayer.getDurationMillis() : 0; break;
        }
        return res;
    }

    public boolean isPng(){
        boolean res = false;
        if(currMedia == null) return false;
        switch(currMedia.getType()) {
            case SONG: res = player.isPlaying(); break;
            case VIDEO: res = videoView.isPlaying(); break;
            case YOUTUBE: res = yTState == YouTubeState.PLAYING || yTState == YouTubeState.BUFFERING; break;
        }
        return res;
    }

    public boolean isStopped() {
        return isStopped;
    }

    public void pausePlayer() {
        if(currMedia == null) return;
        switch (currMedia.getType()) {
            case SONG: player.pause(); break;
            case VIDEO:
                curVideoPos = videoView.getCurrentPosition();
                videoView.pause();
                break;
            case YOUTUBE:
                if(yTState != YouTubeState.STOPPED && youTubePlayer != null) {
                    youTubePlayer.pause();
                    yTState = YouTubeState.PAUSED;
                }
                break;
        }
        setNotification("Playlist paused", mediaTitle, android.R.drawable.ic_media_pause);
    }

    public void seek(int posn){
        if(currMedia == null) return;
        switch(currMedia.getType()) {
            case SONG: player.seekTo(posn); break;
            case VIDEO: videoView.seekTo(posn); break;
            case YOUTUBE:
                if(yTState != YouTubeState.STOPPED && youTubePlayer != null) youTubePlayer.seekToMillis(posn);
                break;
        }
    }

    public void go(){
        if(currMedia == null) return;
        setAlbumArtOrVideo();
        setNotification("Playing", mediaTitle, android.R.drawable.ic_media_play);
        switch(currMedia.getType()) {
            case SONG: player.start(); break;
            case VIDEO:
                videoView.resume();
                seek(curVideoPos);
                break;
            case YOUTUBE:
                if(yTState != YouTubeState.STOPPED && youTubePlayer != null) {
                    youTubePlayer.play();
                    yTState = YouTubeState.PLAYING;
                }
                break;
        }
    }

    public void playPrev(){
        mediaPos--;
        if(mediaPos <0) mediaPos = medias.size()-1;
        playMedia();
    }

    //skip to next
    public void playNext(){
        if(shuffle){
            int newSong = mediaPos;
            while(newSong== mediaPos && medias.size() > 1){
                newSong=rand.nextInt(medias.size());
            }
            mediaPos =newSong;
        }
        else{
            mediaPos++;
            if(mediaPos >= medias.size()) mediaPos =0;
        }
        playMedia();
    }

    public void setShuffle(){
        if(shuffle) shuffle=false;
        else shuffle=true;
    }

    @Override
    public void onDestroy() {
        if(player != null) {
            player.release();
        }
        if (videoView != null) videoView.stopPlayback();
        stopForeground(true);
        overlayInitialized = false;
    }

    private void setNotification(String title, String songTitle, int icon) {
        builder.setContentIntent(pendInt)
                .setSmallIcon(icon)
                .setTicker(songTitle)
                .setOngoing(true)
                .setContentTitle(title)
                .setContentText(songTitle);
        Notification not = builder.build();

        startForeground(NOTIFY_ID, not);
    }

    private void setAlbumArtOrVideo() {
        if(musicCenter == null) return;
        if(!overlayInitialized) {
            initOverlay(); //&& (currMedia == null || currMedia.getType() != MediaType.YOUTUBE)) initOverlay();
            return;
        }
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        switch(currMedia.getType()) {
            case SONG:
                albumArt.setVisibility(View.VISIBLE);
                videoView.setVisibility(View.GONE);
                youTubePlayerView.setVisibility(View.GONE);
                params.addRule(RelativeLayout.BELOW, R.id.cover_ol);
                break;
            case VIDEO:
                albumArt.setVisibility(View.GONE);
                videoView.setVisibility(View.VISIBLE);
                youTubePlayerView.setVisibility(View.GONE);
                params.addRule(RelativeLayout.BELOW, R.id.video_ol);
                btn_ol.setVisibility(View.VISIBLE);
                break;
            case YOUTUBE:
                albumArt.setVisibility(View.GONE);
                videoView.setVisibility(View.GONE);
                youTubePlayerView.setVisibility(View.VISIBLE);
                params.addRule(RelativeLayout.BELOW, R.id.youtube_ol);
                break;
        }

        btn_ol.setLayoutParams(params);
        if(mainVisible && (currMedia == null || currMedia.getType() == MediaType.SONG)) hideOverlay();
        else showOverlay();
    }

    public static MusicService getInstance() {return instance; }

    // initialize overlay variables
    public void activityInitialized(View overlayView, final OverlayAct overlayAct) {
        if(musicCenter == null || overlayInitialized) return;
        // create overlay
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // create view from layout
        this.overlayView = overlayView;

        // Setup layout parameter
        olParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        olParams.gravity = Gravity.TOP | Gravity.LEFT; // Orientation
        olParams.x = 0; // where you want to draw this, coordinates
        olParams.y = 0;

        // store important buttons
        playBtn = (ImageButton) this.overlayView.findViewById(R.id.btn_play_ol);

        // handle move of overlay on display
        this.overlayView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int xOffset = v.getWidth() / 2;
                int yOffset = v.getHeight() / 2;
                int xpos = (int) event.getRawX() - xOffset;
                int ypos = (int) event.getRawY() - yOffset;
                olParams.x = xpos;
                olParams.y = ypos;
                windowManager.updateViewLayout(MusicService.this.overlayView, olParams);
                return false;
            }
        });

        // handle video View
        albumArt = (ImageView) this.overlayView.findViewById(R.id.cover_ol);
        videoView = (VideoView) this.overlayView.findViewById(R.id.video_ol);
        youTubePlayerView = (YouTubePlayerView) this.overlayView.findViewById(R.id.youtube_ol);

        btn_ol = (View) this.overlayView.findViewById(R.id.music_btns_ol);

        // set buttonlisteners
        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                musicCenter.play(v);
            }
        });
        ImageButton prevBtn = (ImageButton) this.overlayView.findViewById(R.id.btn_prev_ol);
        prevBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                musicCenter.prev(v);
            }
        });
        ImageButton nextBtn = (ImageButton) this.overlayView.findViewById(R.id.btn_next_ol);
        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                musicCenter.next(v);
            }
        });

        videoView.setOnCompletionListener(this);
        videoView.setOnErrorListener(this);
        videoView.setOnPreparedListener(this);

        this.overlayAct = overlayAct;
        //windowManager.addView(overlayView, olParams);
        overlayInitialized = true;

        setAlbumArtOrVideo();
        if(currMedia != null) {
            switch(currMedia.getType()) {
                case VIDEO: playVideo(); break;
                case YOUTUBE: playYoutubeVideo(); break;
            }
        }

    }

    public OverlayAct getOverlayAct() { return overlayAct; }

    public void showOverlay() {
        if(overlayInitialized && !overlayVisible) {
            windowManager.addView(overlayView, olParams);
            overlayVisible = true;
        }
    }

    public void hideOverlay() {
        if(overlayInitialized && overlayVisible &&
                ((currMedia == null || currMedia.getType() == MediaType.SONG))) {
            windowManager.removeView(overlayView);
            overlayVisible = false;
        }
    }

    public void setMainVisible(boolean mainVisible) {
        this.mainVisible = mainVisible;
        if(mainVisible) hideOverlay();
        else showOverlay();
    }

    // handle force close of the app as normal close
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        musicCenter.finish();
        super.onTaskRemoved(rootIntent);
    }
}
