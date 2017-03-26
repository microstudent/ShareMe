package com.leaves.app.shareme;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.rtsp.RtspServer;


public class RTSPActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rtsp);
        // Sets the port of the RTSP server to 1234
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putString(RtspServer.KEY_PORT, String.valueOf(7236));
        editor.apply();

        String path = Environment.getExternalStorageDirectory().getAbsolutePath();
        // Configures the SessionBuilder
        SessionBuilder.getInstance()
                .setContext(getApplicationContext())
                .setMp3Path(path + "/a.mp3")
                .setVideoEncoder(SessionBuilder.VIDEO_NONE)
                .setAudioEncoder(SessionBuilder.AUDIO_MP3);

        // Starts the RTSP server
        this.startService(new Intent(this,RtspServer.class));
//        AsyncRtspServer server = new AsyncRtspServer();
//        server.options("/");
//        server.get("/", new HttpServerRequestCallback() {
//            @Override
//            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
//                response.send("hello");
//            }
//        });
//        server.listen(7236);
    }
}
