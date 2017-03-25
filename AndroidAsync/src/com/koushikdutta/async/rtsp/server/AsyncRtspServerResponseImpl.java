package com.koushikdutta.async.rtsp.server;

import android.text.TextUtils;
import com.koushikdutta.async.*;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.callback.WritableCallback;
import com.koushikdutta.async.http.filter.ChunkedOutputFilter;
import com.koushikdutta.async.http.server.MalformedRangeException;
import com.koushikdutta.async.http.server.StreamSkipException;
import com.koushikdutta.async.rtsp.Headers;
import com.koushikdutta.async.rtsp.Protocol;
import com.koushikdutta.async.rtsp.RtspUtil;
import com.koushikdutta.async.util.StreamUtility;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

/**
 * Created by Leaves Zhang on 2017/3/23.
 */
public abstract class AsyncRtspServerResponseImpl implements AsyncRtspServerResponse {
    private Headers mRawHeaders = new Headers();
    private long mContentLength = -1;
    AsyncSocket mSocket;
    AsyncRtspServerRequestImpl mRequest;
    boolean headWritten = false;
    DataSink mSink;
    int code = 200;
    CompletedCallback closedCallback;
    WritableCallback writable;
    boolean ended;

    AsyncRtspServerResponseImpl(AsyncSocket socket, AsyncRtspServerRequestImpl req) {
        mSocket = socket;
        mRequest = req;
    }

    @Override
    public void write(ByteBufferList bb) {
        // order is important here...
//        assert !ended;
        // do the header write... this will call onWritable, which may be reentrant
        if (!headWritten)
            initFirstWrite();

        // now check to see if the list is empty. reentrancy may cause it to empty itself.
        if (bb.remaining() == 0)
            return;

        // null sink means that the header has not finished writing
        if (mSink == null)
            return;

        // can successfully write!
        mSink.write(bb);
    }

    private void initFirstWrite() {
        if (headWritten)
            return;

        headWritten = true;

        final boolean isChunked;
        String currentEncoding = mRawHeaders.get("Transfer-Encoding");
        if ("".equals(currentEncoding))
            mRawHeaders.removeAll("Transfer-Encoding");
        boolean canUseChunked = ("Chunked".equalsIgnoreCase(currentEncoding))
                && !"close".equalsIgnoreCase(mRawHeaders.get("Connection"));
        if (mContentLength < 0) {
            String contentLength = mRawHeaders.get("Content-Length");
            if (!TextUtils.isEmpty(contentLength))
                mContentLength = Long.valueOf(contentLength);
        }
        if (mContentLength < 0 && canUseChunked) {
            mRawHeaders.set("Transfer-Encoding", "Chunked");
            isChunked = true;
        } else {
            isChunked = false;
        }
        //添加CSeq的header
        if (mRequest != null) {
            mRawHeaders.set("CSeq", mRequest.getCSeq());
        }
        //添加服务器名称
        mRawHeaders.set("Server", AsyncRtspServer.getServerDescribe());

        String statusLine = String.format(Locale.ENGLISH, "RTSP/1.0 %s %s", code, AsyncRtspServer.getResponseCodeDescription(code));
        String rh = mRawHeaders.toPrefixString(statusLine);

        Util.writeAll(mSocket, rh.getBytes(), new CompletedCallback() {
            @Override
            public void onCompleted(Exception ex) {
                if (ex != null) {
                    report(ex);
                    return;
                }
                if (isChunked) {
                    ChunkedOutputFilter chunked = new ChunkedOutputFilter(mSocket);
                    chunked.setMaxBuffer(0);
                    mSink = chunked;
                } else {
                    mSink = mSocket;
                }

                mSink.setClosedCallback(closedCallback);
                closedCallback = null;
                mSink.setWriteableCallback(writable);
                writable = null;
                if (ended) {
                    // the response ended while headers were written
                    end();
                    return;
                }
                getServer().post(new Runnable() {
                    @Override
                    public void run() {
                        WritableCallback wb = getWriteableCallback();
                        if (wb != null)
                            wb.onWriteable();
                    }
                });
            }
        });
    }

    @Override
    public void setWriteableCallback(WritableCallback handler) {
        if (mSink != null)
            mSink.setWriteableCallback(handler);
        else
            writable = handler;
    }

