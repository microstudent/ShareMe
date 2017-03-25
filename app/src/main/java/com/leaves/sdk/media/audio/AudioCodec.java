package com.leaves.sdk.media.audio;

import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Leaves Zhang on 2017/3/25.
 */
public class AudioCodec implements IAudioCodec {
    private static final String TAG = "AudioCodec";

    private static final int ERROR_CODEC_NOT_SUPPORT = 0;

    private String mInputMediaType;
    private String mOutputMediaType;
    private Callback mCallback;

    public static boolean isMediaCodecSupported() {
        try {
            Class.forName("android.media.MediaCodec");
            Log.i(TAG, "Phone supports the MediaCoded API");
            return true;
        } catch (ClassNotFoundException e) {
            Log.i(TAG, "Phone does not support the MediaCodec API");
            return false;
        }
    }

    @Override
    public void setInputType(String mediaType) {
        mInputMediaType = mediaType;
    }

    @Override
    public void setOutputType(String mediaType) {
        mOutputMediaType = mediaType;
    }


    @Override
    public void release() {

    }

    @Override
    public void prepare() {
        if (!isMediaCodecSupported()) {
            notifyError(ERROR_CODEC_NOT_SUPPORT);
            return;
        }

    }

    private void notifyError(int reason) {
        if (mCallback != null) {
            mCallback.onError(reason);
        }
    }

    @Override
    public void setInputStream(InputStream stream) {

    }

    @Override
    public OutputStream getOutputStream() {
        return null;
    }

    @Override
    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public interface Callback {
        void onError(int reason);

        void onStart();

        void onComplete();
    }
}
