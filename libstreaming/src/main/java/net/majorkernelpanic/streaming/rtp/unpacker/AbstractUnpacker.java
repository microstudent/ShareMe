package net.majorkernelpanic.streaming.rtp.unpacker;

import java.nio.ByteBuffer;

/**
 * 这个类对拿到的RTP数据进行解析，拆分
 * Created by Leaves on 2017/4/2.
 */

public abstract class AbstractUnpacker {
    protected ByteBuffer mOutputBuffer;
    protected RtpReceiveSocket mSocket;

    public AbstractUnpacker() {
        mOutputBuffer = ByteBuffer.allocate(10);
        mSocket = new RtpReceiveSocket();
    }

    public abstract void start();

    public abstract void stop();

    /**
     * Sets the destination of the stream.
     * @param dest The destination address of the stream
     * @param rtpPort Destination port that will be used for RTP
     * @param rtcpPort Destination port that will be used for RTCP
     */
    public void setDestination(int rtpPort, int rtcpPort) {
        mSocket.setDestination(rtpPort, rtcpPort);
    }

    public ByteBuffer getOutputBuffer() {
        return mOutputBuffer;
    }
}
