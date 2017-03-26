package com.leaves.sdk.media.audio;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Created by Leaves Zhang on 2017/3/25.
 */
public class AudioCodec implements IAudioCodec {
    private static final String TAG = "AudioCodec";

    private static final int ERROR_CODEC_NOT_SUPPORT = 0;

    private String mInputMediaType;
    private String mOutputMediaType;
    private Callback mCallback;

    private MediaCodec mMediaDecoder;
    private MediaCodec mMediaEncoder;
    private MediaFormat mInputFormat;
    private MediaFormat mOutputFormat;

    private ByteBuffer[] mDecodeInputBuffers;
    private ByteBuffer[] mDecodeOutputBuffers;
    private MediaCodec.BufferInfo mDecodeBufferInfo;
    private ByteBuffer[] mEncodeInputBuffers;
    private ByteBuffer[] mEncodeOutputBuffers;
    private MediaCodec.BufferInfo mEncodeBufferInfo;

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
        initMediaDecode();
    }

    private void initMediaDecode() {
        try {
            mMediaDecoder = MediaCodec.createDecoderByType(mInputMediaType);
            if (mInputFormat != null) {
                mMediaDecoder.configure(mInputFormat, null, null, 0);
            } else {
                MediaFormat decodeFormat = MediaFormat.createAudioFormat(mInputMediaType, 44100, 2);//参数对应-> mime type、采样率、声道数
                decodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, 96000);//比特率
                decodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 100 * 1024);//作用于inputBuffer的大小
                mMediaDecoder.configure(decodeFormat, null, null, 0);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "create mediaDecode failed");
            return;
        }
        mMediaDecoder.start();//启动MediaCodec ，等待传入数据
        mDecodeInputBuffers = mMediaDecoder.getInputBuffers();//MediaCodec在此ByteBuffer[]中获取输入数据
        mDecodeOutputBuffers = mMediaDecoder.getOutputBuffers();//MediaCodec将解码后的数据放到此ByteBuffer[]中 我们可以直接在这里面得到PCM数据
        mDecodeBufferInfo = new MediaCodec.BufferInfo();//用于描述解码得到的byte[]数据的相关信息
    }

    private void initMediaEncode() {
        try {
            mMediaEncoder = MediaCodec.createEncoderByType(mOutputMediaType);
            if (mOutputFormat != null) {
                mMediaEncoder.configure(mOutputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            } else {
                MediaFormat decodeFormat = MediaFormat.createAudioFormat(mOutputMediaType, 44100, 2);//参数对应-> mime type、采样率、声道数
                decodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, 96000);//比特率
                decodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 100 * 1024);//作用于inputBuffer的大小
                mMediaEncoder.configure(decodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "create mMediaEncoder failed");
            return;
        }
        mMediaEncoder.start();//启动MediaCodec ，等待传入数据
        mEncodeInputBuffers = mMediaEncoder.getInputBuffers();
        mEncodeOutputBuffers = mMediaEncoder.getOutputBuffers();
        mEncodeBufferInfo = new MediaCodec.BufferInfo();
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
