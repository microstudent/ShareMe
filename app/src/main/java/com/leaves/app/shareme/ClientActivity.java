package com.leaves.app.shareme;

import android.app.Activity;
import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;
import droidninja.filepicker.FilePickerBuilder;
import droidninja.filepicker.FilePickerConst;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;

import java.io.*;
import java.util.ArrayList;

import static android.os.Environment.DIRECTORY_PICTURES;

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
    }


    @OnClick(R.id.bt_send)
    public void send() {
        String ip = mIPView.getText().toString();
        if (mWebSocket != null) {
//            mWebSocket.send(mMsgView.getText().toString());
            pickPhoto();
        } else {
            AsyncHttpClient.getDefaultInstance().websocket("http://" + ip, "test", new AsyncHttpClient.WebSocketConnectCallback() {
                @Override
                public void onCompleted(Exception ex, WebSocket webSocket) {
                    if (ex != null) {
                        log(ex.toString());
                        ex.printStackTrace();
                        return;
                    }
                    mWebSocket = webSocket;

                    pickPhoto();

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


    private void pickPhoto() {
        FilePickerBuilder.getInstance().setMaxCount(1)
                .setSelectedFiles(new ArrayList<String>())
                .setActivityTheme(R.style.AppTheme)
                .pickPhoto(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode)
        {
            case FilePickerConst.REQUEST_CODE_PHOTO:
                if(resultCode== Activity.RESULT_OK && data!=null)
                {
                    sendPic(data.getStringArrayListExtra(FilePickerConst.KEY_SELECTED_PHOTOS));
                }
                break;
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
        mWebSocket.close();
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
