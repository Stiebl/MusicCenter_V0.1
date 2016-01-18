package com.priewasserstieblehner.musiccenter.playerhandling;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import com.google.android.youtube.player.YouTubeBaseActivity;
import com.priewasserstieblehner.musiccenter.MusicCenter;
import com.priewasserstieblehner.musiccenter.R;

public class OverlayAct extends YouTubeBaseActivity {

    private MusicService service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_overlay);
        LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View controllerView = inflater.inflate(R.layout.controller_overlay, null);

        service = MusicService.getInstance();
        if(service != null) {
            service.activityInitialized(controllerView, this);
            setFinishOnTouchOutside(false);
            moveTaskToBack(true);
            restoreMainActivity(null);
        }

    }

    // also used by button "restore of overlay"
    public void restoreMainActivity(View v) {
        Intent openMain = new Intent(this, MusicCenter.class);
        openMain.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(openMain);
    }

}
