package com.leaves.app.shareme.service;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;
import com.leaves.app.shareme.Constant;
import com.leaves.app.shareme.bean.Frame;
import com.leaves.app.shareme.bean.Media;
import com.leaves.app.shareme.eventbus.MediaEvent;
import com.leaves.app.shareme.eventbus.RxBus;

import net.majorkernelpanic.streaming.InputStream;
import net.majorkernelpanic.streaming.ReceiveSession;
import net.majorkernelpanic.streaming.rtcp.OnRTCPUpdateListener;
import net.majorkernelpanic.streaming.rtp.OnPCMDataAvailableListener;
import net.majorkernelpanic.streaming.rtsp.RtspClient;

import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Leaves on 2017/4/18.
 */

public class MusicClientService extends AbsMusicService implements Runnable, RtspClient.Callback, OnPCMDataAvailableListener, OnRTCPUpdateListener, WebSocket.StringCallback, AudioTrack.OnPlaybackPositionUpdateListener {
    private static final String TAG = "MusicClientService";
    public static final int ACTION_PLAY = 0;
    public static final int ACTION_PAUSE = 1;
    public static final int ACTION_STOP = 2;

    private static final boolean DEBUG = true;
    private static final long RETRY_DELAY = 3000;//3sec重试一次
    private static final long MIN_SYNC_DELAY = 10;//最小可忍受的延迟，millsec
    private static final long MAX_SYNC_DELAY = 60;//最大不可忍受延迟，millsec
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
    private long mDelay;
    private int mByteHasWrite;
    private int mBytePerSecond;
    private boolean needSync = true;

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
        mAudioTrack.setPlaybackPositionUpdateListener(this);
        mAudioTrack.setPositionNotificationPeriod(1);
        mConfig = config;
        mBytePerSecond = mConfig.sampleRate * mConfig.channelCount * 2; // 44100 samples, 2 channels, 2 bytes per sample
    }

    @Override
    public void onRtspUpdate(int message, Exception exception) {
        if (exception != null) {
            Toast.makeText(this, "RTSP无法连接至服务器，请重试！", Toast.LENGTH_SHORT).show();
            //通知websocket服务器
//            if (mConnectedWebSocket != null) {
//                mConnectedWebSocket.send("");
//            }
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
        //rtpTime换算
//        Log.d(TAG, "do sync ntp =  " + ntpTime + "millsec,while rtp ts = " + rtpTs + ",and current rtp ts = " + mCurrentRtpTime);
        mDelay = getCurrentPosition() - rtpTime;
        if (mDelay > MAX_SYNC_DELAY) {
            needSync = true;
        }
//        mSyncTimer.schedule(new SyncTask(rtpTime), new Date(ntpTime));
    }


    private long getCurrentPosition() {
        return (long) ((double) mByteHasWrite / mBytePerSecond * 1000L);
    }

    /**
     * 非主线程
     */
    @Override
    public void onStringAvailable(String s) {
        mMedia = mGson.fromJson(s, Media.class);
        start(true);
        MediaEvent event = new MediaEvent(ACTION_PLAY, mMedia);
        RxBus.getDefault().post(event);
    }

    @Override
    public void run() {
        while (!mPlayThread.isInterrupted()) {
            if (mAudioTrack != null && mAudioTrack.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
                mAudioTrack.play();
            }
            doSyncIfNeeded();
            Frame frame = mFrameQueue.poll();
            if (frame != null) {
                mByteHasWrite += frame.getPCMData().length;
//                playSilentIfNeeded(frame.getRtpTime());
                if (DEBUG) Log.d(TAG, "writing audio track, mCurrent = " + mCurrentRtpTime + ",timeStamp = " + frame.getRtpTime());
                mAudioTrack.write(frame.getPCMData(), 0, frame.getPCMData().length);
            } else {
                needSync = true;
//                Log.e(TAG, "no buffer to playAsServer!");
            }
        }
    }

    private void doSyncIfNeeded() {
        if (needSync) {
            if (mDelay > 0) {
                //播放进度比服务端快，沉睡一段时间以同步
                try {
                    Thread.sleep(mDelay);
                    Log.d(TAG, "sleep " + mDelay + " millsec for sync");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else if (mDelay < 0) {
                Log.d(TAG, "skipping " + mDelay + " millsec for sync");
                //播放速度比服务器慢，要快进
                int bytesToSkip = (int) (((double) mBytePerSecond * Math.abs(mDelay)) / 1000);
                while (bytesToSkip > 0) {
                    if (!mFrameQueue.isEmpty()) {
                        Frame frame = mFrameQueue.poll();
                        if (frame != null) {
                            mByteHasWrite += frame.getPCMData().length;
                            bytesToSkip -= frame.getPCMData().length;
                        }
                    }
                }
            }
            needSync = mDelay < MIN_SYNC_DELAY;
            mDelay = 0;
        }
    }

    @Override
    public void onMarkerReached(AudioTrack track) {

    }

    @Override
    public void onPeriodicNotification(AudioTrack track) {

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

        @Override
        public boolean isConnectionAlive() {
            return mConnectedWebSocket != null;
        }
    }

    private class SyncTask extends TimerTask {
        private final long mRtpTime;

        public SyncTask(long rtpTime) {
            mRtpTime = rtpTime;
        }

        @Override
        public void run() {
            mDelay = getCurrentPosition() - mRtpTime;
        }
    }
}

