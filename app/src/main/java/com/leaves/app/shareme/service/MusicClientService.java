package com.leaves.app.shareme.service;

import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;
import com.leaves.app.shareme.Constant;
import com.leaves.app.shareme.bean.Message;
import com.leaves.app.shareme.bean.Frame;
import com.leaves.app.shareme.bean.Media;
import com.leaves.app.shareme.gson.GsonUtils;
import com.leaves.app.shareme.ui.activity.MainActivity;

import net.majorkernelpanic.streaming.InputStream;
import net.majorkernelpanic.streaming.ReceiveSession;
import net.majorkernelpanic.streaming.rtp.OnPCMDataAvailableListener;
import net.majorkernelpanic.streaming.rtsp.RtspClient;

import java.util.List;
import java.util.Queue;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Leaves on 2017/4/18.
 */

public class MusicClientService extends AbsMusicService implements Runnable, RtspClient.Callback, OnPCMDataAvailableListener, WebSocket.StringCallback, AudioTrack.OnPlaybackPositionUpdateListener, CompletedCallback {
    private static final String TAG = "MusicClientService";
    public static final int ACTION_PLAY = 0;
    public static final int ACTION_PAUSE = 1;
    public static final int ACTION_STOP = 2;

    private static final boolean DEBUG = false;
    private static final long RETRY_DELAY = 3000;//3sec重试一次
    private static final long MAX_SYNC_DELAY = 25;//最大不可忍受延迟，millsec
    private AudioTrack mAudioTrack;
    private ClientBinder mBinder;
    private ReceiveSession mSession;
    private RtspClient mClient;
    private String mServerIp;
    private Queue<Frame> mFrameQueue;
    private InputStream.Config mConfig;
    private WebSocket mConnectedWebSocket;
    private Gson mGson;
    private Thread mPlayThread;
    private long mDelay;
    private boolean needSync = true;
    private MusicPlayerListener mMusicPlayerListener;
    private long mInitDelay = -1;//服务器端与本地的ntp时间初始差距
    private long mInitRtpTime = -1;
    private double mMsPerAACFrame;
    private volatile long mPlayTimeToSync;
    private volatile long mPlayingRTPTime;

    @Override
    public void onCreate() {
        super.onCreate();
        mGson = new Gson();
        // Configures the SessionBuilder
        init();
    }

    private void init() {
        mSession = new ReceiveSession.Builder()
                .setAudioDecoder(ReceiveSession.Builder.AUDIO_AAC)
                .setVideoDecoder(ReceiveSession.Builder.VIDEO_NONE)
                .setOnPCMDataAvailableListener(this)
                .build();
        mClient = new RtspClient();
        mClient.setSession(mSession);
        mClient.setCallback(this);
        mPlayThread = new Thread(this);
        mPlayThread.setName("PlayThread");

        mFrameQueue = new ConcurrentLinkedQueue<>();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            //先建立实例，等webSocket通知再start
            String serverIp = intent.getStringExtra(Constant.Intent.SERVER_IP);
            if (serverIp != null) {
                mServerIp = serverIp;
                mClient.setServerAddress(mServerIp, 7236);
                mClient.setStreamPath("");
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
                            Log.e(TAG, "ex:" + ex.getMessage());
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
                        mConnectedWebSocket.setClosedCallback(MusicClientService.this);
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
            mAudioTrack.flush();
        }
        if (mMusicPlayerListener != null) {
            mMusicPlayerListener.onMusicPause();
        }
    }

    @Override
    protected void start(boolean invalidate) {
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
        if (!mPlayThread.isAlive()) {
            mPlayThread.start();
        }
        if (mAudioTrack != null && !invalidate) {
            mAudioTrack.play();
        }
        if (mMusicPlayerListener != null) {
            mMusicPlayerListener.onMusicStart(mMedia);
        }
        needSync = true;
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
        return new Intent(this, MainActivity.class);
    }

