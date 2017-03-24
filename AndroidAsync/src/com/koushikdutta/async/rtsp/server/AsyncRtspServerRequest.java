package com.koushikdutta.async.rtsp.server;

import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.rtsp.Headers;
import com.koushikdutta.async.rtsp.body.AsyncRtspRequestBody;

import java.util.regex.Matcher;

/**
 * Created by Leaves on 2017/3/12.
 */

public interface AsyncRtspServerRequest extends DataEmitter {
    public Headers getHeaders();
    public Matcher getMatcher();
    public AsyncRtspRequestBody getBody();
    public AsyncSocket getSocket();
    public String getPath();
    public String getMethod();
    String getCSeq();
}
