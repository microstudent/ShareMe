package com.leaves.app.shareme.service;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;


import com.google.gson.Gson;
import com.koushikdutta.async.http.AsyncHttpGet;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;
import com.leaves.app.shareme.Constant;
import com.leaves.app.shareme.bean.Message;
import com.leaves.app.shareme.bean.Media;
import com.leaves.app.shareme.ui.activity.MainActivity;

import net.majorkernelpanic.streaming.PlaytimeProvider;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.rtsp.RtspServer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Leaves on 2016/11/7.
 */

public class MusicServerService extends AbsMusicService implements WebSocket.StringCallback, PlaytimeProvider {
    private static final String TAG = "MusicServerService";
    public static final int SYNC_SIGNAL_OFFSET = 500;//同步信号发送的间隔，millsec
    private MediaPlayer mMediaPlayer = null;

    private CompositeDisposable mCompositeDisposable;

    private boolean isPrepared = false;
    private ServerBinder mBinder;

    private AsyncHttpServer mAsyncHttpServer;
    private WebSocket mConnectedWebSocket;
    private Gson mGson;
    private MusicPlayerListener mMusicPlayerListener;
    private Observable<Message<List<Long>>> mSyncSignalSender;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mCompositeDisposable == null || !mCompositeDisposable.isDisposed()) {
            mCompositeDisposable = new CompositeDisposable();
        }
        //先建立webSocket服务器，通知client连接
        mAsyncHttpServer = new AsyncHttpServer();
        mAsyncHttpServer.websocket(Constant.WebSocket.REGEX, null, new AsyncHttpServer.WebSocketRequestCallback() {
            @Override
            public void onConnected(WebSocket webSocket, AsyncHttpServerRequest request) {
                Log.d("MusicServerService", "server connect success");
                mConnectedWebSocket = webSocket;
                webSocket.setStringCallback(MusicServerService.this);
            }
        });
        mAsyncHttpServer.addAction(AsyncHttpGet.METHOD, "/cover", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                response.sendFile(new File(mMedia.getImage()));
            }
        });
        mAsyncHttpServer.listen(Constant.WebSocket.PORT);
        mSyncSignalSender = Observable.fromCallable(new Callable<Message<List<Long>>>() {
            @Override
            public Message<List<Long>> call() throws Exception {
                List<Long> times = new ArrayList<>(2);//ntp time and play time
                times.add(System.currentTimeMillis());
                times.add(getCurrentPlayTime());
                return new Message<>(Message.TYPE_SYNC, times);
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .repeat()
                .sample(SYNC_SIGNAL_OFFSET, TimeUnit.MILLISECONDS)
                .doOnNext(new Consumer<Message<List<Long>>>() {
                    @Override
                    public void accept(Message<List<Long>> message) throws Exception {
                        if (mConnectedWebSocket != null) {
                            mConnectedWebSocket.send(mGson.toJson(message));
                        }
                    }
                });
        return START_STICKY;
    }


    @Override
    protected void stop() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        stopForeground(true);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mGson = new Gson();
        if (mCompositeDisposable == null || !mCompositeDisposable.isDisposed()) {
            mCompositeDisposable = new CompositeDisposable();
        }
        mMediaPlayer = new MediaPlayer(); // initialize it here
//        mMediaPlayer.setOnPreparedListener(this);
//        mMediaPlayer.setOnErrorListener(this);
//        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        isPrepared = false;
        startRTSPServer();
    }

    @Override
    public void onStringAvailable(String s) {

    }

    @Override
    protected Intent getNotificationIntent() {
        return new Intent(this, MainActivity.class);
    }

    @Override
    protected void reset() {

    }

    private void resetMediaPlayer() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mMediaPlayer = new MediaPlayer();
        }
    }

    protected void pause() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
        }
        if (mConnectedWebSocket != null) {
            mConnectedWebSocket.send(mGson.toJson(new Message<>(Message.TYPE_PAUSE, null)));
        }
        if (mMusicPlayerListener != null) {
            mMusicPlayerListener.onMusicPause();
        }
    }

    @Override
    protected void start(boolean invalidate) {
        //notifyThe client
        if (mConnectedWebSocket != null) {
            if (invalidate) {
                mConnectedWebSocket.send(mGson.toJson(new Message<>(Message.TYPE_MEDIA, mMedia)));
            } else {
                mConnectedWebSocket.send(mGson.toJson(new Message<>(Message.TYPE_RESUME, null)));
            }
        }
        if (invalidate) {
            SessionBuilder.getInstance().setMp3Path(mMedia.getSrc());
            Uri uri = Uri.parse(mMedia.getSrc());
            Observable.just(uri)
                    .observeOn(Schedulers.io())
                    .subscribe(new Consumer<Uri>() {
                        @Override
                        public void accept(Uri uri) throws Exception {
                            resetMediaPlayer();
                            mMediaPlayer.setDataSource(MusicServerService.this, uri);
                            mMediaPlayer.prepare();
                            isPrepared = true;
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            onMusicPlayError(throwable);
                        }
                    });
            Observable.just(uri)
                    .observeOn(Schedulers.io())
                    .delay(Constant.DEFAULT_PLAY_TIME_DELAY, TimeUnit.MILLISECONDS)
                    .subscribe(new Consumer<Uri>() {
                        @Override
                        public void accept(Uri uri) throws Exception {
                            if (isPrepared) {
                                mMediaPlayer.start();
                                mSyncSignalSender.subscribe(new Consumer<Message<List<Long>>>() {
                                    @Override
                                    public void accept(Message<List<Long>> message) throws Exception {
                                    }
                                }, new Consumer<Throwable>() {
                                    @Override
                                    public void accept(Throwable throwable) throws Exception {
                                        Log.e(TAG, "accept: ", throwable);
                                    }
                                });
                            } else {
                                throw new Exception("the music player is not prepared!");
                            }
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            onMusicPlayError(throwable);
                        }
                    });
        } else {
            if (mMediaPlayer != null && isPrepared) {
                mMediaPlayer.start();
            }
        }
        if (mMusicPlayerListener != null) {
            mMusicPlayerListener.onMusicStart(mMedia);
        }
    }

    private void onMusicPlayError(Throwable throwable) {
        Observable.just("播放失败: " + throwable.toString())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        Toast.makeText(MusicServerService.this, s, Toast.LENGTH_SHORT).show();
                    }
                });
        throwable.printStackTrace();
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
        }
    }

    private void startRTSPServer() {
        // Sets the port of the RTSP server to 1234
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putString(RtspServer.KEY_PORT, String.valueOf(7236));
        editor.apply();
        // Configures the SessionBuilder
        SessionBuilder.getInstance()
                .setContext(getApplicationContext())
                .setVideoEncoder(SessionBuilder.VIDEO_NONE)
                .setAudioEncoder(SessionBuilder.AUDIO_MP3);

        // Starts the RTSP server
        this.startService(new Intent(this, RtspServer.class));
    }

