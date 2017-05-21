package com.leaves.app.shareme.service;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;


import com.devbrackets.android.exomedia.AudioPlayer;
import com.devbrackets.android.exomedia.listener.OnCompletionListener;
import com.devbrackets.android.exomedia.util.DeviceUtil;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.koushikdutta.async.http.AsyncHttpGet;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;
import com.leaves.app.shareme.Constant;
import com.leaves.app.shareme.bean.Message;
import com.leaves.app.shareme.bean.Media;
import com.leaves.app.shareme.contract.AudioListContract;
import com.leaves.app.shareme.gson.GsonUtils;
import com.leaves.app.shareme.presenter.AudioListPresenter;
import com.leaves.app.shareme.ui.activity.MainActivity;

import net.majorkernelpanic.streaming.PlaytimeProvider;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.rtsp.RtspServer;

import java.io.File;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.realm.RealmObject;

/**
 * Created by Leaves on 2016/11/7.
 */

public class MusicServerService extends AbsMusicService implements WebSocket.StringCallback, PlaytimeProvider, AudioListContract.View, OnCompletionListener {
    private static final String TAG = "MusicServerService";
    public static final int SYNC_SIGNAL_OFFSET = 500;//同步信号发送的间隔，millsec
    private AudioPlayer mAudioPlayer = null;

    private CompositeDisposable mCompositeDisposable;

    private boolean isPrepared = false;
    private ServerBinder mBinder;

    private AsyncHttpServer mAsyncHttpServer;
    private WebSocket mConnectedWebSocket;
    private Gson mGson;
    private MusicPlayerListener mMusicPlayerListener;
    private Observable<Message<List<Long>>> mSyncSignalSender;
    private AudioListPresenter mAudioListPresenter;
    private List<Media> mAudioList;
    private int mPlayMediaIndex = -1;
    private Disposable mSyncDisposable;
    private boolean isBusy;
    private float mLeftF = 1;
    private float mRightF = 1;

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
                sendAudioList();
                webSocket.setStringCallback(MusicServerService.this);
            }
        });
        mAsyncHttpServer.addAction(AsyncHttpGet.METHOD, "/cover", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                if (!TextUtils.isEmpty(mMedia.getImage())) {
                    File file = new File(mMedia.getImage());
                    if (file.exists()) {
                        response.sendFile(file);
                    }
                }
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
        mAudioListPresenter = new AudioListPresenter(this, this);
        mAudioListPresenter.start();
        return START_STICKY;
    }

    private void sendAudioList() {
        if (mConnectedWebSocket != null && mAudioList != null) {
            Message<List<Media>> message = new Message<>(Message.TYPE_LIST, mAudioList);
            mConnectedWebSocket.send(mGson.toJson(message));
        }
    }


    @Override
    protected void stop() {
        super.stop();
        if (mAudioPlayer != null) {
            mAudioPlayer.release();
            mAudioPlayer = null;
        }
        stopForeground(true);
    }

    @Override
    public AbsMusicServiceBinder getBinder() {
        return mBinder;
    }

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
        if (mCompositeDisposable == null || !mCompositeDisposable.isDisposed()) {
            mCompositeDisposable = new CompositeDisposable();
        }
        mAudioPlayer = new AudioPlayer(this); // initialize it here
        mAudioPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mAudioPlayer.setOnCompletionListener(this);