    @Override
    protected void reset() {
        if (mPlayThread != null) {
            mPlayThread.interrupt();
        }
        if (mAudioTrack != null) {
            mAudioTrack.flush();
            mAudioTrack.stop();
            mAudioTrack.release();
            mAudioTrack = null;
        }
        if (mClient != null) {
            mClient.release();
        }
        if (mFrameQueue != null) {
            mFrameQueue.clear();
        }
        mDelay = 0;
        mInitDelay = -1;
        init();
        mClient.setServerAddress(mServerIp, 7236);
        mClient.setStreamPath("");
    }

//    private void playSilentIfNeeded(long timeStamp) {
//        if (mCurrentRtpTime == 0) {
//            mCurrentRtpTime = timeStamp;
//        }
//        while (mCurrentRtpTime + 1 != timeStamp && timeStamp > mCurrentRtpTime) {
//            try {
//                Log.d(TAG, "sleep for sync, mCurrent = " + mCurrentRtpTime + ",timeStamp = " + timeStamp);
//                Thread.sleep(mSleepTimeout);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            mCurrentRtpTime++;
//        }
//        mCurrentRtpTime = timeStamp;
//    }


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
        Observable.just(mAudioTrack)
                .subscribeOn(Schedulers.io())
                .delay(Constant.DEFAULT_PLAY_TIME_DELAY, TimeUnit.MILLISECONDS)
                .subscribe(new Consumer<AudioTrack>() {
                    @Override
                    public void accept(AudioTrack audioTrack) throws Exception {
                        audioTrack.play();
                    }
                });
        mAudioTrack.setPlaybackPositionUpdateListener(this);
        mAudioTrack.setPositionNotificationPeriod(1);
        mConfig = config;
        mMsPerAACFrame = (double) 1024 * 1000 / mConfig.sampleRate;
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
        if (DEBUG) Log.d(TAG, "onPCMDataAvailable" + ",timeStamp = " + rtpTime);;
    }


    public void onSyncDataAvailable(long ntpTime, long playTime) {
        //rtpTime换算
//        Log.d(TAG, "do sync ntp =  " + ntpTime + "millsec,while rtp ts = " + rtpTs + ",and current rtp ts = " + mCurrentRtpTime);

        if (playTime > 0) {
            if (mInitDelay == -1) {
                mInitDelay =  System.currentTimeMillis() - ntpTime - 10;
                mDelay = getCurrentPosition() - playTime - 10;//10ms是对传输耗时的假设判断
            } else {
                mDelay = getCurrentPosition() - (playTime + (System.currentTimeMillis() - mInitDelay - ntpTime));
            }
            mPlayTimeToSync = playTime + (System.currentTimeMillis() - mInitDelay - ntpTime);

            if (Math.abs(mDelay) > MAX_SYNC_DELAY) {
                needSync = true;
            }
        }
//        mSyncTimer.schedule(new SyncTask(rtpTime), new Date(ntpTime));
    }


    private long getCurrentPosition() {
        return getFramePlayTime(mPlayingRTPTime);
//        return (long) ((double) mByteHasWrite / mBytePerSecond * 1000L);
    }

    /**
     * 非主线程
     */
    @Override
    public void onStringAvailable(String s) {
        handleMessage(s);
    }

    private void handleMessage(String s) {
        JsonObject object = mGson.fromJson(s, JsonObject.class);
        JsonElement type = object.get("type");
        if (type != null) {
            switch (type.getAsInt()) {
                case Message.TYPE_MEDIA:
                    Message<Media> mediaMessage = GsonUtils.fromJsonObject(mGson, s, Media.class);
                    if (mediaMessage != null) {
                        mMedia = mediaMessage.getObject();
                        mMedia.setImage("http://" + mServerIp + ":" + Constant.WebSocket.PORT + "/cover");
                        play(mMedia, true);
                    }
                    break;
                case Message.TYPE_PAUSE:
                    pause();
                    break;
                case Message.TYPE_RESUME:
                    play(mMedia, false);
                    break;
                case Message.TYPE_SYNC:
                    Message<List<Long>> syncMessage = GsonUtils.fromJsonArray(mGson, s, Long.class);
                    List<Long> times = syncMessage.getObject();
                    long ntpTime = times.get(0);
                    long playTime = times.get(1);
                    onSyncDataAvailable(ntpTime, playTime);
                    break;
                case Message.TYPE_LIST:
                    Message<List<Media>> listMessage = GsonUtils.fromJsonArray(mGson, s, Media.class);
                    Constant.sPlayList = listMessage.getObject();
                    break;
            }
        }
    }

    @Override
    public void run() {
        while (!mPlayThread.isInterrupted()) {
            if (mAudioTrack != null && mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                doSyncIfNeeded();
                Frame frame = mFrameQueue.poll();
                if (frame != null) {
                    mPlayingRTPTime = frame.getRtpTime();
                    Log.d(TAG, "getFramePlayTime(frame):" + getFramePlayTime(frame));
                    Log.d(TAG, "mPlayingRTPTime:" + mPlayingRTPTime);
//                playSilentIfNeeded(frame.getRtpTime());
                    mAudioTrack.write(frame.getPCMData(), 0, frame.getPCMData().length);
                } else {
                    needSync = true;
                }
            }
        }
    }

    private long getFramePlayTime(@NonNull Frame frame) {
        return getFramePlayTime(frame.getRtpTime());
    }

    private long getFramePlayTime(long rtpTime) {
//        if (mInitRtpTime == -1) {
//            mInitRtpTime = rtpTime;
//            return 0;
//        } else {
        long duration = rtpTime;
        return (long) (mMsPerAACFrame * duration);
//        }
    }

    private void doSyncIfNeeded() {
        if (needSync) {
            if (mDelay > 0) {
                //播放进度比服务端快，沉睡一段时间以同步
                try {
                    Log.w(TAG, "sleep " + mDelay + " millsec for sync");
                    Thread.sleep(mDelay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else if (mDelay < 0) {
                Log.w(TAG, "skipping " + mDelay + " millsec for sync");
                //播放速度比服务器慢，要快进
                Frame frame;
                while (true) {
                    if (!mFrameQueue.isEmpty()) {
                        frame = mFrameQueue.peek();
                        if (getFramePlayTime(frame) < mPlayTimeToSync) {
                            mFrameQueue.poll();
                        } else {
                            break;
                        }
                    }
                }
            }
            needSync = Math.abs(mDelay) > MAX_SYNC_DELAY;
            mDelay = 0;
        }
    }

    @Override
    public void onMarkerReached(AudioTrack track) {

    }

    @Override
    public void onPeriodicNotification(AudioTrack track) {

    }

    @Override
    public void onCompleted(Exception ex) {
        tryConnectToWebSocketServer();
    }


    public class ClientBinder extends AbsMusicServiceBinder {

        @Override
        public void play(Media media, boolean invalidate) {
            MusicClientService.this.play(media, invalidate);
        }

        @Override
        public void pause() {

        }

        @Override
        public void stop() {
            MusicClientService.this.reset();
        }

        @Override
        public boolean isConnectionAlive() {
            return mConnectedWebSocket != null;
        }

        @Override
        public void setMusicPlayerListener(MusicPlayerListener musicPlayerListener) {
            mMusicPlayerListener = musicPlayerListener;
        }
    }
}

