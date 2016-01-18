package com.priewasserstieblehner.musiccenter.media;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.priewasserstieblehner.musiccenter.R;

import java.util.ArrayList;

/**
 * Created by jakob_000 on 21.10.2015.
 */
public class MediaAdapter extends BaseAdapter{
    private ArrayList<MyMedia> medias;
    private LayoutInflater songInf;

    public MediaAdapter(Context c, ArrayList<MyMedia> theSongs){
        medias =theSongs;
        songInf=LayoutInflater.from(c);
    }

    @Override
    public int getCount() {
        return medias.size();
    }

    @Override
    public Object getItem(int position) {

        return medias.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //map to song layout
        LinearLayout songLay = (LinearLayout)songInf.inflate
                (R.layout.listitem, parent, false);
        //get title and artist views
        TextView titleView = (TextView)songLay.findViewById(R.id.first_line);
        TextView artistView = (TextView)songLay.findViewById(R.id.second_line);
        TextView albumView = (TextView) songLay.findViewById(R.id.third_lines);
        //get song using position
        MyMedia currMedia = medias.get(position);
        String title = currMedia.getTitle();
        titleView.setText(title);
        switch(currMedia.getType()) {
            case SONG:
                Song currSong = (Song) currMedia;
                //get title and artist strings
                artistView.setText(currSong.getArtist());
                albumView.setText(currSong.getAlbum());
                break;
            case VIDEO:
                artistView.setText("Video");
                albumView.setVisibility(View.GONE);
                albumView.setText("");
                albumView.setTextSize(0);
                break;
            case YOUTUBE:
                YouTubeVideo video = (YouTubeVideo) currMedia;
                artistView.setText(video.getDesc());
                albumView.setText("YouTube");
                break;
        }

        //set position as tag
        songLay.setTag(position);
        return songLay;
    }
}
