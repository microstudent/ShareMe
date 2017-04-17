package com.leaves.app.shareme.service;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.leaves.app.shareme.bean.Media;

import net.majorkernelpanic.streaming.InputStream;
import net.majorkernelpanic.streaming.ReceiveSession;
import net.majorkernelpanic.streaming.rtsp.RtspClient;
import net.majorkernelpanic.streaming.rtsp.RtspServer;

/**
 * Created by Leaves on 2017/4/18.
 */

public class MusicClientService extends AbsMusicService implements ReceiveSession.Callback, RtspClient.Callback {
    private AudioTrack mAudioTrack;
    private ClientBinder mBinder;
    private ReceiveSession mSession;
    private RtspClient mClient;
    private String mServerIp;

    @Override
    public void onCreate() {
        super.onCreate();
        // Configures the SessionBuilder
        mSession = new ReceiveSession.Builder()
                .setAudioDecoder(ReceiveSession.Builder.AUDIO_AAC)
                .setVideoDecoder(ReceiveSession.Builder.VIDEO_NONE)
                .setCallback(this)
                .build();
        mClient = new RtspClient();
        mClient.setSession(mSession);
        mClient.setCallback(this);
    }

    @Override
    protected void pause() {
        if (mAudioTrack != null) {
            mAudioTrack.pause();
        }
    }

    @Override
    protected void start(boolean invalidate) {
        mAudioTrack.play();
        if (!mClient.isStreaming()) {
            String ip, path;
            ip = mServerIp;
            path = "";
            mClient.setServerAddress(ip, 7236);
            mClient.setStreamPath(path);
            mClient.startStream();
        }
    }

    @Override
    protected void stop() {

    }

    @Override
    protected Intent getNotificationIntent() {
        return null;
    }

    @Override
    protected void reset() {

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (mBinder == null) {
            mBinder = new ClientBinder();
        }
        return mBinder;
    }

    private void setServerIp(String serverIp) {
        mServerIp = serverIp;
    }

    private void setConfig(InputStream.Config config) {
        mAudioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                config.sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                1300,
                AudioTrack.MODE_STREAM
        );
    }

    @Override
    public void onBitrateUpdate(long bitrate) {

    }

    @Override
    public void onSessionError(int reason, int streamType, Exception e) {

    }

    @Override
    public void onSessionStarted() {

    }

    @Override
    public void onSessionStopped() {

    }

    @Override
    public void onRtspUpdate(int message, Exception exception) {

    }


    public class ClientBinder extends AbsMusicServiceBinder {

        @Override
        void play(Media media) {

        }

        @Override
        void pause() {

        }

        @Override
        void stop() {

        }

        public void setConfig(InputStream.Config config) {
            MusicClientService.this.setConfig(config);
        }

        public void setServerIp(String serverIp) {
            MusicClientService.this.setServerIp(serverIp);
        }
    }
}

