package com.leaves.app.shareme;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.leaves.app.shareme.wifidirect.WifiDirect;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {


    private WifiDirect mWifiDirect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mWifiDirect = new WifiDirect(this);
    }

    @OnClick(R.id.bt_server)
    public void goServer() {
        Intent intent = new Intent(this, ServerActivity.class);
        startActivity(intent);
    }


    @OnClick(R.id.bt_client)
    public void goClient() {
//        Intent intent = new Intent(this, ClientActivity.class);
//        startActivity(intent);
        mWifiDirect.scan();
    }
}
