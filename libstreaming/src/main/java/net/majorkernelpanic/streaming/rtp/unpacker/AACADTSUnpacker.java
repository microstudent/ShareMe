package net.majorkernelpanic.streaming.rtp.unpacker;

import android.media.MediaCodec;
import android.os.SystemClock;
import android.util.Log;

import net.majorkernelpanic.streaming.ByteUtils;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;
import java.util.TreeMap;

import okio.Okio;

import static net.majorkernelpanic.streaming.rtp.unpacker.RtpReceiveSocket.MTU;

/**
 * Created by Leaves on 2017/4/2.
 */

public class AACADTSUnpacker extends AbstractUnpacker implements Runnable {
    private static final String TAG = "AACADTSUnpacker";
    private Thread mThread;
    private MediaCodec mDecoder;
    private ByteBuffer[] mInputBuffers;
    private Queue<Long> mTimeStampQueue;

    public AACADTSUnpacker(MediaCodec decoder) {
        if (decoder != null) {
            mDecoder = decoder;
            mInputBuffers = decoder.getInputBuffers();
        }
        mTimeStampQueue = new LinkedList<>();
    }

    @Override
    public void start() {
        if (mThread == null) {
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
            } catch (InterruptedException e) {
            }
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
        if (mDecoder != null) {
            long timeStamp = ByteUtils.byteToLong(rtpPacket, 4, 4);
            int AUHeaderLength = (int) ByteUtils.byteToLong(rtpPacket, rtphl, 2);
            int AUSize = (int) ByteUtils.byteToLong(rtpPacket, rtphl + 2, 2);
            AUSize >>= 3;
            int AUIndex = (int) ByteUtils.byteToLong(rtpPacket, rtphl + 3, 1);
            boolean bitMark = (rtpPacket[1] & 0x80) == 0x80;
            if (bitMark) {
                int inputIndex = mDecoder.dequeueInputBuffer(10000);
                if (inputIndex >= 0) {
                    ByteBuffer inputBuffer = mInputBuffers[inputIndex];
                    inputBuffer.clear();
                    inputBuffer.put(rtpPacket, rtphl + 4, AUSize);
                    ByteUtils.logByte(rtpPacket, rtphl + 4, AUSize);
                    mTimeStampQueue.add(timeStamp);
                    mDecoder.queueInputBuffer(inputIndex, 0, AUSize, 0, 0);

                } else {
                    Log.v(TAG, "No buffer available...");
                }
            } else {
                Log.d(TAG, "bitMark == false");
            }
        }
    }
}
