package com.koushikdutta.async.rtsp.server;

import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.DataSink;
import com.koushikdutta.async.callback.CompletedCallback;

import com.koushikdutta.async.rtsp.Headers;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;

public interface AsyncRtspServerResponse extends DataSink, CompletedCallback {
    public void end();
    public void send(String contentType, byte[] bytes);
    public void send(String contentType, String string);
    public void send(String string);
    public void sendStream(InputStream inputStream, long totalLength);
    public AsyncRtspServerResponse code(int code);
    public int code();
    public Headers getHeaders();
    public void writeHead();
    public void setContentType(String contentType);
    public void redirect(String location);

    // NOT FINAL
//    public void proxy(AsyncRtspResponse response);

    /**
     * Alias for end. Used with CompletedEmitters
     */
    public void onCompleted(Exception ex);
    public AsyncSocket getSocket();
}
