package com.koushikdutta.async.rtsp.body;

import android.util.Pair;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.DataSink;
import com.koushikdutta.async.Util;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.rtsp.AsyncRtspRequest;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;

/**
 * Created by Leaves Zhang on 2017/3/23.
 */
public class SDPBody implements AsyncRtspRequestBody<HashMap<String, String>> {
    private HashMap<String, String> mParameters;
    private byte[] mBodyBytes;
    public static final String CONTENT_TYPE = "application/sdp";

    public SDPBody(HashMap<String, String> mParameters) {
        this.mParameters = mParameters;
    }

    public SDPBody() {
    }

    private void buildData() {
        boolean first = true;
        StringBuilder b = new StringBuilder();
        try {
            for (String key : mParameters.keySet()) {
                if (mParameters.get(key) == null) {
                    continue;
                }
                if (!first)
                    b.append("\r\n");
                first = false;

                b.append(URLEncoder.encode(key, "UTF-8"));
                b.append('=');
                b.append(URLEncoder.encode(mParameters.get(key), "UTF-8"));
            }
            mBodyBytes = b.toString().getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }



    @Override
    public void write(AsyncRtspRequest request, DataSink response, CompletedCallback completed) {
        if (mBodyBytes == null)
            buildData();
        Util.writeAll(response, mBodyBytes, completed);
    }

    @Override
    public void parse(DataEmitter emitter, final CompletedCallback completed) {
        final ByteBufferList data = new ByteBufferList();
        emitter.setDataCallback(new DataCallback() {
            @Override
            public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
                bb.get(data);
            }
        });
        emitter.setEndCallback(new CompletedCallback() {
            @Override
            public void onCompleted(Exception ex) {
                if (ex != null) {
                    completed.onCompleted(ex);
                    return;
                }
                try {
                    mParameters = parse(data.readString(), "\r\n", false, null);
                    completed.onCompleted(null);
                }
                catch (Exception e) {
                    completed.onCompleted(e);
                }
            }
        });
    }

    @Override
    public String getContentType() {
        return CONTENT_TYPE + "; charset=utf-8";
    }

    @Override
    public boolean readFullyOnRequest() {
        return true;
    }

    @Override
    public int length() {
        if (mBodyBytes == null)
            buildData();
        return mBodyBytes.length;    }

    @Override
    public HashMap<String, String> get() {
        return mParameters;
    }

    public interface StringDecoder {
        public String decode(String s);
    }

    public static HashMap<String, String> parse(String value, String delimiter, boolean unquote, StringDecoder decoder) {
        HashMap<String, String> map = new HashMap<>();
        if (value == null)
            return map;
        String[] parts = value.split(delimiter);
        for (String part : parts) {
            String[] pair = part.split("=", 2);
            String key = pair[0].trim();
            String v = null;
            if (pair.length > 1)
                v = pair[1];
            if (unquote && v != null && v.endsWith("\"") && v.startsWith("\""))
                v = v.substring(1, v.length() - 1);
            if (decoder != null) {
                key = decoder.decode(key);
                v = decoder.decode(v);
            }
            map.put(key, v);
        }
        return map;
    }
}
