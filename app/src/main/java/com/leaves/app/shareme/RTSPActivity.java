package com.leaves.app.shareme;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;


import com.koushikdutta.async.rtsp.server.AsyncRtspServer;
import com.koushikdutta.async.rtsp.server.DefaultOptionsRequestCallback;

import java.net.Inet4Address;

public class RTSPActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rtsp);
        AsyncRtspServer server = new AsyncRtspServer();
        server.options("/test.mp3");
//        server.get("/", new HttpServerRequestCallback() {
//            @Override
//            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
//                response.send("hello");
//            }
//        });
        server.listen(7236);
    }



}
