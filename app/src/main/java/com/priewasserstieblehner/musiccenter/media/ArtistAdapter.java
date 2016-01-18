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
public class ArtistAdapter extends BaseAdapter{
    private ArrayList<Artist> artists;
    private LayoutInflater songInf;

    public ArtistAdapter(Context c, ArrayList<Artist> artists){
        this.artists=artists;
        songInf=LayoutInflater.from(c);
    }

    @Override
    public int getCount() {
        return artists.size();
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
        LinearLayout songLay = (LinearLayout)songInf.inflate
                (R.layout.listitem, parent, false);
        //get title and artist views
        TextView artistView = (TextView)songLay.findViewById(R.id.first_line);
        TextView emptyView1 = (TextView)songLay.findViewById(R.id.second_line);
        TextView emptyView2 = (TextView) songLay.findViewById(R.id.third_lines);
        //get song using position
        Artist currArtist = artists.get(position);
        //get title and artist strings
        String strArtist = currArtist.toString();
        artistView.setTextSize(18);
        artistView.setText(strArtist);
        emptyView1.setText("");
        emptyView2.setTextSize(0);
        emptyView2.setText("");
        //set position as tag
        songLay.setTag(currArtist);
        return songLay;
    }
}
