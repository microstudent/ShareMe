package com.leaves.app.shareme.service;

import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.realm.RealmObject;

/**
 * Created by Leaves on 2017/4/18.
 */

public class MusicClientService extends AbsMusicService implements Runnable, RtspClient.Callback, OnPCMDataAvailableListener, WebSocket.StringCallback, CompletedCallback {
    private static final String TAG = "MusicClientService";

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
    private long mPlayDelay;
    private boolean needSync = true;
    private MusicPlayerListener mMusicPlayerListener;
    private long mInitDelay = -1;//服务器端与本地的ntp时间初始差距
    private long mInitRtpTime = -1;
    private double mMsPerAACFrame;
    private volatile long mPlayTimeToSync;
    private volatile long mPlayingRTPTime;
    private Disposable mPlayDisposable;
    private AudioManager mAudioManager;
    private float mLeftF = 1;
    private float mRightF = 1;

    @Override
    public void onCreate() {
        super.onCreate();
        mGson = new GsonBuilder()
                .setExclusionStrategies(new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes f) {
                        return f.getDeclaringClass().equals(RealmObject.class);
                    }

                    @Override
                    public boolean shouldSkipClass(Class<?> clazz) {
                        return false;
                    }
                }).create();
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
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
        super.pause();
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
        super.stop();
        mClient.stopStream();
        if (mAudioTrack != null) {
            mAudioTrack.stop();
        }
    }

    @Override
    public AbsMusicServiceBinder getBinder() {
        return mBinder;
    }

    @Override
    protected Intent getNotificationIntent() {
        return new Intent(this, MainActivity.class);
    }

    @Override
    protected void reset() {
        if (mPlayDisposable != null) {
            mPlayDisposable.dispose();
        }
        if (mPlayThread != null) {
            mPlayThread.interrupt();
        }
        if (mAudioTrack != null) {
            mAudioTrack.flush();
            mAudioTrack.release();
        }
        if (mClient != null) {
            mClient.release();
        }
        if (mFrameQueue != null) {
            mFrameQueue.clear();
        }
        mPlayDelay = 0;
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
        int minBufferSize = AudioTrack.getMinBufferSize(config.sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        mAudioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                config.sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize,
                AudioTrack.MODE_STREAM
        );
        mPlayDisposable = Observable.just(mAudioTrack)
                .subscribeOn(Schedulers.io())
                .delay(Constant.DEFAULT_PLAY_TIME_DELAY, TimeUnit.MILLISECONDS)
                .subscribe(new Consumer<AudioTrack>() {
                    @Override
                    public void accept(AudioTrack audioTrack) throws Exception {
                        audioTrack.play();
                    }
                });
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
                mInitDelay =  System.currentTimeMillis() - ntpTime - 100;
                mPlayDelay = getCurrentPosition() - playTime - 100;//100ms是对传输耗时的假设判断
            } else {
                mPlayDelay = getCurrentPosition() - (playTime + (System.currentTimeMillis() - mInitDelay - ntpTime));
            }
            mPlayTimeToSync = playTime + (System.currentTimeMillis() - mInitDelay - ntpTime);

            if (Math.abs(mPlayDelay) > MAX_SYNC_DELAY) {
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
//                    Log.d(TAG, "getFramePlayTime(frame):" + getFramePlayTime(frame));
//                    Log.d(TAG, "mPlayingRTPTime:" + mPlayingRTPTime);
//                playSilentIfNeeded(frame.getRtpTime());
                    mAudioTrack.write(frame.getPCMData(), 0, frame.getPCMData().length);
                } else {
                    needSync = true;
                }
            }
        }
    }

    private long getFramePlayTime(Frame frame) {
        if (frame != null) {
            return getFramePlayTime(frame.getRtpTime());
        }
        return 0;
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
            if (mPlayDelay > 0) {
                //播放进度比服务端快，沉睡一段时间以同步
                try {
                    Log.w(TAG, "sleep " + mPlayDelay + " millsec for sync");
                    Thread.sleep(mPlayDelay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else if (mPlayDelay < 0) {
                Log.w(TAG, "skipping " + mPlayDelay + " millsec for sync");
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
            needSync = Math.abs(mPlayDelay) > MAX_SYNC_DELAY;
            mPlayDelay = 0;
        }
    }

    @Override
    public int getLeftVolume() {
        return (int) (mLeftF * 100);
    }

    @Override
    public void setVolume(int left, int right) {
        if (left != -1) {
            mLeftF = (float) left / 100;
        }
        if (right != -1) {
            mRightF = (float) right / 100;
        }
        if (mAudioTrack != null) {
            mAudioTrack.setStereoVolume(mLeftF, mRightF);
        }
    }

    @Override
    public void moveToPrev() {

    }

    @Override
    public void moveToNext() {

    }

    @Override
    public int getRightVolume() {
        return (int) (mRightF * 100);
    }

    @Override
    public void onCompleted(Exception ex) {
        mConnectedWebSocket = null;
        tryConnectToWebSocketServer();
    }


    public class ClientBinder extends AbsMusicServiceBinder {

        @Override
        public void play(Media media, boolean invalidate) {
            MusicClientService.this.play(media, invalidate);
            if (mConnectedWebSocket != null) {
                Message<Media> msg;
                if (invalidate) {
                    msg = new Message<>(Message.TYPE_PLAY, media);
                } else {
                    msg = new Message<>(Message.TYPE_RESUME, media);
                }
                mConnectedWebSocket.send(mGson.toJson(msg));
            }
        }

        @Override
        public void pause() {
            MusicClientService.this.pause();
            if (mConnectedWebSocket != null) {
                Message<Object> pause = new Message<>(Message.TYPE_PAUSE, null);
                mConnectedWebSocket.send(mGson.toJson(pause));
            }
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
        public void moveToNext() {
            MusicClientService.this.moveToNext();
            if (mConnectedWebSocket != null) {
                Message<Object> next = new Message<>(Message.TYPE_NEXT, null);
                mConnectedWebSocket.send(mGson.toJson(next));
            }
        }

        @Override
        public void moveToPrev() {
            MusicClientService.this.moveToPrev();
            if (mConnectedWebSocket != null) {
                Message<Object> prev = new Message<>(Message.TYPE_PREV, null);
                mConnectedWebSocket.send(mGson.toJson(prev));
            }
        }

        @Override
        public void setMusicPlayerListener(MusicPlayerListener musicPlayerListener) {
            mMusicPlayerListener = musicPlayerListener;
        }

        @Override
        public boolean isBusy() {
            return false;
        }

        @Override
        public int getLeftVolume() {
            return MusicClientService.this.getLeftVolume();
        }

        @Override
        public int getRightVolume() {
            return MusicClientService.this.getRightVolume();
        }

        @Override
        public void setVolume(int left, int right) {
            MusicClientService.this.setVolume(left, right);
        }
    }
}

