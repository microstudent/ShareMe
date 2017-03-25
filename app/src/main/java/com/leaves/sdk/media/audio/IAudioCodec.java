package com.leaves.sdk.media.audio;

import android.media.MediaFormat;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Leaves Zhang on 2017/3/25.
 */
public interface IAudioCodec {
    void setInputType(String mediaType);

    void setOutputType(String mediaType);

    void release();

    void prepare();

    void setInputStream(InputStream stream);

    OutputStream getOutputStream();

    void setCallback(AudioCodec.Callback callback);
}
