package net.majorkernelpanic.streaming.rtp.unpacker;

import android.util.Log;
import android.util.SparseArray;

import net.majorkernelpanic.streaming.BuildConfig;
import net.majorkernelpanic.streaming.ByteUtils;
import net.majorkernelpanic.streaming.InputStream;
import net.majorkernelpanic.streaming.rtcp.SenderReport;

import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
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
    public static final int TRANSPORT_TCP = 0x01;

    public static final int MTU = 1300;
    public static final long FIRST_RUN_DELAY = 6000;//2sec
    private static final boolean DEBUG = true;

    private final byte[][] mBuffers;

    private SenderReport mReport;
    private volatile int mSeq = 0;
    private int mBufferCount ,mBufferIn, mBufferOut;
    private DatagramPacket[] mPackets;
    private DatagramSocket mSocket;
    private Semaphore mBufferRequested;
    private Thread mReceiverThread;
    private SparseArray<Object> mSortBuffers;
    private long mWaitingTimeout;
    private boolean isFirstRun = true;
    private int mTransport;//传送方式

    public RtpReceiveSocket() {
        mBufferCount = 300;
        mBuffers = new byte[mBufferCount][];
        mPackets = new DatagramPacket[mBufferCount];
        mReport = new SenderReport();

        mSortBuffers = new SparseArray<Object>();

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
            mReceiverThread.setName("receiverThread");
            mReceiverThread.start();
        }
        if (isFirstRun) {
            try {
                Thread.sleep(FIRST_RUN_DELAY);
                isFirstRun = false;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        byte[] result = null;
        try {
            //等待时间最多只能是一帧的时间，一帧用多少时间由采样率决定，
            while (mSortBuffers.get(mSeq) == null) {
                if (DEBUG) Log.d(TAG, "skipping seq" + mSeq);
                Thread.sleep(mWaitingTimeout);
                mSeq++;
            }
            result = (byte[]) mSortBuffers.get(mSeq);
            if (DEBUG) Log.d(TAG, "reading seq: " + mSeq);
            mSortBuffers.remove(mSeq);
//            clearUpBuffers();
            mSeq++;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result;
    }


    public void reset() {
        mReport.reset();
        mBufferRequested = new Semaphore(mBufferCount);
        mBufferIn = mBufferOut = 0;
        mSeq = 0;
        mSortBuffers.clear();
    }

    /** Sets the destination address and to which the packets will be sent. */
    public void setDestination(int dport, int rtcpPort) {
        if (dport != 0 && rtcpPort != 0) {
            try {
                mSocket = new DatagramSocket(dport);
                mSocket.setSoTimeout(0);
                if (DEBUG) Log.d("RtpReceiveSocket", "listening on " + dport);
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
                    byte[] src = consumeData();
                    int seq = (int) ByteUtils.byteToLong(src, 2, 2);
                    if (DEBUG) Log.d(TAG, "receiving seq: " + seq);
                    mSortBuffers.put(seq, src);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void close() {
        mSocket.close();
        mReceiverThread.interrupt();
        reset();
    }


    /**
     * in millsec
     */
    public void setWaitingTimeout(long waitingTimeout) {
        mWaitingTimeout = waitingTimeout;
    }
}
