package com.koushikdutta.async.rtsp;

import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.FilteredDataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.filter.ChunkedInputFilter;
import com.koushikdutta.async.http.filter.ContentLengthFilter;
import com.koushikdutta.async.http.filter.GZIPInputFilter;
import com.koushikdutta.async.http.filter.InflaterInputFilter;
import com.koushikdutta.async.rtsp.body.AsyncRtspRequestBody;
import com.koushikdutta.async.rtsp.body.SDPBody;

/**
 * 主要是一些解析的方法
 * Created by Leaves on 2017/3/23.
 */
public class RtspUtil {
    public static DataEmitter getBodyDecoder(DataEmitter emitter, Protocol protocol, Headers headers, boolean server) {
        long _contentLength;
        try {
            _contentLength = Long.parseLong(headers.get("Content-Length"));
        }
        catch (Exception ex) {
            _contentLength = -1;
        }
        final long contentLength = _contentLength;
        if (-1 != contentLength) {
            if (contentLength < 0) {
                EndEmitter ender = EndEmitter.create(emitter.getServer(), new BodyDecoderException("not using chunked encoding, and no content-length found."));
                ender.setDataEmitter(emitter);
                emitter = ender;
                return emitter;
            }
            if (contentLength == 0) {
                EndEmitter ender = EndEmitter.create(emitter.getServer(), null);
                ender.setDataEmitter(emitter);
                emitter = ender;
                return emitter;
            }
            ContentLengthFilter contentLengthWatcher = new ContentLengthFilter(contentLength);
            contentLengthWatcher.setDataEmitter(emitter);
            emitter = contentLengthWatcher;
        } else if ("chunked".equalsIgnoreCase(headers.get("Transfer-Encoding"))) {
            ChunkedInputFilter chunker = new ChunkedInputFilter();
            chunker.setDataEmitter(emitter);
            emitter = chunker;
        } else {
            if ((server || protocol == Protocol.RTSP_1_0) && !"close".equalsIgnoreCase(headers.get("Connection"))) {
                // if this is the server, and the client has not indicated a request body, the client is done
                EndEmitter ender = EndEmitter.create(emitter.getServer(), null);
                ender.setDataEmitter(emitter);
                emitter = ender;
                return emitter;
            }
        }

        if ("gzip".equals(headers.get("Content-Encoding"))) {
            GZIPInputFilter gunzipper = new GZIPInputFilter();
            gunzipper.setDataEmitter(emitter);
            emitter = gunzipper;
        }
        else if ("deflate".equals(headers.get("Content-Encoding"))) {
            InflaterInputFilter inflater = new InflaterInputFilter();
            inflater.setDataEmitter(emitter);
            emitter = inflater;
        }

        // conversely, if this is the client (http 1.0), and the server has not indicated a request body, we do not report
        // the close/end event until the server actually closes the connection.
        return emitter;
    }

    public static AsyncRtspRequestBody getBody(DataEmitter emitter, CompletedCallback reporter, Headers headers) {
        String contentType = headers.get("Content-Type");
        if (contentType != null) {
            String[] values = contentType.split(";");
            for (int i = 0; i < values.length; i++) {
                values[i] = values[i].trim();
            }
            for (String ct: values) {
                if (SDPBody.CONTENT_TYPE.equals(ct)) {
                    return new SDPBody();
                }
            }
        }

        return null;
    }


    static class EndEmitter extends FilteredDataEmitter {
        private EndEmitter() {
        }

        public static EndEmitter create(AsyncServer server, final Exception e) {
            final EndEmitter ret = new EndEmitter();
            // don't need to worry about any race conditions with post and this return value
            // since we are in the server thread.
            server.post(new Runnable() {
                @Override
                public void run() {
                    ret.report(e);
                }
            });
            return ret;
        }
    }

    public static boolean isKeepAlive(Protocol protocol, Headers headers) {
        String connection = headers.get("Connection");
        if (connection == null)
            return protocol == Protocol.RTSP_1_0;
        return "keep-alive".equalsIgnoreCase(connection);
    }
}
