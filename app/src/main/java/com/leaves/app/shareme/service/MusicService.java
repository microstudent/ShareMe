package com.leaves.app.shareme.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;


import com.leaves.app.shareme.Constant;
import com.leaves.app.shareme.R;
import com.leaves.app.shareme.RTSPActivity;
import com.leaves.app.shareme.bean.Media;
import com.leaves.app.shareme.eventbus.MediaEvent;
import com.leaves.app.shareme.eventbus.Message;
import com.leaves.app.shareme.eventbus.RxBus;
import com.leaves.app.shareme.eventbus.TimeSeekEvent;

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

public class MusicService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {
    public static final int ACTION_PLAY = 1;
    public static final int ACTION_STOP = 2;
    public static final int ACTION_PAUSE = 3;
    private MediaPlayer mMediaPlayer = null;

    private Disposable mTimeSeekDisposable;
    private CompositeDisposable mCompositeDisposable;

    private Media mPlayingMedia;

    private WifiManager.WifiLock mWifiLock;
    private Observable<Long> timeSeek;

    private boolean isPrepared = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mCompositeDisposable == null || !mCompositeDisposable.isDisposed()) {
            mCompositeDisposable = new CompositeDisposable();

            mCompositeDisposable.add(RxBus.getDefault().toFlowable(MediaEvent.class)
                    .subscribe(new Consumer<MediaEvent>() {
                        @Override
                        public void accept(MediaEvent mediaEvent) throws Exception {
                            switch (mediaEvent.getAction()) {
                                case ACTION_PLAY:
                                    play(mediaEvent.getMedia());
                                    break;
                                case ACTION_PAUSE:
                                    pause();
                                    break;
                                case ACTION_STOP:
                                    stop();
                            }
                        }
                    }));
            mCompositeDisposable.add(RxBus.getDefault().toFlowable(Message.class)
                    .subscribe(new Consumer<Message>() {
                        @Override
                        public void accept(Message message) throws Exception {
                            if (message.getTag() == Constant.TAG_QUERY_PLAYING_AUDIO) {
                                RxBus.getDefault().post(new Message(Constant.TAG_MEDIA, mPlayingMedia));
                            }
                        }
                    }));
        }
        Media media = (Media) intent.getSerializableExtra(Constant.MEDIA);
        if (media != null) {
            play(media);
        }
        return START_STICKY;
    }


    private void stop() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        unregisterTimeSeek();
        stopForeground(true);
    }

    private void pause() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
        }
        unregisterTimeSeek();
    }

    private void play(Media media) {
        if (media == null) {
            return;
        }
        if (mMediaPlayer == null) {
            initMediaPlayer();
        }
        if (mPlayingMedia != null && media.getSrc().equals(mPlayingMedia.getSrc()) && isPrepared) {
            mMediaPlayer.start();
            registerTimeSeek();
            return;
        }
        mMediaPlayer.reset();
        Uri uri = Uri.parse(media.getSrc());
        try {
            mMediaPlayer.setDataSource(this, uri);
            mPlayingMedia = media;
            mMediaPlayer.prepareAsync(); // prepare async to not block main thread
            isPrepared = false;
            mWifiLock.setReferenceCounted(false);
            mWifiLock.acquire();

            // assign the song name to songName
            Intent intent = new Intent(this, RTSPActivity.class);
            intent.putExtra(Constant.PLAY_TYPE, "");
            PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            Notification notification = new NotificationCompat.Builder(this)
                    .setContentIntent(pi)
                    .setLargeIcon(BitmapFactory.decodeFile(media.getImage()))
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentText(media.getTitle())
                    .setSubText(media.getArtist())
                    .build();
            startForeground(22, notification);
            registerTimeSeek();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initMediaPlayer() {
        mMediaPlayer = new MediaPlayer(); // initialize it here
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mWifiLock = ((WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");
        timeSeek = Observable.fromCallable(new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                if (mMediaPlayer != null) {
                    return (long) mMediaPlayer.getCurrentPosition();
                }
                return 0L;
            }
        }).observeOn(Schedulers.newThread()).delay(1, TimeUnit.SECONDS).repeat().observeOn(AndroidSchedulers.mainThread());
        isPrepared = false;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        isPrepared = true;
        mp.start();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Toast.makeText(this, "播放失败", Toast.LENGTH_SHORT).show();
        if (mp != null) {
            mp.reset();
        }
        unregisterTimeSeek();
        return true;
    }


    private void registerTimeSeek() {
        //只允许一个timeSeek
        unregisterTimeSeek();

        mTimeSeekDisposable = timeSeek.subscribe(new Consumer<Long>() {
            @Override
            public void accept(Long aLong) throws Exception {
                TimeSeekEvent e = new TimeSeekEvent(aLong, mPlayingMedia.getDuration());
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
    public void onDestroy() {
        super.onDestroy();
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        if (mWifiLock != null) {
            mWifiLock.release();
        }
        unregisterTimeSeek();
        if (mCompositeDisposable != null) {
            mCompositeDisposable.dispose();
        }
        isPrepared = false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        unregisterTimeSeek();
    }
}
