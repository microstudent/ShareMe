package net.majorkernelpanic.streaming.audio;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.support.annotation.NonNull;

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
    private final ByteBuffer[] mDecodeOutputBuffers;
    private final MediaCodec.BufferInfo mDecodeBufferInfo;
    private MediaCodec mMediaDecode;

    private MediaExtractor mMediaExtractor;
    private String mPath;
    private MediaFormat mMediaFormat;
    private boolean isEndOfInputFile;
    private int mOutputBufferIndex = -1;

    public Mp3Wrapper(String path) {
        mPath = path;
        mMediaExtractor = new MediaExtractor();
        try {
            mMediaExtractor.setDataSource(mPath);
            for (int i = 0; i < mMediaExtractor.getTrackCount(); i++) {//遍历媒体轨道 此处我们传入的是音频文件，所以也就只有一条轨道
                MediaFormat format = mMediaExtractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("audio")) {//获取音频轨道
//                    format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 200 * 1024);
                    mMediaExtractor.selectTrack(i);//选择此音频轨道
                    mMediaDecode = MediaCodec.createDecoderByType(mime);//创建Decode解码器
                    mMediaDecode.configure(format, null, null, 0);
                    break;
                }
            }
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
                        sizeHasRead += size;
                    }
                }
            }

            // Read the output from the codec.
            if (mOutputBufferIndex >= 0)
                // Ensure that the data is placed at the start of the buffer
                mDecodeOutputBuffers[mOutputBufferIndex].position(0);

            mOutputBufferIndex = mMediaDecode.dequeueOutputBuffer(mDecodeBufferInfo, 10000);
            if (mOutputBufferIndex >= 0) {
                // Handle EOF
                if (mDecodeBufferInfo.flags != 0) {
                    mMediaDecode.stop();
                    mMediaDecode.release();
                    mMediaDecode = null;
                    return 0;
                }

                audioBuffer.put(mDecodeOutputBuffers[mOutputBufferIndex]);
                // Release the buffer so MediaCodec can use it again.
                // The data should stay there until the next time we are called.
                mMediaDecode.releaseOutputBuffer(mOutputBufferIndex, false);
            }
        }
        return sizeHasRead;
    }
}