//        mAudioPlayer.setOnPreparedListener(this);
//        mAudioPlayer.setOnErrorListener(this);
//        mAudioPlayer.setOnCompletionListener(this);
        startRTSPServer();
    }

    @Override
    public void onStringAvailable(String s) {
        JsonObject object = mGson.fromJson(s, JsonObject.class);
        JsonElement type = object.get("type");
        if (type != null) {
            switch (type.getAsInt()) {
                case Message.TYPE_MEDIA:
                    //服务器端不接收此类消息
                    break;
                case Message.TYPE_PAUSE:
                    pause();
                    break;
                case Message.TYPE_RESUME:
                    play(mMedia, false);
                    break;
                case Message.TYPE_SYNC:
                    //服务器端不接受此类消息
                    break;
                case Message.TYPE_LIST:
                    //服务器端不接受此类消息
                    break;
                case Message.TYPE_NEXT:
                    moveToNext();
                    break;
                case Message.TYPE_PREV:
                    moveToPrev();
                    break;
                case Message.TYPE_PLAY:
                    Message<Media> mediaMessage = GsonUtils.fromJsonObject(mGson, s, Media.class);
                    play(mediaMessage.getObject(), true);
                    break;
            }
        }
    }

    @Override
    protected Intent getNotificationIntent() {
        return new Intent(this, MainActivity.class);
    }

    @Override
    protected void reset() {
        isPrepared = false;
        if (mSyncDisposable != null) {
            mSyncDisposable.dispose();
        }
    }




    private void resetMediaPlayer() {
        mAudioPlayer.stopPlayback();
        mAudioPlayer.reset();
        isPrepared = false;
    }

    protected void pause() {
        super.pause();
        if (mAudioPlayer != null && mAudioPlayer.isPlaying()) {
            mAudioPlayer.pause();
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
            isBusy = true;
            SessionBuilder.getInstance().setMp3Path(mMedia.getSrc());
            Uri uri = Uri.parse(mMedia.getSrc());
            mCompositeDisposable.add(Observable.just(uri)
                    .observeOn(Schedulers.io())
                    .subscribe(new Consumer<Uri>() {
                        @Override
                        public void accept(Uri uri) throws Exception {
                            resetMediaPlayer();
                            mAudioPlayer.setDataSource(uri);
//                            mAudioPlayer.setDataSource(MusicServerService.this, uri);
                            isPrepared = true;
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            onMusicPlayError(throwable);
                        }
                    }));
            mCompositeDisposable.add(Observable.just(uri)
                    .observeOn(Schedulers.io())
                    .delay(Constant.DEFAULT_PLAY_TIME_DELAY, TimeUnit.MILLISECONDS)
                    .subscribe(new Consumer<Uri>() {
                        @Override
                        public void accept(Uri uri) throws Exception {
                            if (isPrepared) {
                                mAudioPlayer.start();
                                mSyncDisposable = mSyncSignalSender.subscribe(new Consumer<Message<List<Long>>>() {
                                    @Override
                                    public void accept(Message<List<Long>> message) throws Exception {
                                    }
                                }, new Consumer<Throwable>() {
                                    @Override
                                    public void accept(Throwable throwable) throws Exception {
                                        Log.e(TAG, "accept: ", throwable);
                                    }
                                });
                                isBusy = false;
                            } else {
                                throw new Exception("the music player is not prepared!");
                            }
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            onMusicPlayError(throwable);
                        }
                    }));
        } else {
            if (mAudioPlayer != null && isPrepared) {
                mAudioPlayer.start();
            }
        }
        if (mMusicPlayerListener != null) {
            mMusicPlayerListener.onMusicStart(mMedia);
        }
        if (mAudioList != null) {
            for (int i = 0; i < mAudioList.size(); i++) {
                Media media = mAudioList.get(i);
                if (media.getSrc().equals(mMedia.getSrc())) {
                    mPlayMediaIndex = i;
                    break;
                }
            }
        }
    }

    private void onMusicPlayError(Throwable throwable) {
        mCompositeDisposable.add(Observable.just("播放失败: " + throwable.toString())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        Toast.makeText(MusicServerService.this, s, Toast.LENGTH_SHORT).show();
                    }
                }));
        throwable.printStackTrace();
        if (mAudioPlayer != null) {
            mAudioPlayer.reset();
        }
        isBusy = false;
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
//        if (mAudioPlayer == null) {
//            initMediaPlayer();
//        }
//        if (mPlayingMedia != null && media.getSrc().equals(mPlayingMedia.getSrc()) && isBusy) {
//            mAudioPlayer.start();
//            registerTimeSeek();
//            return;
//        }
//        mAudioPlayer.reset();
//        Uri uri = Uri.parse(media.getSrc());
//        try {
//            mAudioPlayer.setDataSource(this, uri);
//            mPlayingMedia = media;
//            mAudioPlayer.prepareAsync(); // prepare async to not block main thread
//            isBusy = false;
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
    public void moveToNext() {
        if (isBusy || mPlayMediaIndex == -1) {
            return;
        }
        if (mPlayMediaIndex >= 0 && mAudioList != null && mPlayMediaIndex < mAudioList.size()) {
            if (mAudioList.size() == 1) {
                play(mAudioList.get(0), true);
            } else {
                play(mAudioList.get(mPlayMediaIndex + 1 % (mAudioList.size() - 1)), true);
            }
        }
    }

    @Override
    public void moveToPrev() {
        if (isBusy || mPlayMediaIndex == -1) {
            return;
        }
        if (mAudioList != null && !mAudioList.isEmpty()) {
            play(mAudioList.get(mPlayMediaIndex == 0 ? mPlayMediaIndex - 1 : mAudioList.size() - 1 % mAudioList.size()), true);
        }
    }


    @Override
    public long getCurrentPlayTime() {
        if (mAudioPlayer != null && isPrepared) {
            return mAudioPlayer.getCurrentPosition();
        }
        return 0;
    }

    @Override
    public void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mAudioListPresenter.onDestroy();
    }

    @Override
    public int getLeftVolume() {
        return (int) (mLeftF * 100);
    }

    @Override
    public int getRightVolume() {
        return (int) (mRightF * 100);
    }

    @Override
    public void setVolume(int left, int right) {
        if (left != -1) {
            mLeftF = (float) left / 100;
        }
        if (right != -1) {
            mRightF = (float) right / 100;
        }
        if (mAudioPlayer != null) {
            mAudioPlayer.setVolume(mLeftF, mRightF);
        }
    }

    @Override
    public void setData(List<Media> medias) {
        mAudioList = medias;
    }

    @Override
    public void onCompletion() {
        //播放下一首
        moveToNext();
    }

    public class ServerBinder extends AbsMusicServiceBinder {

        @Override
        public void play(Media media, boolean invalidate) {
            MusicServerService.this.play(media, invalidate);
        }

        @Override
        public void pause() {
            MusicServerService.this.pause();
            if (mConnectedWebSocket != null) {
                mConnectedWebSocket.send(mGson.toJson(new Message<>(Message.TYPE_PAUSE, null)));
            }
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
        public void moveToNext() {
            MusicServerService.this.moveToNext();
        }

        @Override
        public void moveToPrev() {
            MusicServerService.this.moveToPrev();
        }

        @Override
        public void setMusicPlayerListener(MusicPlayerListener musicPlayerListener) {
            mMusicPlayerListener = musicPlayerListener;
        }

        @Override
        public boolean isBusy() {
            return isBusy;
        }

        @Override
        public int getLeftVolume() {
            return MusicServerService.this.getLeftVolume();
        }

        @Override
        public int getRightVolume() {
            return MusicServerService.this.getRightVolume();
        }

        @Override
        public void setVolume(int left, int right) {
            MusicServerService.this.setVolume(left,right);
        }
    }
}
