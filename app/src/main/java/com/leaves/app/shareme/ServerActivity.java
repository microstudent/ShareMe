package com.leaves.app.shareme;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;

import java.util.ArrayList;
import java.util.List;

public class ServerActivity extends AppCompatActivity {

    @BindView(R.id.tv_log)
    TextView mLogView;

    AsyncHttpServer server = new AsyncHttpServer();
    List<WebSocket> _sockets = new ArrayList<WebSocket>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        ButterKnife.bind(this);
        server.websocket("/", "test", new AsyncHttpServer.WebSocketRequestCallback() {
            @Override
            public void onConnected(final WebSocket webSocket, AsyncHttpServerRequest request) {
                _sockets.add(webSocket);

                //Use this to clean up any references to your websocket
                webSocket.setClosedCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        try {
                            if (ex != null)
                                log(ex.toString());
                        } finally {
                            _sockets.remove(webSocket);
                        }
                    }
                });

                webSocket.setStringCallback(new WebSocket.StringCallback() {
                    @Override
                    public void onStringAvailable(String s) {
                        log(s);
                        _sockets.get(0).send("hello,My friend");
                    }
                });
            }
        });

        server.listen(8877);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        server.stop();
    }

    public void log(final String msg) {
        Observable.just(mLogView)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<TextView>() {
                    @Override
                    public void accept(TextView textView) throws Exception {
                        mLogView.append(msg);
                        mLogView.append("\n");
                    }
                });
    }
}
