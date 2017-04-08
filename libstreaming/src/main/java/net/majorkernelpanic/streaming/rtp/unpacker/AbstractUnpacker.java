package net.majorkernelpanic.streaming.rtp.unpacker;

import java.net.Socket;
import java.nio.ByteBuffer;

import okio.Okio;
import okio.Source;

/**
 * Created by Leaves on 2017/4/2.
 */

public abstract class AbstractUnpacker {
    private ByteBuffer mOutputBuffer;
    private Socket mSocket;

    public AbstractUnpacker() {
        mOutputBuffer = ByteBuffer.allocate(10);
        mSocket = new Socket();
    }

    public ByteBuffer getOutputBuffer() {
        return mOutputBuffer;
    }
}
