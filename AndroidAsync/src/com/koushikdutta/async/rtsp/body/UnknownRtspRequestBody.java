package com.koushikdutta.async.rtsp.body;

import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.DataSink;
import com.koushikdutta.async.Util;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.body.AsyncHttpRequestBody;
import com.koushikdutta.async.rtsp.AsyncRtspRequest;

public class UnknownRtspRequestBody implements AsyncRtspRequestBody<Void> {
    public UnknownRtspRequestBody(String contentType) {
        mContentType = contentType;
    }

    int length = -1;
    public UnknownRtspRequestBody(DataEmitter emitter, String contentType, int length) {
        mContentType = contentType;
        this.emitter = emitter;
        this.length = length;
    }

    private String mContentType;
    @Override
    public String getContentType() {
        return mContentType;
    }

    @Override
    public boolean readFullyOnRequest() {
        return false;
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public Void get() {
        return null;
    }

    @Deprecated
    public void setCallbacks(DataCallback callback, CompletedCallback endCallback) {
        emitter.setEndCallback(endCallback);
        emitter.setDataCallback(callback);
    }

    public DataEmitter getEmitter() {
        return emitter;
    }

    DataEmitter emitter;

    @Override
    public void write(AsyncRtspRequest request, DataSink sink, CompletedCallback completed) {
        Util.pump(emitter, sink, completed);
        if (emitter.isPaused())
            emitter.resume();
    }

    @Override
    public void parse(DataEmitter emitter, CompletedCallback completed) {
        this.emitter = emitter;
        emitter.setEndCallback(completed);
        emitter.setDataCallback(new DataCallback.NullDataCallback());
    }
}
