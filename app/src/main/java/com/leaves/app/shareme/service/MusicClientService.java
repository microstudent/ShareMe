package com.leaves.app.shareme.service;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;
import com.leaves.app.shareme.Constant;
import com.leaves.app.shareme.bean.Frame;
import com.leaves.app.shareme.bean.Media;

import net.majorkernelpanic.streaming.InputStream;
import net.majorkernelpanic.streaming.ReceiveSession;
import net.majorkernelpanic.streaming.rtcp.OnRTCPUpdateListener;
import net.majorkernelpanic.streaming.rtp.OnPCMDataAvailableListener;
import net.majorkernelpanic.streaming.rtsp.RtspClient;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.reactivex.Observable;
import io.reactivex.functions.BooleanSupplier;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Leaves on 2017/4/18.
 */

public class MusicClientService extends AbsMusicService implements RtspClient.Callback, OnPCMDataAvailableListener, OnRTCPUpdateListener, WebSocket.StringCallback {
    private static final String TAG = "MusicClientService";
    private AudioTrack mAudioTrack;
    private ClientBinder mBinder;
    private ReceiveSession mSession;
    private RtspClient mClient;
    private String mServerIp;
    private Queue<Frame> mFrameQueue;
    private boolean isPlaying = false;
    private volatile long mCurrentRtpTime;
    private InputStream.Config mConfig;
    private WebSocket mConnectedWebSocket;


    @Override
    public void onCreate() {
        super.onCreate();
        // Configures the SessionBuilder
        mSession = new ReceiveSession.Builder()
                .setAudioDecoder(ReceiveSession.Builder.AUDIO_AAC)
                .setVideoDecoder(ReceiveSession.Builder.VIDEO_NONE)
                .setRTCPListener(this)
                .setOnPCMDataAvailableListener(this)
                .build();
        mClient = new RtspClient();
        mClient.setSession(mSession);
        mClient.setCallback(this);

        mFrameQueue = new ConcurrentLinkedQueue<>();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            //先建立实例，等webSocket通知再start
            String serverIp = intent.getStringExtra(Constant.Intent.SERVER_IP);
            if (serverIp != null) {
                mClient.setServerAddress(serverIp, 7236);
                mClient.setStreamPath("");
                mServerIp = serverIp;
                tryConnectToWebSocketServer();
            }
        }
        return START_STICKY;
    }

    private void tryConnectToWebSocketServer() {
        //web socket
        AsyncHttpClient.getDefaultInstance().websocket("http://" + mServerIp + ":" + Constant.WebSocket.PORT, null,
                new AsyncHttpClient.WebSocketConnectCallback() {
                    @Override
                    public void onCompleted(Exception ex, WebSocket webSocket) {
                        if (ex != null) {
                            Log.d(TAG, "ex:" + ex.getMessage());
                            ex.printStackTrace();
                            return;
                        }
                        mConnectedWebSocket = webSocket;

                        webSocket.setStringCallback(MusicClientService.this);
                    }
                });
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
            mClient.startStream();
        }
        Observable.just(mFrameQueue)
                .observeOn(Schedulers.newThread())
                .repeatUntil(new BooleanSupplier() {
                    @Override
                    public boolean getAsBoolean() throws Exception {
                        return isPlaying;
                    }
                }).subscribe(new Consumer<Queue<Frame>>() {
            @Override
            public void accept(Queue<Frame> frames) throws Exception {
                Frame frame = frames.poll();
                if (frame != null) {
                    playSilentIfNeeded(frame.getRtpTime());
                    mAudioTrack.write(frame.getPCMData(), 0, frame.getPCMData().length);
                } else {
                    Log.e(TAG, "no buffer to playAsServer!");
                }
            }
        });
    }

    @Override
    protected void stop() {
        mClient.stopStream();
        if (mAudioTrack != null) {
            mAudioTrack.stop();
        }
    }

    @Override
    protected Intent getNotificationIntent() {
        return null;
    }

    @Override
    protected void reset() {

    }

    private void playSilentIfNeeded(long timeStamp) {
        if (mCurrentRtpTime == 0) {
            mCurrentRtpTime = timeStamp;
        }
        while (mCurrentRtpTime + 1 != timeStamp && timeStamp > mCurrentRtpTime) {
            try {
                Thread.sleep(1000000L / mConfig.sampleRate);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
//            Log.d("AACInputStream", "current = " + mCurrentTimeStamp + "timeStamp = " + timeStamp);
            mCurrentRtpTime++;
        }
        mCurrentRtpTime = timeStamp;
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (mBinder == null) {
            mBinder = new ClientBinder();
        }
        return mBinder;
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
        mConfig = config;
    }


    private void sync(long currentRTPTime) {
        while (mFrameQueue.peek() != null) {
            Frame frame = mFrameQueue.peek();
            if (frame.getRtpTime() > currentRTPTime) {
                //超前了，sleep一段时间再播放
                mCurrentRtpTime = currentRTPTime;
                break;
            } else {
                //落后了，将未来得及播放的帧出队列
                mFrameQueue.poll();
            }
        }
    }

    @Override
    public void onRtspUpdate(int message, Exception exception) {

    }

    @Override
    public void onPCMDataAvailable(long rtpTime, byte[] data) {
        Frame frame = new Frame();
        frame.setRtpTime(rtpTime);
        frame.setPCMData(data);
        mFrameQueue.add(frame);
    }

    @Override
    public void onRTCPUpdate(long ntpTime, long rtpTime) {

    }

    @Override
    public void onStringAvailable(String s) {

    }


    public class ClientBinder extends AbsMusicServiceBinder {

        @Override
        public void play(Media media) {

        }

        @Override
        public void pause() {

        }

        @Override
        public void stop() {

        }

        public void setConfig(InputStream.Config config) {
            MusicClientService.this.setConfig(config);
        }

        public void sync(long currentRTPTime) {
            MusicClientService.this.sync(currentRTPTime);
        }
    }
}

