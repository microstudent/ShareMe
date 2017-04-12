package net.majorkernelpanic.streaming.rtp.unpacker;

import android.util.Log;

import net.majorkernelpanic.streaming.ByteUtils;
import net.majorkernelpanic.streaming.rtcp.SenderReport;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 这个类忠实地将接受到的数据向上传递
 * Created by Leaves on 2017/4/7.
 */

public class RtpReceiveSocket implements Runnable{
    private static final String TAG = "RtpReceiveSocket";
    /** Use this to use UDP for the transport protocol. */
    public final static int TRANSPORT_UDP = 0x00;

    public static final int MTU = 1300;

    private final byte[][] mBuffers;
    private final byte[] mTcpHeader;

    private SenderReport mReport;
    private int mSsrc, mPort = -1;
    private volatile long mSeq = 1;
    private int mTransport;
    private int mBufferCount ,mBufferIn, mBufferOut;
    private DatagramPacket[] mPackets;
    private long[] mTimestamps;
    private DatagramSocket mSocket;
    private Semaphore mBufferRequested, mBufferReceived, mSeqChecker;
    private Thread mReceiverThread, mCheckerThread;
    private TreeMap<Long, Object> mSortBuffers;

    public RtpReceiveSocket() {
        mBufferCount = 300;
        mBuffers = new byte[mBufferCount][];
        mPackets = new DatagramPacket[mBufferCount];
        mReport = new SenderReport();
        mTcpHeader = new byte[] {'$',0,0,0};

        mSortBuffers = new TreeMap<>();

        reset();

        for (int i=0; i<mBufferCount; i++) {

            mBuffers[i] = new byte[MTU];
            mPackets[i] = new DatagramPacket(mBuffers[i], MTU);

			/*							     Version(2)  Padding(0)					 					*/
			/*									 ^		  ^			Extension(0)						*/
			/*									 |		  |				^								*/
			/*									 | --------				|								*/
			/*									 | |---------------------								*/
			/*									 | ||  -----------------------> Source Identifier(0)	*/
			/*									 | ||  |												*/
//            mBuffers[i][0] = (byte) Integer.parseInt("10000000",2);

			/* Payload Type */
//            mBuffers[i][1] = (byte) 96;

			/* Byte 2,3        ->  Sequence Number                   */
			/* Byte 4,5,6,7    ->  Timestamp                         */
			/* Byte 8,9,10,11  ->  Sync Source Identifier            */

        }
    }

    public byte[] read() {
        if (mReceiverThread == null) {
            mReceiverThread = new Thread(this);
            mReceiverThread.start();
        }
        if (mCheckerThread == null) {
            mCheckerThread = new Thread(new CheckerRunnable());
            mCheckerThread.start();
        }
        byte[] result = null;
        try {
            while (!mSeqChecker.tryAcquire(5, TimeUnit.MILLISECONDS)) {
                mSeq++;
            }
            result = (byte[]) mSortBuffers.get(mSeq);
            mSortBuffers.remove(mSeq);
//            clearUpBuffers();
            mSeq++;
            if (mSeq < 100) {
                Log.d(TAG, "mSeq:" + mSeq);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result;
    }

    private void clearUpBuffers() {
        while (true) {
            Long key = mSortBuffers.floorKey(mSeq);
            if (key == null) {
                break;
            }
            mSortBuffers.remove(key);
        }
    }

    public void reset() {
        mTimestamps = new long[mBufferCount];
        mReport.reset();
        mBufferRequested = new Semaphore(mBufferCount);
        mBufferReceived = new Semaphore(mBufferCount);
        mBufferReceived.drainPermits();
        mSeqChecker = new Semaphore(1);
        mSeqChecker.drainPermits();
        mBufferIn = mBufferOut = 0;
        mSeq = 1;
        mSortBuffers.clear();
    }

    /** Sets the destination address and to which the packets will be sent. */
    public void setDestination(int dport, int rtcpPort) {
        if (dport != 0 && rtcpPort != 0) {
            mTransport = TRANSPORT_UDP;
            mPort = dport;
            try {
                mSocket = new DatagramSocket(dport);
//                Log.d("RtpReceiveSocket", "listening on " + dport);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
//            for (int i = 0; i < mBufferCount; i++) {
//                mPackets[i].setPort(dport);
//                mPackets[i].setAddress(dest);
//            }
//            mReport.setDestination(dest, rtcpPort);
        }
    }

    public byte[] consumeData() throws InterruptedException {
        mBufferReceived.acquire();
        byte[] result = mBuffers[mBufferOut];
        if (++mBufferOut >= mBufferCount) mBufferOut = 0;
        mBufferRequested.release();
        return result;
    }

    @Override
    public void run() {
        try {
            while (mBufferRequested.tryAcquire(4, TimeUnit.SECONDS)) {
                if (mSocket != null) {
                    mSocket.receive(mPackets[mBufferIn]);
                    if (++mBufferIn >= mBufferCount) mBufferIn = 0;
                    mBufferReceived.release();
                    byte[] src = consumeData();
                    long seq = ByteUtils.byteToLong(src, 2, 2);
                    mSortBuffers.put(seq, src);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void close() {
        mSocket.close();
        mCheckerThread.interrupt();
        mReceiverThread.interrupt();
        reset();
    }

    private class CheckerRunnable implements Runnable {
        @Override
        public void run() {
            while (!mCheckerThread.isInterrupted()) {
                if (mSortBuffers.containsKey(mSeq)) {
                    mSeqChecker.release();
                }
            }
        }
    }
}
