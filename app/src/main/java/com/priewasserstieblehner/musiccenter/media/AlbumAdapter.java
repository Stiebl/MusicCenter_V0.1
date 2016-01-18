package com.priewasserstieblehner.musiccenter.media;

import android.content.Context;
import android.view.ContextMenu;
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
public class AlbumAdapter extends BaseAdapter{
    private ArrayList<Album> albums;
    private LayoutInflater songInf;

    public AlbumAdapter(Context c, ArrayList<Album> albums){
        this.albums=albums;
        songInf=LayoutInflater.from(c);
    }

    @Override
    public int getCount() {
        return albums.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //map to song layout
        LinearLayout listLay = (LinearLayout)songInf.inflate
                (R.layout.listitem, parent, false);
        //get title and artist views
        TextView albumView = (TextView)listLay.findViewById(R.id.first_line);
        TextView artistView = (TextView)listLay.findViewById(R.id.second_line);
        TextView emptyView1 = (TextView) listLay.findViewById(R.id.third_lines);
        //get song using position
        Album currAlbum = albums.get(position);
        Song song = (Song) currAlbum.getMedias().get(0);
        String artist = song != null ? song.getArtist() : "";
        //get title and artist strings
        String strAlbum = currAlbum.toString();
        albumView.setTextSize(16);
        albumView.setText(strAlbum);
        artistView.setText(artist);
        emptyView1.setTextSize(0);
        emptyView1.setText("");
        //set position as tag
        listLay.setTag(currAlbum);
        return listLay;
    }


}
