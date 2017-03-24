package com.koushikdutta.async.rtsp.server;


import android.util.Log;
import android.util.SparseArray;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.AsyncServerSocket;
import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.ListenCallback;
import com.koushikdutta.async.rtsp.Headers;
import com.koushikdutta.async.rtsp.RtspUtil;
import com.koushikdutta.async.rtsp.action.RtspOptions;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Leaves on 2017/3/12.
 */

public class AsyncRtspServer {
    public static final String TAG = "AsyncRtspServer";

    ArrayList<AsyncServerSocket> mListeners;

    CompletedCallback mCompletedCallback;

    final Hashtable<String, ArrayList<Pair>> mActions = new Hashtable<>();

    private static SparseArray<String> mCodes = new SparseArray<>();

    public AsyncRtspServer() {
        mListeners = new ArrayList<>();

    }

    private ListenCallback mListenCallback = new ListenCallback() {
        @Override
        public void onAccepted(final AsyncSocket socket) {
            String fullPath;
            String path;
            boolean responseComplete;
            boolean requestComplete;
            AsyncRtspServerRequestImpl request = new AsyncRtspServerRequestImpl() {
                RtspServerRequestCallback match;
                String fullPath;
                String path;
                boolean responseComplete;
                boolean requestComplete;
                AsyncRtspServerResponseImpl res;
                boolean hasContinued;

                @Override
                public String getPath() {
                    return path;
                }

                @Override
                protected void onHeadersReceived() {
                    String statusLine = getStatusLine();
                    String[] parts = statusLine.split(" ");
                    fullPath = parts[1];
                    path = RtspUtil.getRelativePath(fullPath);
                    Log.d("AsyncRtspServer", path);
                    method = parts[0];
                    synchronized (mActions) {
                        ArrayList<Pair> pairs = mActions.get(method);
                        if (pairs != null) {
                            for (Pair p : pairs) {
                                Matcher m = p.regex.matcher(path);
                                if (m.matches()) {
                                    mMatcher = m;
                                    match = p.callback;
                                    break;
                                }
                            }
                        }
                    }
                    res = new AsyncRtspServerResponseImpl(socket, this) {
                        @Override
                        protected void report(Exception e) {
                            if (e != null) {
                                socket.setDataCallback(new NullDataCallback());
                                socket.setEndCallback(new NullCompletedCallback());
                                socket.close();
                            }
                        }

                        @Override
                        protected void onEnd() {
                            mSocket.setEndCallback(null);
                            responseComplete = true;
                            // reuse the socket for a subsequent request.
                            handleOnCompleted();
                        }
                    };

                    boolean handled = onRequest(this, res);

                    if (match == null && !handled) {
                        res.code(404);
                        res.end();
                        return;
                    }

                    if (!getBody().readFullyOnRequest()) {
                        onRequest(match, this, res);
                    } else if (requestComplete) {
                        onRequest(match, this, res);
                    }
                }

                private void handleOnCompleted() {
                    if (requestComplete && responseComplete) {
//                        if (HttpUtil.isKeepAlive(Protocol.RTSP_1_0, getHeaders())) {
//                            onAccepted(socket);
//                        } else {
//                            socket.close();
//                        }
                    }
                }


                @Override
                public void onCompleted(Exception ex) {
                    // if the protocol was switched off http, ignore this request/response.
                    if (res.code() == 101)
                        return;
                    requestComplete = true;
                    super.onCompleted(ex);
                    // no http pipelining, gc trashing if the socket dies
                    // while the request is being sent and is paused or something
                    mSocket.setDataCallback(new NullDataCallback() {
                        @Override
                        public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
                            super.onDataAvailable(emitter, bb);
                            mSocket.close();
                        }
                    });
                    handleOnCompleted();

//                    if (getBody().readFullyOnRequest()) {
//                        onRequest(match, this, res);
//                    }
                }
            };
            request.setSocket(socket);
            socket.resume();
        }

        @Override
        public void onListening(AsyncServerSocket socket) {
            mListeners.add(socket);
        }

        @Override
        public void onCompleted(Exception ex) {
            report(ex);
        }
    };

    public void options(String regex) {
        addAction(RtspOptions.METHOD, regex, new DefaultOptionsRequestCallback());
    }

    public void options(String regex, RtspServerRequestCallback callback) {
        addAction(RtspOptions.METHOD, regex, callback);
    }

    public void addAction(String action, String regex, RtspServerRequestCallback callback) {
        Pair p = new Pair();
        p.regex = Pattern.compile("^" + regex);
        p.callback = callback;

        synchronized (mActions) {
            ArrayList<Pair> pairs = mActions.get(action);
            if (pairs == null) {
                pairs = new ArrayList<>();
                mActions.put(action, pairs);
            }
            pairs.add(p);
        }
    }

    public AsyncServerSocket listen(int port) {
        return listen(AsyncServer.getDefault(), port);
    }

    private AsyncServerSocket listen(AsyncServer server, int port) {
        return server.listen(null, port, mListenCallback);
    }

    private void report(Exception ex) {
        if (mCompletedCallback != null)
            mCompletedCallback.onCompleted(ex);
    }

    public void setErrorCallback(CompletedCallback completedCallback) {
        mCompletedCallback = completedCallback;
    }

    public ListenCallback getListenCallback() {
        return mListenCallback;
    }

    public CompletedCallback getErrorCallback() {
        return mCompletedCallback;
    }

    protected boolean onRequest(AsyncRtspServerRequest request, AsyncRtspServerResponse response) {
        return false;
    }

    protected void onRequest(RtspServerRequestCallback callback, AsyncRtspServerRequest request, AsyncRtspServerResponse response) {
        if (callback != null)
            callback.onRequest(request, response);
    }

    static {
        mCodes.put(100, "Continue");
        mCodes.put(200, "OK");
        mCodes.put(201, "Created");
        mCodes.put(250, "Low on Storage Space");
        mCodes.put(300, "Multiple Choices");
        mCodes.put(302, "Moved Temporarily");
        mCodes.put(303, "See Other");
        mCodes.put(400, "Bad Request");
        mCodes.put(404, "Not Found");
        mCodes.put(412, "Precondition Failed");
        mCodes.put(451, "Parameter Not Understood");
        mCodes.put(452, "Conference Not Found");
        mCodes.put(453, "Not Enough Bandwidth");
        mCodes.put(457, "Invalid Range");
        mCodes.put(458, "Parameter Is Read-Only");
        mCodes.put(502, "Bad Gateway");
        mCodes.put(505, "RTSP Version not supported");
        mCodes.put(551, "Option not supported");
    }

    public static Object getResponseCodeDescription(int code) {
        String d = mCodes.get(code);
        if (d == null)
            return "Unknown";
        return d;
    }

    private static class Pair {
        Pattern regex;
        RtspServerRequestCallback callback;
    }

    public void removeAction(String action, String regex) {
        synchronized (mActions) {
            ArrayList<Pair> pairs = mActions.get(action);
            if (pairs == null)
                return;
            for (int i = 0; i < pairs.size(); i++) {
                Pair p = pairs.get(i);
                if (regex.equals(p.regex.toString())) {
                    pairs.remove(i);
                    return;
                }
            }
        }
    }



}