//    private void playAsServer(Media media) {
//        if (media == null) {
//            return;
//        }
//        if (mMediaPlayer == null) {
//            initMediaPlayer();
//        }
//        if (mPlayingMedia != null && media.getSrc().equals(mPlayingMedia.getSrc()) && isPrepared) {
//            mMediaPlayer.start();
//            registerTimeSeek();
//            return;
//        }
//        mMediaPlayer.reset();
//        Uri uri = Uri.parse(media.getSrc());
//        try {
//            mMediaPlayer.setDataSource(this, uri);
//            mPlayingMedia = media;
//            mMediaPlayer.prepareAsync(); // prepare async to not block main thread
//            isPrepared = false;
//            mWifiLock.setReferenceCounted(false);
//            mWifiLock.acquire();
//
//            // assign the song name to songName
//            Intent intent = new Intent(this, RTSPActivity.class);
//            intent.putExtra(Constant.PLAY_TYPE, "");
//            PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
//                    intent,
//                    PendingIntent.FLAG_UPDATE_CURRENT);
//            Notification notification = new NotificationCompat.Builder(this)
//                    .setContentIntent(pi)
//                    .setLargeIcon(BitmapFactory.decodeFile(media.getImage()))
//                    .setSmallIcon(R.drawable.ic_notification)
//                    .setContentText(media.getTitle())
//                    .setSubText(media.getArtist())
//                    .build();
//            startForeground(22, notification);
//            registerTimeSeek();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (mBinder == null) {
            mBinder = new ServerBinder();
        }
        return mBinder;
    }


    @Override
    public long getCurrentPlayTime() {
        if (mMediaPlayer != null && isPrepared) {
            return mMediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    public class ServerBinder extends AbsMusicServiceBinder {

        @Override
        public void play(Media media, boolean invalidate) {
            MusicServerService.this.play(media, invalidate);
        }

        @Override
        public void pause() {
            MusicServerService.this.pause();
        }

        @Override
        public void stop() {
            MusicServerService.this.stop();
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
