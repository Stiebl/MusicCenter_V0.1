package com.priewasserstieblehner.musiccenter;

import android.content.Context;
import android.util.Log;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.priewasserstieblehner.musiccenter.media.MyMedia;
import com.priewasserstieblehner.musiccenter.media.YouTubeVideo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jakob_000 on 02.01.2016.
 */
public class YouTubeConnector {
    private YouTube youTube;
    private YouTube.Search.List query;

    public YouTubeConnector(Context context) {
        youTube = new YouTube.Builder(new NetHttpTransport(),
                new JacksonFactory(), new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest hr) throws IOException {}
        }).setApplicationName(context.getString(R.string.app_name)).build();

        try{
            query = youTube.search().list("id,snippet");
            query.setKey(context.getResources().getString(R.string.app_key));
            query.setType("video");
            query.setFields("items(id/videoId,snippet/title,snippet/description,snippet/thumbnails/default/url)");
            query.setMaxResults((long) 25);
        }catch(IOException e){
            Log.d("YC", "Could not initialize: " + e);
        }
    }

    public ArrayList<MyMedia> search(String keywords){
        query.setQ(keywords);
        try{
            SearchListResponse response = query.execute();
            List<SearchResult> results = response.getItems();

            ArrayList<MyMedia> items = new ArrayList<MyMedia>();
            for(SearchResult result:results){
                String title = result.getSnippet().getTitle();
                String id = result.getId().getVideoId();
                String desc = result.getSnippet().getDescription();
                String thumbnailURL = result.getSnippet().getThumbnails().getDefault().getUrl();
                YouTubeVideo item = new YouTubeVideo(title,id, desc, thumbnailURL);
                items.add(item);
            }
            return items;
        }catch(IOException e){
            Log.d("YC", "Could not search: "+e);
            return null;
        }catch(Exception ex) {
            Log.d("Exception in YoutubeSearch", ex.toString());
            return null;
        }
    }

}
