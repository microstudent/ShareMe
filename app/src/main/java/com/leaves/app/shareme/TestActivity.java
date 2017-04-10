package com.leaves.app.shareme;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

public class TestActivity extends AppCompatActivity {

    private MediaExtractor mMediaExtractor;
    private MediaFormat mMediaFormat;
    private ByteBuffer mDecodeInputBuffer;
    String path = Environment.getExternalStorageDirectory().getAbsolutePath();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        mDecodeInputBuffer = ByteBuffer.allocate(20000);

        mMediaExtractor = new MediaExtractor();
        try {
            mMediaExtractor.setDataSource(path + "/a.mp3");
            for (int i = 0; i < mMediaExtractor.getTrackCount(); i++) {//遍历媒体轨道 此处我们传入的是音频文件，所以也就只有一条轨道
                mMediaFormat = mMediaExtractor.getTrackFormat(i);
                String mime = mMediaFormat.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("audio")) {//获取音频轨道
//                    format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 200 * 1024);
                    mMediaExtractor.selectTrack(i);//选择此音频轨道
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (true) {
            int size = mMediaExtractor.readSampleData(mDecodeInputBuffer, 0);
            if (size < 0) {
                // End Of File
                Log.d("TestActivity", "end of file");
                break;
            } else {
                Log.d("TestActivity", "getting sample");
                mMediaExtractor.advance();
            }
        }
    }
}
