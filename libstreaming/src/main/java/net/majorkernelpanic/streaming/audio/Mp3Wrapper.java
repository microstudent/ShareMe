package net.majorkernelpanic.streaming.audio;

import android.media.*;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by Leaves Zhang on 2017/3/26.
 */
public class Mp3Wrapper {
    private static final String TAG = "Mp3Wrapper";
    /**
     * Denotes a successful operation.
     */
    public static final int SUCCESS = 0;

    public static final int SUCCESS_END_OF_STREAM = -5;

    /**
     * Denotes a generic operation failure.
     */
    public static final int ERROR = -1;
    /**
     * Denotes a failure due to the use of an invalid value.
     */
    public static final int ERROR_BAD_VALUE = -2;
    /**
     * Denotes a failure due to the improper use of a method.
     */
    public static final int ERROR_INVALID_OPERATION = -3;
    /**
     * An error code indicating that the object reporting it is no longer valid and needs to
     * be recreated.
     */
    public static final int ERROR_DEAD_OBJECT = -6;

    private final ByteBuffer[] mDecodeInputBuffers;
    private ByteBuffer[] mDecodeOutputBuffers;
    private final MediaCodec.BufferInfo mDecodeBufferInfo;
    private MediaCodec mMediaDecode;

    private MediaExtractor mMediaExtractor;
    private String mPath;
    private MediaFormat mMediaFormat;
    private boolean isEndOfInputFile;
    private int mOutputBufferIndex = -1;
    private long mSendOffset;
    private int mCount = 0;

