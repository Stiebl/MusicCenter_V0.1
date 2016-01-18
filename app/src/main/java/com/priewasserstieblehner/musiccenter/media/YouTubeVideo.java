package com.priewasserstieblehner.musiccenter.media;

/**
 * Created by jakob_000 on 02.01.2016.
 */
public class YouTubeVideo extends MyMedia{

    private String youTubeId;
    private String desc;
    private String thumbnailURL;

    public YouTubeVideo(String title, String youtTubeId, String desc, String thumbnailURL) {
            super(MediaType.YOUTUBE, 0, title);
        this.youTubeId = youtTubeId;
        this.desc = desc;
        this.thumbnailURL = thumbnailURL;
    }

    public String getYouTubeId() { return youTubeId; }

    public String getDesc() { return desc; }

    public String getThumbnailURL() { return thumbnailURL; }
}
