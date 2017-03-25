package com.koushikdutta.async.rtsp.body;

import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.DataSink;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.rtsp.AsyncRtspRequest;

public interface AsyncRtspRequestBody<T> {
    public void write(AsyncRtspRequest request, DataSink sink, CompletedCallback completed);
    public void parse(DataEmitter emitter, CompletedCallback completed);
    public String getContentType();

    /**
     * 是否在上一个onRequest中完全处理完毕
     */
    public boolean readFullyOnRequest();
    public int length();
    public T get();
}
