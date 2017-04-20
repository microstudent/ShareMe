package com.leaves.app.shareme;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.gl.SurfaceView;
import net.majorkernelpanic.streaming.rtsp.RtspServer;


public class RTSPActivity extends AppCompatActivity {

    private SurfaceView mSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rtsp);
        // Sets the port of the RTSP server to 1234
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putString(RtspServer.KEY_PORT, String.valueOf(7236));
        editor.apply();

        String path = Environment.getExternalStorageDirectory().getAbsolutePath();

        mSurfaceView = (SurfaceView) findViewById(R.id.surface);

        // Configures the SessionBuilder
        SessionBuilder.getInstance()
                .setContext(getApplicationContext())
                .setVideoEncoder(SessionBuilder.VIDEO_NONE)
                .setMp3Path(path + "/a.mp3")
                .setAudioEncoder(SessionBuilder.AUDIO_MP3);

        // Starts the RTSP server
        this.startService(new Intent(this,RtspServer.class));
    }
}
