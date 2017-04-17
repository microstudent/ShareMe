package net.majorkernelpanic.streaming.inputstream.audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import net.majorkernelpanic.streaming.InputStream;
import net.majorkernelpanic.streaming.rtcp.OnRTCPUpdateListener;
import net.majorkernelpanic.streaming.rtcp.RTCPReceiver;
import net.majorkernelpanic.streaming.rtp.OnPCMDataAvailableListener;
import net.majorkernelpanic.streaming.rtp.unpacker.AACADTSUnpacker;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Queue;


import static net.majorkernelpanic.streaming.rtp.unpacker.RtpReceiveSocket.MTU;

/**
 * 这个类将AAC音频解码
 * Created by Leaves on 2017/4/10.
 */

public class AACInputStream implements InputStream, Runnable {
    private AACADTSUnpacker mAACADTSUnpacker;
    private MediaCodec mMediaDecode;
    private ByteBuffer[] mOutputBuffer;
    private MediaCodec.BufferInfo mDecodeBufferInfo;

//    private AudioTrack mAudioTrack;
    private Thread mPlayThread;
    private Config mConfig;
    private long mCurrentTimeStamp = 0;
    private RTCPReceiver mRTCPReceiver;
    private OnPCMDataAvailableListener mOnPCMDataAvailableListener;

    public AACInputStream() {
        mRTCPReceiver = new RTCPReceiver();
    }

    @Override
    public void start() throws IllegalStateException, IOException {
        mAACADTSUnpacker.start();
        if (mPlayThread == null) {
            mPlayThread = new Thread(this);
            mPlayThread.setName("PlayThread");
            mPlayThread.start();
        }
    }

    private void consumePCMData(long rtpTime, byte[] chunkPCM) {
        if (mOnPCMDataAvailableListener != null) {
            mOnPCMDataAvailableListener.onPCMDataAvaialable(rtpTime, chunkPCM);
        }
//        if (mAudioTrack != null) {
//            long start = System.currentTimeMillis();
//            mAudioTrack.write(chunkPCM, 0, chunkPCM.length);
//            Log.d("AACInputStream", " cost time : " + (System.currentTimeMillis() - start));
//            Log.d("AACInputStream", "count:" + mSeq++);
//        }
    }

    @Override
    public void stop() {
        mAACADTSUnpacker.stop();
    }

    @Override
    public void config(Config config) {
        try {
            mMediaDecode = MediaCodec.createDecoderByType("audio/mp4a-latm");//创建Decode解码器
            MediaFormat format = MediaFormat.createAudioFormat("audio/mp4a-latm", config.sampleRate, config.channelCount);
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, MTU);
            format.setInteger(MediaFormat.KEY_IS_ADTS, 1);

            byte[] bytes = new byte[]{(byte) 0x12, (byte)0x12};
            ByteBuffer bb = ByteBuffer.wrap(bytes);
            format.setByteBuffer("csd-0", bb);

            mMediaDecode.configure(format, null, null, 0);
            mMediaDecode.start();
            mOutputBuffer = mMediaDecode.getOutputBuffers();
            mDecodeBufferInfo = new MediaCodec.BufferInfo();//用于描述解码得到的byte[]数据的相关信息
            mAACADTSUnpacker = new AACADTSUnpacker(mMediaDecode);
            mAACADTSUnpacker.setConfig(config);
//
//            mAudioTrack = new AudioTrack(
//                    AudioManager.STREAM_MUSIC,
//                    config.sampleRate,
//                    AudioFormat.CHANNEL_OUT_STEREO,
//                    AudioFormat.ENCODING_PCM_16BIT,
//                    MTU,
//                    AudioTrack.MODE_STREAM
//            );
            mConfig = config;
//            mAudioTrack.play();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void setListeningPorts(int rtpPort, int rtcpPort) {
        mAACADTSUnpacker.setDestination(rtpPort, rtcpPort);
        mRTCPReceiver.setDestination(rtcpPort);
    }


    @Override
    public void setOutputStream(OutputStream stream, byte channelIdentifier) {
        //TCP用
    }

    @Override
    public int[] getLocalPorts() {
        return new int[0];
    }

    @Override
    public int getSSRC() {
        return 0;
    }

    @Override
    public long getBitrate() {
        return 0;
    }

    @Override
    public String getSessionDescription() throws IllegalStateException {
        return null;
    }

    @Override
    public boolean isStreaming() {
        return false;
    }

    @Override
    public void run() {
        while (!mPlayThread.isInterrupted()) {
            int outputIndex = mMediaDecode.dequeueOutputBuffer(mDecodeBufferInfo, 10000);
            ByteBuffer outputBuffer;
            byte[] chunkPCM;
            if (outputIndex >= 0) {
                outputBuffer = mOutputBuffer[outputIndex];//拿到用于存放PCM数据的Buffer
                chunkPCM = new byte[mDecodeBufferInfo.size];//BufferInfo内定义了此数据块的大小
                outputBuffer.get(chunkPCM);//将Buffer内的数据取出到字节数组中
                outputBuffer.clear();//数据取出后一定记得清空此Buffer MediaCodec是循环使用这些Buffer的，不清空下次会得到同样的数据
                playSilentIfNeeded(mDecodeBufferInfo.presentationTimeUs);
                consumePCMData(mDecodeBufferInfo.presentationTimeUs, chunkPCM);//自己定义的方法，供编码器所在的线程获取数据,下面会贴出代码
                mMediaDecode.releaseOutputBuffer(outputIndex, false);//此操作一定要做，不然MediaCodec用完所有的Buffer后 将不能向外输出数据
            }
        }
    }

    private void playSilentIfNeeded(long timeStamp) {
        if (mCurrentTimeStamp == 0) {
            mCurrentTimeStamp = timeStamp;
        }
        while (mCurrentTimeStamp + 1 != timeStamp && timeStamp > mCurrentTimeStamp) {
            try {
                Thread.sleep(1000000L / mConfig.sampleRate);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.d("AACInputStream", "current = " + mCurrentTimeStamp + "timeStamp = " + timeStamp);
            mCurrentTimeStamp++;
        }
        mCurrentTimeStamp = timeStamp;
    }

    public void setOnRTCPUpdateListener(OnRTCPUpdateListener onRTCPUpdateListener) {
        if (mRTCPReceiver != null) {
            mRTCPReceiver.setOnRTCPUpdateListener(onRTCPUpdateListener);
        }
    }

    public void setOnPCMDataAvailableListener(OnPCMDataAvailableListener onPCMDataAvailableListener) {
        mOnPCMDataAvailableListener = onPCMDataAvailableListener;
    }
}
