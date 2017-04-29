package com.leaves.app.shareme.service;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;


import com.google.gson.Gson;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.leaves.app.shareme.Constant;
import com.leaves.app.shareme.bean.Frame;
import com.leaves.app.shareme.bean.Media;
import com.leaves.app.shareme.eventbus.RxBus;
import com.leaves.app.shareme.eventbus.TimeSeekEvent;
import com.leaves.app.shareme.ui.activity.MainActivity;

import net.majorkernelpanic.streaming.PlaytimeProvider;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.rtsp.RtspServer;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Leaves on 2016/11/7.
 */

public class MusicServerService extends AbsMusicService implements WebSocket.StringCallback, PlaytimeProvider {
    private MediaPlayer mMediaPlayer = null;

    private Disposable mTimeSeekDisposable;
    private CompositeDisposable mCompositeDisposable;


    private Observable<Long> timeSeek;

    private boolean isPrepared = false;
    private ServerBinder mBinder;

    private AsyncHttpServer mWebSocketServer;
    private WebSocket mConnectedWebSocket;
    private Gson mGson;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mCompositeDisposable == null || !mCompositeDisposable.isDisposed()) {
            mCompositeDisposable = new CompositeDisposable();
        }
        //先建立webSocket服务器，通知client连接
        mWebSocketServer = new AsyncHttpServer();
        mWebSocketServer.websocket(Constant.WebSocket.REGEX, null, new AsyncHttpServer.WebSocketRequestCallback() {
            @Override
            public void onConnected(WebSocket webSocket, AsyncHttpServerRequest request) {
                Log.d("MusicServerService", "server connect success");
                mConnectedWebSocket = webSocket;
                webSocket.setStringCallback(MusicServerService.this);
            }
        });
        mWebSocketServer.listen(Constant.WebSocket.PORT);
        Log.d("MusicServerService", "listen on" + Constant.WebSocket.PORT);
        return START_STICKY;
    }


    @Override
    protected void stop() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        unregisterTimeSeek();
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
        timeSeek = Observable.fromCallable(new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                if (mMediaPlayer != null) {
                    return (long) mMediaPlayer.getCurrentPosition() * 1000L;
                }
                return 0L;
            }
        }).observeOn(Schedulers.newThread()).repeat().observeOn(AndroidSchedulers.mainThread());
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

    protected void pause() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
        }
        unregisterTimeSeek();
    }

    @Override
    protected void start(boolean invalidate) {
        //notifyThe client
        if (mConnectedWebSocket != null) {
            mConnectedWebSocket.send(mGson.toJson(mMedia));
        }
        if (invalidate) {
            mMediaPlayer.reset();
            SessionBuilder.getInstance().setMp3Path(mMedia.getSrc());
            Uri uri = Uri.parse(mMedia.getSrc());
            Observable.just(uri)
                    .observeOn(Schedulers.io())
                    .subscribe(new Consumer<Uri>() {
                        @Override
                        public void accept(Uri uri) throws Exception {
                            mMediaPlayer.setDataSource(MusicServerService.this, uri);
                            mMediaPlayer.prepare();
                            isPrepared = true;
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            throwable.printStackTrace();
                            Toast.makeText(MusicServerService.this, "播放失败", Toast.LENGTH_SHORT).show();
                            if (mMediaPlayer != null) {
                                mMediaPlayer.reset();
                            }
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
                            }
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            throwable.printStackTrace();
                        }
                    });
        } else {
            if (mMediaPlayer != null && isPrepared) {
                mMediaPlayer.start();
            }
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
                .setAudioEncoder(SessionBuilder.AUDIO_MP3)
                .setPlaytimeProvider(this);

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

    private void registerTimeSeek() {
        //只允许一个timeSeek
        unregisterTimeSeek();

        mTimeSeekDisposable = timeSeek.subscribe(new Consumer<Long>() {
            @Override
            public void accept(Long aLong) throws Exception {
                TimeSeekEvent e = new TimeSeekEvent(aLong, mMedia.getDuration());
                RxBus.getDefault().post(e);
            }
        });
    }

    private void unregisterTimeSeek() {
        if (mTimeSeekDisposable != null) {
            mTimeSeekDisposable.dispose();
        }
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
        public void play(Media media) {
            if (mMedia != null && mMedia.getSrc().equals(media.getSrc())) {
                MusicServerService.this.play(media, false);
            } else {
                MusicServerService.this.play(media, true);
            }
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
    }
}
