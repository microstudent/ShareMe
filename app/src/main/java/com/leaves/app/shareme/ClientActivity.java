package com.leaves.app.shareme;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.EditText;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;

import java.io.*;
import java.util.ArrayList;


public class ClientActivity extends AppCompatActivity {

    @BindView(R.id.tv_log)
    TextView mLogView;
    @BindView(R.id.et_ip)
    EditText mIPView;
    @BindView(R.id.et_msg)
    EditText mMsgView;

    private WebSocket mWebSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        ButterKnife.bind(this);
        String ip = getIntent().getStringExtra("ip");
        mIPView.setText(ip + ":8877");
    }


    @OnClick(R.id.bt_send)
    public void send() {
        String ip1 = mIPView.getText().toString();
        if (mWebSocket != null) {
            mWebSocket.send(mMsgView.getText().toString());
        } else {
            AsyncHttpClient.getDefaultInstance().websocket("http://" + ip1, "test", new AsyncHttpClient.WebSocketConnectCallback() {
                @Override
                public void onCompleted(Exception ex, WebSocket webSocket) {
                    if (ex != null) {
                        log(ex.toString());
                        ex.printStackTrace();
                        return;
                    }
                    mWebSocket = webSocket;

//                    webSocket.send(mMsgView.getText().toString());
                    webSocket.setStringCallback(new WebSocket.StringCallback() {
                        public void onStringAvailable(String s) {
                            log("I got a string: " + s);
                        }
                    });
                }
            });
        }
    }


    private void sendPic(ArrayList<String> stringArrayListExtra) {
        File file = new File(stringArrayListExtra.get(0));
        if (file.exists()) {
            byte[] bytes = new byte[0];
            try {
                bytes = getContent(file.getPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            mWebSocket.send(bytes);
        }
    }


    public byte[] getContent(String filePath) throws IOException {
        File file = new File(filePath);
        long fileSize = file.length();
        if (fileSize > Integer.MAX_VALUE) {
            System.out.println("file too big...");
            return null;
        }
        FileInputStream fi = new FileInputStream(file);
        try {
            return readStream(fi);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public static byte[] readStream(InputStream inStream) throws Exception {
        byte[] buffer = new byte[1024];
        int len = -1;
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        while ((len = inStream.read(buffer)) != -1) {
            outStream.write(buffer, 0, len);
        }
        byte[] data = outStream.toByteArray();
        outStream.close();
        inStream.close();
        return data;

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mWebSocket != null) {
            mWebSocket.close();
        }
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
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });
    }
}
