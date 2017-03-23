package com.koushikdutta.async.rtsp.server;


import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.AsyncServerSocket;
import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.ListenCallback;
import com.koushikdutta.async.http.server.AsyncHttpServerResponseImpl;
import com.koushikdutta.async.rtsp.Headers;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Leaves on 2017/3/12.
 */

public class AsyncRtspServer {
    ArrayList<AsyncServerSocket> mListeners;

    CompletedCallback mCompletedCallback;

    final Hashtable<String, ArrayList<Pair>> mActions = new Hashtable<String, ArrayList<Pair>>();


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
                AsyncHttpServerResponseImpl res;
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
                    path = fullPath.split("\\?")[0];
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
//                    res = new AsyncRtspServerResponseImpl(socket, this) {
//                        @Override
//                        protected void report(Exception e) {
//                            super.report(e);
//                            if (e != null) {
//                                socket.setDataCallback(new NullDataCallback());
//                                socket.setEndCallback(new NullCompletedCallback());
//                                socket.close();
//                            }
//                        }
//
//                        @Override
//                        protected void onEnd() {
//                            super.onEnd();
//                            mSocket.setEndCallback(null);
//                            responseComplete = true;
//                            // reuse the socket for a subsequent request.
//                            handleOnCompleted();
//                        }
//                    };
//
//                    boolean handled = onRequest(this, res);
//
//                    if (match == null && !handled) {
//                        res.code(404);
//                        res.end();
//                        return;
//                    }
//
//                    if (!getBody().readFullyOnRequest()) {
//                        onRequest(match, this, res);
//                    } else if (requestComplete) {
//                        onRequest(match, this, res);
//                    }
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

    public static Object getResponseCodeDescription(int code) {
        return null;
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
