package com.leaves.app.shareme.service;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.ArrayMap;
import android.util.Log;
import android.util.LongSparseArray;
import android.widget.Toast;

import com.google.gson.Gson;
import com.koushikdutta.async.future.Future;
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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.BooleanSupplier;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Leaves on 2017/4/18.
 */

public class MusicClientService extends AbsMusicService implements Runnable, RtspClient.Callback, OnPCMDataAvailableListener, OnRTCPUpdateListener, WebSocket.StringCallback {
    private static final String TAG = "MusicClientService";
    private static final boolean DEBUG = true;
    private static final long RETRY_DELAY = 3000;//3sec重试一次
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
    private Gson mGson;
    private Thread mPlayThread;
    private long mSleepTimeout;
    private Timer mSyncTimer;

    @Override
    public void onCreate() {
        super.onCreate();
        mGson = new Gson();
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
        mPlayThread = new Thread(this);
        mPlayThread.setName("playThread");

        mFrameQueue = new ConcurrentLinkedQueue<>();
        mSyncTimer = new Timer("syncTimer");
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
                            try {
                                Thread.sleep(RETRY_DELAY);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            tryConnectToWebSocketServer();
                            return;
                        }
                        mConnectedWebSocket = webSocket;
                        Observable.just(this)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Consumer<AsyncHttpClient.WebSocketConnectCallback>() {
                                    @Override
                                    public void accept(AsyncHttpClient.WebSocketConnectCallback webSocketConnectCallback) throws Exception {
                                        Toast.makeText(MusicClientService.this, "连接至WebSocket服务器成功", Toast.LENGTH_SHORT).show();
                                    }
                                });
                        webSocket.setStringCallback(MusicClientService.this);
                    }
                });
    }

    @Override
    protected void pause() {
        if (mAudioTrack != null) {
            mAudioTrack.pause();
            isPlaying = false;
        }
    }

    @Override
    protected void start(boolean invalidate) {
        isPlaying = true;
        if (!mClient.isStreaming()) {
            Observable.just(mClient)
                    .subscribeOn(Schedulers.single())
                    .delay(10, TimeUnit.MILLISECONDS)
                    .subscribe(new Consumer<RtspClient>() {
                        @Override
                        public void accept(RtspClient rtspClient) throws Exception {
                            mClient.startStream();
                        }
                    });
        }
        mPlayThread.start();
    }

    @Override
    protected void stop() {
        mClient.stopStream();
        if (mAudioTrack != null) {
            mAudioTrack.stop();
            isPlaying = false;
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
                Log.d(TAG, "sleep for sync, mCurrent = " + mCurrentRtpTime + ",timeStamp = " + timeStamp);
                Thread.sleep(mSleepTimeout);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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
        mSleepTimeout = 1000000L / config.sampleRate;
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
        if (exception != null) {
            Toast.makeText(this, "RTSP无法连接至服务器，请重试！", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onConfigured(InputStream.Config config) {
        setConfig(config);
    }

    @Override
    public void onPCMDataAvailable(long rtpTime, byte[] data) {
        Frame frame = new Frame();
        frame.setRtpTime(rtpTime);
        frame.setPCMData(data);
        mFrameQueue.add(frame);
        if (DEBUG) Log.d(TAG, "onPCMDataAvailable" + ",timeStamp = " + rtpTime);
    }

    @Override
    public void onRTCPUpdate(long ntpTime, long rtpTime) {
//        mSyncTimer.schedule(new SyncTask(rtpTime), new Date(ntpTime));
    }

    /**
     * 非主线程
     */
    @Override
    public void onStringAvailable(String s) {
        mMedia = mGson.fromJson(s, Media.class);
        start(true);
    }

    @Override
    public void run() {
        while (!mPlayThread.isInterrupted()) {
            if (mAudioTrack != null && mAudioTrack.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
                mAudioTrack.play();
            }
            Frame frame = mFrameQueue.poll();
            if (frame != null) {
                playSilentIfNeeded(frame.getRtpTime());
                if (DEBUG) Log.d(TAG, "writing audio track, mCurrent = " + mCurrentRtpTime + ",timeStamp = " + frame.getRtpTime());
                mAudioTrack.write(frame.getPCMData(), 0, frame.getPCMData().length);
            } else {
//                Log.e(TAG, "no buffer to playAsServer!");
            }
        }
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

        public Media getCurrentPlayMedia() {
            return mMedia;
        }


        public void sync(long currentRTPTime) {
            MusicClientService.this.sync(currentRTPTime);
        }
    }

    private class SyncTask extends TimerTask {
        private final long mRtpTime;

        public SyncTask(long rtpTime) {
            mRtpTime = rtpTime;
        }

        @Override
        public void run() {
            sync(mRtpTime);
        }
    }
}