    @Override
    public void end() {
        if (ended)
            return;
        ended = true;
        if (headWritten && mSink == null) {
            // header is in the process of being written... bail out.
            // end will be called again after finished.
            return;
        }
        if (!headWritten) {
            // end was called, and no head or body was yet written,
            // so strip the transfer encoding as that is superfluous.
            mRawHeaders.remove("Transfer-Encoding");
        }
        if (mSink instanceof ChunkedOutputFilter) {
            ((ChunkedOutputFilter)mSink).setMaxBuffer(Integer.MAX_VALUE);
            mSink.write(new ByteBufferList());
            onEnd();
        } else if (!headWritten) {
            writeHead();
            onEnd();
        } else {
            onEnd();
        }
    }

    protected abstract void onEnd();

    @Override
    public WritableCallback getWriteableCallback() {
        if (mSink != null)
            return mSink.getWriteableCallback();
        return writable;
    }

    @Override
    public void writeHead() {
        initFirstWrite();
    }

    @Override
    public void setContentType(String contentType) {
        mRawHeaders.set("Content-Type", contentType);
    }

    @Override
    public void send(String contentType, byte[] bytes) {
        mContentLength = bytes.length;
        mRawHeaders.set("Content-Length", Integer.toString(bytes.length));
        mRawHeaders.set("Content-Type", contentType);

        Util.writeAll(this, bytes, new CompletedCallback() {
            @Override
            public void onCompleted(Exception ex) {
                onEnd();
            }
        });
    }

    @Override
    public void send(String contentType, final String string) {
        try {
            send(contentType, string.getBytes("UTF-8"));
        }
        catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void send(String string) {
        String contentType = mRawHeaders.get("Content-Type");
        if (contentType == null)
            contentType = "text/plain; charset=utf-8";
        send(contentType, string);
    }

    @Override
    public void sendStream(final InputStream inputStream, long totalLength) {
        long start = 0;
        long end = totalLength - 1;

        String range = mRequest.getHeaders().get("Range");
        if (range != null) {
            String[] parts = range.split("=");
            if (parts.length != 2 || !"bytes".equals(parts[0])) {
                // Requested range not satisfiable
                code(416);
                end();
                return;
            }

            parts = parts[1].split("-");
            try {
                if (parts.length > 2)
                    throw new MalformedRangeException();
                if (!TextUtils.isEmpty(parts[0]))
                    start = Long.parseLong(parts[0]);
                if (parts.length == 2 && !TextUtils.isEmpty(parts[1]))
                    end = Long.parseLong(parts[1]);
                else
                    end = totalLength - 1;

                code(206);
                getHeaders().set("Content-Range", String.format(Locale.ENGLISH, "bytes %d-%d/%d", start, end, totalLength));
            }
            catch (Exception e) {
                code(416);
                end();
                return;
            }
        }
        try {
            if (start != inputStream.skip(start))
                throw new StreamSkipException("skip failed to skip requested amount");
            mContentLength = end - start + 1;
            mRawHeaders.set("Content-Length", String.valueOf(mContentLength));
            mRawHeaders.set("Accept-Ranges", "bytes");
            Util.pump(inputStream, mContentLength, this, new CompletedCallback() {
                @Override
                public void onCompleted(Exception ex) {
                    StreamUtility.closeQuietly(inputStream);
                    onEnd();
                }
            });
        }
        catch (Exception e) {
            code(500);
            end();
        }
    }

    @Override
    public AsyncRtspServerResponse code(int code) {
        this.code = code;
        return this;
    }

    @Override
    public int code() {
        return code;
    }

    @Override
    public void redirect(String location) {
        code(302);
        mRawHeaders.set("Location", location);
        end();
    }


    @Override
    public void onCompleted(Exception ex) {
        end();
    }

    @Override
    public boolean isOpen() {
        if (mSink != null)
            return mSink.isOpen();
        return mSocket.isOpen();
    }

    @Override
    public void setClosedCallback(CompletedCallback handler) {
        if (mSink != null)
            mSink.setClosedCallback(handler);
        else
            closedCallback = handler;
    }


    @Override
    public CompletedCallback getClosedCallback() {
        if (mSink != null)
            return mSink.getClosedCallback();
        return closedCallback;
    }

    @Override
    public AsyncServer getServer() {
        return mSocket.getServer();
    }

    @Override
    public String toString() {
        if (mRawHeaders == null)
            return super.toString();
        String statusLine = String.format(Locale.ENGLISH, "RTSP/1.0 %s %s", code, AsyncRtspServer.getResponseCodeDescription(code));
        return mRawHeaders.toPrefixString(statusLine);
    }

    protected abstract void report(Exception ex);

    @Override
    public Headers getHeaders() {
        return mRawHeaders;
    }

    public AsyncSocket getSocket() {
        return mSocket;
    }
}
