package net.majorkernelpanic.streaming;

import net.majorkernelpanic.streaming.inputstream.audio.AACInputStream;
import net.majorkernelpanic.streaming.rtcp.OnRTCPUpdateListener;
import net.majorkernelpanic.streaming.rtp.OnPCMDataAvailableListener;

import java.io.IOException;

/**
 * 针对Client使用的Session
 * Created by Leaves on 2017/4/10.
 */

public class ReceiveSession {
    private Object mVideoStream;
    private InputStream mAudioStream;

    private ReceiveSession() {
    }

    private void setAudioDecoder(AACInputStream stream) {
        mAudioStream = stream;
   }

    public void setVideoStream(Object videoStream) {
        mVideoStream = videoStream;
    }

    public void syncStart() {
        try {
            mAudioStream.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        mAudioStream.stop();
    }

    public InputStream getTrack(int id) {
        if (id == 0) {
            return mAudioStream;
        } else {
            return null;
        }
    }

    public void start() {
        syncStart();
    }

    public static class Builder{
        public static final int AUDIO_AAC = 1 >> 1;
        public static final int VIDEO_NONE = 1 >> 2;

        private int mAudioDecoder;
        private int mVideoDecoder;
        private OnRTCPUpdateListener mRTCPListener;
        private OnPCMDataAvailableListener mRTPListener;

        public Builder setAudioDecoder(int audioDecoder) {
            mAudioDecoder = audioDecoder;
            return this;
        }

        public Builder setVideoDecoder(int videoDecoder) {
            mVideoDecoder = videoDecoder;
            return this;
        }


        public Builder setRTCPListener(OnRTCPUpdateListener listener) {
            mRTCPListener = listener;
            return this;
        }

        public Builder setOnPCMDataAvailableListener(OnPCMDataAvailableListener listener) {
            mRTPListener = listener;
            return this;
        }

        public ReceiveSession build() {
            ReceiveSession session = new ReceiveSession();
            switch (mAudioDecoder) {
                case AUDIO_AAC:
                    AACInputStream stream = new AACInputStream();
                    stream.setOnRTCPUpdateListener(mRTCPListener);
                    stream.setOnPCMDataAvailableListener(mRTPListener);
                    session.setAudioDecoder(stream);
                    break;
            }
            switch (mVideoDecoder) {
                case VIDEO_NONE:
                    session.setVideoStream(null);
            }
//            if (session.getVideoTrack() != null) {
//                VideoStream video = session.getVideoTrack();
//                video.setFlashState(mFlash);
//                video.setVideoQuality(mVideoQuality);
//                video.setSurfaceView(mSurfaceView);
//                video.setPreviewOrientation(mOrientation);
//                video.setDestinationPorts(5006);
//            }

//            if (session.getAudioTrack() != null) {
//                InputStream audio = session.getAudioTrack();
//                audio.setListeningPorts(5004, 10000);
//            }
            return session;
        }
    }


    private InputStream getAudioTrack() {
        return mAudioStream;
    }

    private Object getVideoTrack() {
        return mVideoStream;
    }

    public interface Callback {

        /**
         * Called periodically to inform you on the bandwidth
         * consumption of the streams when streaming.
         */
        public void onBitrateUpdate(long bitrate);

        /** Called when some error occurs. */
        public void onSessionError(int reason, int streamType, Exception e);

        /**
         * Called when the streams of the session have correctly been started.
         * If an error occurs while starting the {@link Session},
         * {@link Callback#onSessionError(int, int, Exception)} will be
         * called instead of  {@link Callback#onSessionStarted()}.
         */
        public void onSessionStarted();

        /** Called when the stream of the session have been stopped. */
        public void onSessionStopped();
    }
}
