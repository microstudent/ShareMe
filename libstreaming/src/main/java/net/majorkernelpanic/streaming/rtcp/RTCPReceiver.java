package net.majorkernelpanic.streaming.rtcp;

import net.majorkernelpanic.streaming.ByteUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * Created by Leaves on 2017/4/17.
 */

public class RTCPReceiver implements Runnable{
    private static final long OFFSET_1900_TO_1970 = ((365L * 70L) + 17L) * 24L * 60L * 60L;

    private static final int PACKET_LENGTH = 28;

    public static final int MTU = 1500;
    private DatagramSocket mSocket;
    private DatagramPacket mPacket;
    private Thread mReceiveThread;
    private byte[] mBuffer;
    private OnRTCPUpdateListener mOnRTCPUpdateListener;

    public RTCPReceiver() {
        mBuffer = new byte[MTU];
        mPacket = new DatagramPacket(mBuffer, MTU);
    }

    public void setDestination(int rtcpPort) {
        try {
            mSocket = new DatagramSocket(rtcpPort);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        mReceiveThread = new Thread(this);
        mReceiveThread.setName("rtcpReceiveThread");
    }

    @Override
    public void run() {
        while (!mReceiveThread.isInterrupted()) {
            try {
                mSocket.receive(mPacket);
                mPacket.getData();
                notifyDataReceived();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void notifyDataReceived() {
//        int length = (int) ((ByteUtils.byteToLong(mBuffer, 2, 2) + 1) * 4);//in byte
//        long ssrc = ByteUtils.byteToLong(mBuffer, 4, 4);

//        long ntpHB = ByteUtils.byteToLong(mBuffer, 8, 4);
//        long ntpLB = ByteUtils.byteToLong(mBuffer, 12, 4);
//        long ntpTime = ntpHB * 1000000000L + (ntpLB * 1000000000L) / 4294967296L;
        long ntpTime = readTimeStamp(mBuffer, 8);
        long rtpTime = ByteUtils.byteToLong(mBuffer, 16, 4);
//        long packetCount = ByteUtils.byteToLong(mBuffer, 20, 4);
//        long octetCount = ByteUtils.byteToLong(mBuffer, 24, 4);
        if (mOnRTCPUpdateListener != null) {
            mOnRTCPUpdateListener.onRTCPUpdate(ntpTime, rtpTime);
        }
    }

    public void setOnRTCPUpdateListener(OnRTCPUpdateListener onRTCPUpdateListener) {
        mOnRTCPUpdateListener = onRTCPUpdateListener;
    }

    public void start() {
        if (mReceiveThread != null) {
            mReceiveThread.start();
        }
    }

    /**
     * Reads the NTP time stamp at the given offset in the buffer and returns
     * it as a system time (milliseconds since January 1, 1970).
     */
    private long readTimeStamp(byte[] buffer, int offset) {
        long seconds = read32(buffer, offset);
        long fraction = read32(buffer, offset + 4);
        return ((seconds - OFFSET_1900_TO_1970) * 1000) + ((fraction * 1000L) / 0x100000000L);
    }

    /**
     * Reads an unsigned 32 bit big endian number from the given offset in the buffer.
     */
    private long read32(byte[] buffer, int offset) {
        byte b0 = buffer[offset];
        byte b1 = buffer[offset+1];
        byte b2 = buffer[offset+2];
        byte b3 = buffer[offset+3];
        // convert signed bytes to unsigned values
        int i0 = ((b0 & 0x80) == 0x80 ? (b0 & 0x7F) + 0x80 : b0);
        int i1 = ((b1 & 0x80) == 0x80 ? (b1 & 0x7F) + 0x80 : b1);
        int i2 = ((b2 & 0x80) == 0x80 ? (b2 & 0x7F) + 0x80 : b2);
        int i3 = ((b3 & 0x80) == 0x80 ? (b3 & 0x7F) + 0x80 : b3);
        return ((long)i0 << 24) + ((long)i1 << 16) + ((long)i2 << 8) + (long)i3;
    }
}
