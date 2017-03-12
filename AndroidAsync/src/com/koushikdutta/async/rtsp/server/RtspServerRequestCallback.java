package com.koushikdutta.async.rtsp.server;



public interface RtspServerRequestCallback {
    public void onRequest(AsyncRtspServerRequest request, AsyncRtspServerResponse response);
}