    public Mp3Wrapper(String path) {
        mPath = path;
        mMediaExtractor = new MediaExtractor();
        try {
            mMediaExtractor.setDataSource(mPath);
            for (int i = 0; i < mMediaExtractor.getTrackCount(); i++) {//遍历媒体轨道 此处我们传入的是音频文件，所以也就只有一条轨道
                mMediaFormat = mMediaExtractor.getTrackFormat(i);
                String mime = mMediaFormat.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("audio")) {//获取音频轨道
//                    format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 200 * 1024);
                    mMediaExtractor.selectTrack(i);//选择此音频轨道
                    mMediaDecode = MediaCodec.createDecoderByType(mime);//创建Decode解码器
                    mMediaDecode.configure(mMediaFormat, null, null, 0);
                    break;
                }
            }
            mSendOffset = 1000000L / mMediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE) - 10;
//            int minBufferSize = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_CONFIGURATION_STEREO, AudioFormat.ENCODING_PCM_16BIT);
//            int bufferSize = 4 * minBufferSize;
        } catch (IOException e) {
            e.printStackTrace();
        }
        mMediaDecode.start();
        mDecodeInputBuffers = mMediaDecode.getInputBuffers();//MediaCodec在此ByteBuffer[]中获取输入数据
        mDecodeOutputBuffers = mMediaDecode.getOutputBuffers();//MediaCodec将解码后的数据放到此ByteBuffer[]中 我们可以直接在这里面得到PCM数据
        mDecodeBufferInfo = new MediaCodec.BufferInfo();//用于描述解码得到的byte[]数据的相关信息
    }

    public MediaFormat getMediaFormat() {
        return mMediaFormat;
    }


    public long getCurrentTime() {
        return mDecodeBufferInfo.presentationTimeUs;
    }

    /**
     * If this buffer is not a direct buffer, this method will always return 0.
     * Note that the value returned by {@link java.nio.Buffer#position()} on this buffer is
     * unchanged after a call to this method.
     * The representation of the data in the buffer will depend on the format specified in
     * the AudioRecord constructor, and will be native endian.
     * @param audioBuffer the direct buffer to which the recorded audio data is written.
     * Data is written to audioBuffer.position().
     * @param sizeInBytes the number of requested bytes. It is recommended but not enforced
     *    that the number of bytes requested be a multiple of the frame size (sample size in
     *    bytes multiplied by the channel count).
     * @return zero or the positive number of bytes that were read, or one of the following
     *    error codes. The number of bytes will not exceed sizeInBytes and will be truncated to be
     *    a multiple of the frame size.
     * <ul>
     * <li>{@link #ERROR_INVALID_OPERATION} if the object isn't properly initialized</li>
     * <li>{@link #ERROR_BAD_VALUE} if the parameters don't resolve to valid data and indexes</li>
     * <li>{@link #ERROR_DEAD_OBJECT} if the object is not valid anymore and
     *    needs to be recreated. The dead object error code is not returned if some data was
     *    successfully transferred. In this case, the error is returned at the next read()</li>
     * <li>{@link #ERROR} in case of other error</li>
     * </ul>
     *  这个方法将会阻塞线程
     */
    public int read(@NonNull ByteBuffer audioBuffer, int sizeInBytes) {
        if ( (audioBuffer == null) || (sizeInBytes < 0) ) {
            return ERROR_BAD_VALUE;
        }
        int sizeHasRead = 0;
        while (sizeHasRead < sizeInBytes) {
            // Read data from the file into the codec.
            if (!isEndOfInputFile) {
                int inputBufferIndex = mMediaDecode.dequeueInputBuffer(10000);
                if (inputBufferIndex >= 0) {
                    int size = mMediaExtractor.readSampleData(mDecodeInputBuffers[inputBufferIndex], 0);
                    if (size < 0) {
                        // End Of File
                        mMediaDecode.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        isEndOfInputFile = true;
                    } else {
                        mMediaDecode.queueInputBuffer(inputBufferIndex, 0, size, mMediaExtractor.getSampleTime(), 0);
                        mMediaExtractor.advance();
                    }
                }
            }

            // Read the output from the codec.
            if (mOutputBufferIndex >= 0)
                // Ensure that the data is placed at the start of the buffer
//                mDecodeOutputBuffers[mOutputBufferIndex].position(0);
                mDecodeOutputBuffers[mOutputBufferIndex].clear();

            mOutputBufferIndex = mMediaDecode.dequeueOutputBuffer(mDecodeBufferInfo, 10000);
            if (mOutputBufferIndex >= 0) {
                // Handle EOF
                if (mDecodeBufferInfo.flags != 0) {
                    Log.d(TAG, "EOF,flag = " + mDecodeBufferInfo.flags);

                    mMediaDecode.stop();
                    mMediaDecode.release();
                    mMediaDecode = null;
                    return SUCCESS_END_OF_STREAM;
                }

                if (mDecodeOutputBuffers[mOutputBufferIndex].remaining() <= audioBuffer.remaining()) {
                    sizeHasRead += mDecodeOutputBuffers[mOutputBufferIndex].remaining();
                    byte[] chunk = new byte[mDecodeBufferInfo.size];
                    mDecodeOutputBuffers[mOutputBufferIndex].mark();
                    mDecodeOutputBuffers[mOutputBufferIndex].get(chunk);
                    mDecodeOutputBuffers[mOutputBufferIndex].reset();
                    try {
                        //太快可能来不及接收，导致丢包,太慢可能导致跟不上播放速度
                        Thread.sleep(mSendOffset);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
//                    if (chunk.length > 0) {
//                        mAudioTrack.write(chunk, 0, chunk.length);
//                    }
//                    Log.d(TAG, "decoding count = " + (mCount++) + "sampleTime = " + mMediaExtractor.getSampleTime() + "and sample Rate = " + mMediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE));
                    audioBuffer.put(mDecodeOutputBuffers[mOutputBufferIndex]);
                    // Release the buffer so MediaCodec can use it again.
                    // The data should stay there until the next time we are called.
                    mMediaDecode.releaseOutputBuffer(mOutputBufferIndex, false);
                } else {
                    // Release the buffer so MediaCodec can use it again.
                    // The data should stay there until the next time we are called.
                    mMediaDecode.releaseOutputBuffer(mOutputBufferIndex, false);
                    break;
                }
            } else if (mOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                mDecodeOutputBuffers = mMediaDecode.getOutputBuffers();
            } else if (mOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                mMediaFormat = mMediaDecode.getOutputFormat();
                Log.i(TAG,mMediaFormat.toString());
            } else if (mOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                Log.v(TAG,"No buffer available...");
                return 0;
            } else {
                Log.e(TAG, "Message: " + mOutputBufferIndex);
                return 0;
            }
        }
        return sizeHasRead;
    }

    public void release() {
        if (mMediaDecode != null) {
            mMediaDecode.stop();
            mMediaDecode.release();
        }
        if (mMediaExtractor != null) {
            mMediaExtractor.release();
        }
    }
}
