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
 * Created by mjoedpc on 06.01.2016.
 */
public class PlaylistAdapter extends BaseAdapter {

    private ArrayList<Playlist> playlists;
    private LayoutInflater songInf;

    public PlaylistAdapter(Context c, ArrayList<Playlist> playlists){
        this.playlists=playlists;
        songInf=LayoutInflater.from(c);
    }

    @Override
    public int getCount() {
        return playlists.size();
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
        TextView albumView = (TextView)listLay.findViewById(R.id.first_line);
        TextView artistView = (TextView)listLay.findViewById(R.id.second_line);
        TextView emptyView1 = (TextView) listLay.findViewById(R.id.third_lines);

        Playlist currPlaylist = playlists.get(position);
        String strAlbum = currPlaylist.toString();
        albumView.setTextSize(16);
        albumView.setText(strAlbum);

        //set position as tag
        listLay.setTag(currPlaylist);


        return listLay;

    }

}
