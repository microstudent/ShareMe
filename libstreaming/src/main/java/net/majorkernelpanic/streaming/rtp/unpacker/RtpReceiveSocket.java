package net.majorkernelpanic.streaming.rtp.unpacker;

import net.majorkernelpanic.streaming.rtcp.SenderReport;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by Leaves on 2017/4/7.
 */

public class RtpReceiveSocket {
    /** Use this to use UDP for the transport protocol. */
    public final static int TRANSPORT_UDP = 0x00;

    /** Use this to use TCP for the transport protocol. */
    public final static int TRANSPORT_TCP = 0x01;

    public static final int MTU = 1300;

    private final byte[][] mBuffers;
    private final byte[] mTcpHeader;

    private SenderReport mReport;
    private int mSsrc, mSeq = 0, mPort = -1;
    private int mTransport;
    private int mBufferCount;
    private DatagramPacket[] mPackets;
    private long[] mTimestamps;
    private DatagramSocket mSocket;

    public RtpReceiveSocket() {
        mBufferCount = 300; // TODO: readjust that when the FIFO is full
        mBuffers = new byte[mBufferCount][];
        mPackets = new DatagramPacket[mBufferCount];
        mReport = new SenderReport();
        mTcpHeader = new byte[] {'$',0,0,0};

        reset();

        for (int i=0; i<mBufferCount; i++) {

            mBuffers[i] = new byte[MTU];
            mPackets[i] = new DatagramPacket(mBuffers[i], 1);

			/*							     Version(2)  Padding(0)					 					*/
			/*									 ^		  ^			Extension(0)						*/
			/*									 |		  |				^								*/
			/*									 | --------				|								*/
			/*									 | |---------------------								*/
			/*									 | ||  -----------------------> Source Identifier(0)	*/
			/*									 | ||  |												*/
            mBuffers[i][0] = (byte) Integer.parseInt("10000000",2);

			/* Payload Type */
            mBuffers[i][1] = (byte) 96;

			/* Byte 2,3        ->  Sequence Number                   */
			/* Byte 4,5,6,7    ->  Timestamp                         */
			/* Byte 8,9,10,11  ->  Sync Source Identifier            */

        }
    }

    private void parse() {

    }

    private void reset() {
        mTimestamps = new long[mBufferCount];
        mReport.reset();
    }

    /** Sets the destination address and to which the packets will be sent. */
    public void setDestination(InetAddress dest, int dport, int rtcpPort) {
        if (dport != 0 && rtcpPort != 0) {
            mTransport = TRANSPORT_UDP;
            mPort = dport;
            try {
                mSocket = new DatagramSocket(dport);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
//            for (int i = 0; i < mBufferCount; i++) {
//                mPackets[i].setPort(dport);
//                mPackets[i].setAddress(dest);
//            }
            mReport.setDestination(dest, rtcpPort);
        }
    }
}
