package net.majorkernelpanic.streaming.audio;

import android.media.MediaCodec;
import android.util.Log;
import net.majorkernelpanic.streaming.rtp.MediaCodecInputStream;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

/**
 * Created by Leaves Zhang on 2017/3/27.
 */
public class AACCodecInputStream extends MediaCodecInputStream{

    private byte[] mADTSHeader;
    private int mHeaderPos = -1;

    public AACCodecInputStream(MediaCodec mediaCodec) {
        super(mediaCodec);
    }


    @Override
    public int read() throws IOException {
        int result = 0;
        try {
            if (mBuffer==null) {
                readFromCodec();
            }

            if (mClosed) throw new IOException("This InputStream was closed");

            if (mHeaderPos >= 0 && mHeaderPos < 7) {
                result = mADTSHeader[mHeaderPos++];
            } else {
                result = mBuffer.get();
            }

            if (!mBuffer.hasRemaining()) {
                mMediaCodec.releaseOutputBuffer(mIndex, false);
                mBuffer = null;
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }

        return result;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        int min = 0;

        try {
            if (mBuffer==null) {
                readFromCodec();
            }

            if (mClosed) throw new IOException("This InputStream was closed");

            if (mHeaderPos >= 0 && mHeaderPos < 7) {
                int remind = 7 - mHeaderPos;
                min = Math.min(length, remind);
                fillHeader(buffer, offset, min);
            } else {
                min = Math.min(length, mBufferInfo.size - mBuffer.position());
                mBuffer.get(buffer, offset, min);
            }
            if (mBuffer.position() >= mBufferInfo.size) {
                mMediaCodec.releaseOutputBuffer(mIndex, false);
                mBuffer = null;
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }

        return min;
    }

    private void readFromCodec() {
        int outBitSize;
        int outPacketSize;
        while (!Thread.interrupted() && !mClosed) {
            mIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 500000);
            if (mIndex >= 0) {
                outBitSize=mBufferInfo.size;
                outPacketSize = outBitSize + 7;//7为ADTS头部的大小
                //Log.d(TAG,"Index: "+mIndex+" Time: "+mBufferInfo.presentationTimeUs+" size: "+mBufferInfo.size);
                mBuffer = mBuffers[mIndex];
                mBuffer.position(mBufferInfo.offset);
                mBuffer.limit(mBufferInfo.offset + outBitSize);
                mADTSHeader = new byte[7];
                mHeaderPos = 0;
                addADTStoPacket(mADTSHeader, outPacketSize);//添加ADTS 代码后面会贴上
                break;
            } else {
                if (mIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    mBuffers = mMediaCodec.getOutputBuffers();
                } else if (mIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    mMediaFormat = mMediaCodec.getOutputFormat();
                    Log.i(TAG, mMediaFormat.toString());
                } else if (mIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    Log.v(TAG, "No buffer available...");
                    //return 0;
                } else {
                    Log.e(TAG, "Message: " + mIndex);
                    //return 0;
                }
            }
        }
    }

    private void fillHeader(byte[] buffer, int offset, int length) {
        for (int i = offset, count = 0; count < length; i++, count++) {
            buffer[i] = mADTSHeader[mHeaderPos++];
        }
    }

    /**
     * 添加ADTS头
     * @param packet
     * @param packetLen
     */
    private void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = 2; // AAC LC
        int freqIdx = 4; // 44.1KHz
        int chanCfg = 2; // CPE


        // fill in ADTS data
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }
}
