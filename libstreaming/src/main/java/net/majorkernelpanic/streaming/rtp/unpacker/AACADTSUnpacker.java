package net.majorkernelpanic.streaming.rtp.unpacker;

import android.os.SystemClock;
import android.util.Log;

import net.majorkernelpanic.streaming.ByteUtils;

import java.nio.ByteBuffer;
import java.util.TreeMap;

import okio.Okio;

/**
 *
 * Created by Leaves on 2017/4/2.
 */

public class AACADTSUnpacker extends AbstractUnpacker implements Runnable{
    private static final String TAG = "AACADTSUnpacker";
    private Thread mThread;
    private ByteBuffer mParsedBuffer;//解析好的AAC音频

    public AACADTSUnpacker() {
        mParsedBuffer = ByteBuffer.allocate(1000);
        mParsedBuffer.put(mOutputBuffer);
    }

    @Override
    public void start() {
        if (mThread==null) {
            mThread = new Thread(this);
            mThread.start();
        }
    }

    @Override
    public void stop() {
        if (mThread != null) {
            mSocket.close();
            mThread.interrupt();
            try {
                mThread.join();
            } catch (InterruptedException e) {}
            mThread = null;
        }
    }

    @Override
    public void run() {
        while (!mThread.isInterrupted()) {
            byte[] result;
            result = mSocket.read();
            if (result != null) {
                parse(result);
            }
        }
    }

    private void parse(byte[] rtpPacket) {
        int AUHeaderLength = (int) ByteUtils.byteToLong(rtpPacket, rtphl, 2);
        int AUSize = (int) ByteUtils.byteToLong(rtpPacket, rtphl + 2, 2);
        int AUIndex = (int) ByteUtils.byteToLong(rtpPacket, rtphl + 3, 1);
        boolean bitMark = (rtpPacket[1] & 0x80) == 0x80;
        if (bitMark) {
            Log.d(TAG, "bitMark == true");
            if (mParsedBuffer.hasRemaining()) {
                mParsedBuffer.put(rtpPacket, AUHeaderLength, rtpPacket.length - AUHeaderLength);
            }
        } else {
            Log.d(TAG, "bitMark == false");
        }
    }
}
