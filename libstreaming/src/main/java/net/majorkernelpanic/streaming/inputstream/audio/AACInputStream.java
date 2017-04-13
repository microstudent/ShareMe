package net.majorkernelpanic.streaming.inputstream.audio;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import net.majorkernelpanic.streaming.InputStream;
import net.majorkernelpanic.streaming.Stream;
import net.majorkernelpanic.streaming.rtp.unpacker.AACADTSUnpacker;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;

import static net.majorkernelpanic.streaming.rtp.unpacker.RtpReceiveSocket.MTU;

/**
 * 这个类将AAC音频解码
 * Created by Leaves on 2017/4/10.
 */

public class AACInputStream implements InputStream {

    private AACADTSUnpacker mAACADTSUnpacker;
    private MediaCodec mMediaDecode;
    private ByteBuffer[] mOutputBuffer;
    private MediaCodec.BufferInfo mDecodeBufferInfo;

    public AACInputStream() {
    }

    @Override
    public void start() throws IllegalStateException, IOException {
        mAACADTSUnpacker.start();
        int outputIndex = mMediaDecode.dequeueOutputBuffer(mDecodeBufferInfo, 10000);
        ByteBuffer outputBuffer;
        byte[] chunkPCM;
        while (outputIndex >= 0) {//每次解码完成的数据不一定能一次吐出 所以用while循环，保证解码器吐出所有数据
            outputBuffer = mOutputBuffer[outputIndex];//拿到用于存放PCM数据的Buffer
            chunkPCM = new byte[mDecodeBufferInfo.size];//BufferInfo内定义了此数据块的大小
            outputBuffer.get(chunkPCM);//将Buffer内的数据取出到字节数组中
            outputBuffer.clear();//数据取出后一定记得清空此Buffer MediaCodec是循环使用这些Buffer的，不清空下次会得到同样的数据
//            putPCMData(chunkPCM);//自己定义的方法，供编码器所在的线程获取数据,下面会贴出代码
            mMediaDecode.releaseOutputBuffer(outputIndex, false);//此操作一定要做，不然MediaCodec用完所有的Buffer后 将不能向外输出数据
            outputIndex = mMediaDecode.dequeueOutputBuffer(mDecodeBufferInfo, 10000);//再次获取数据，如果没有数据输出则outputIndex=-1 循环结束
        }
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
            format.setInteger(MediaFormat.KEY_BIT_RATE, 32000);
            mMediaDecode.configure(format, null, null, 0);
            mMediaDecode.start();
            mOutputBuffer = mMediaDecode.getOutputBuffers();
            mDecodeBufferInfo = new MediaCodec.BufferInfo();//用于描述解码得到的byte[]数据的相关信息
            mAACADTSUnpacker = new AACADTSUnpacker(mMediaDecode);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void setListeningPorts(int rtpPort, int rtcpPort) {
        mAACADTSUnpacker.setDestination(rtpPort, rtcpPort);
    }


    @Override
    public void setOutputStream(OutputStream stream, byte channelIdentifier) {

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
}
