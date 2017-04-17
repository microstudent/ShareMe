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
        long ntpHB = ByteUtils.byteToLong(mBuffer, 8, 4);
        long ntpLB = ByteUtils.byteToLong(mBuffer, 12, 4);
        long ntpTime = ntpHB * 1000000000L + (ntpLB * 1000000000L) / 4294967296L;
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
}
