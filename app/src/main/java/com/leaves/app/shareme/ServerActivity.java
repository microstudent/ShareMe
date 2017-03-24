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
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;
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
        server.get("/", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                response.send("fuckyou");
            }
        });

        server.listen(8080);
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
