package com.koushikdutta.async.rtsp.server;

import android.util.Log;

import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.FilteredDataEmitter;
import com.koushikdutta.async.LineEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.LineEmitter.StringCallback;
import com.koushikdutta.async.rtsp.Headers;
import com.koushikdutta.async.rtsp.body.AsyncRtspRequestBody;

import java.util.regex.Matcher;

/**
 * Created by Leaves on 2017/3/12.
 */

public abstract class AsyncRtspServerRequestImpl extends FilteredDataEmitter implements AsyncRtspServerRequest, CompletedCallback {
    public static final String TAG = "AsyncRtspServerRequest";
    private String statusLine;
    private Headers mRawHeaders = new Headers();
    AsyncSocket mSocket;
    Matcher mMatcher;
    String method;
    AsyncRtspRequestBody mBody;


    StringCallback mHeaderCallback = new StringCallback() {
        @Override
        public void onStringAvailable(String s) {
            //解析Header
            try {
                if (statusLine == null) {
                    statusLine = s;
                    if (statusLine.contains("RTSP/")) {
                        onNotRtsp();
                        mSocket.setDataCallback(null);
                    }
                } else if (!"\r".equals(s)){
                    mRawHeaders.addLine(s);
                } else {
                    //header解析完毕

                }
            }
            catch (Exception ex) {
                onCompleted(ex);
            }
        }
    };

    abstract protected void onHeadersReceived();

    private void onNotRtsp() {
        Log.w(TAG, "running on Not rtsp!");
    }

    @Override
    public void onCompleted(Exception ex) {
        report(ex);
    }

    @Override
    public Headers getHeaders() {
        return mRawHeaders;
    }

    @Override
    public void setDataCallback(DataCallback callback) {
        mSocket.setDataCallback(callback);
    }

    @Override
    public DataCallback getDataCallback() {
        return mSocket.getDataCallback();
    }

    @Override
    public boolean isChunked() {
        return mSocket.isChunked();
    }

    @Override
    public Matcher getMatcher() {
        return mMatcher;
    }

    @Override
    public AsyncRtspRequestBody getBody() {
        return mBody;
    }

    @Override
    public AsyncSocket getSocket() {
        return mSocket;
    }

    @Override
    public void pause() {
        mSocket.pause();
    }

    @Override
    public void resume() {
        mSocket.resume();
    }

    public String getStatusLine() {
        return statusLine;
    }

    @Override
    public boolean isPaused() {
        return mSocket.isPaused();
    }


    @Override
    public String getMethod() {
        return method;
    }

    void setSocket(AsyncSocket socket) {
        mSocket = socket;

        LineEmitter liner = new LineEmitter();
        mSocket.setDataCallback(liner);
        liner.setLineCallback(mHeaderCallback);
        mSocket.setEndCallback(new NullCompletedCallback());
    }

    @Override
    public String toString() {
        if (mRawHeaders == null)
            return super.toString();
        return mRawHeaders.toPrefixString(statusLine);
    }
}
