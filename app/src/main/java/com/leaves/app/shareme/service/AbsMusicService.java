package com.leaves.app.shareme.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.wifi.WifiManager;
import android.os.RemoteException;
import android.widget.RemoteViews;

import com.leaves.app.shareme.R;
import com.leaves.app.shareme.bean.Media;
import com.leaves.app.shareme.receiver.MediaNotificationManager;

import androidx.core.app.NotificationCompat;
import io.reactivex.disposables.CompositeDisposable;

/**
 * Created by Leaves on 2017/4/17.
 */

public abstract class AbsMusicService extends Service {
    public static final int MODE_LOCAL = 0;
    public static final int MODE_REMOTE = 1;

    public final static int STATE_NONE = 0;
    public final static int STATE_STOPPED = 1;
    public final static int STATE_PAUSED = 2;
    public final static int STATE_PLAYING = 3;
    public final static int STATE_ERROR = 7;

    protected MediaNotificationManager mNotificationManager;

    private int mMode;
    private WifiManager.WifiLock mWifiLock;
    protected Media mMedia;

    protected CompositeDisposable mCompositeDisposable;

    protected int mState;

    @Override
    public void onCreate() {
        super.onCreate();
        mWifiLock = ((WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "musicLock");
        mCompositeDisposable = new CompositeDisposable();
        try {
            mNotificationManager = new MediaNotificationManager(this);
        } catch (RemoteException e) {
            throw new IllegalStateException("Could not create a MediaNotificationManager", e);
        }
    }

    public void play(Media media, boolean invalidate) {
        if (isLocalSrc(media.getSrc())) {
            mMode = MODE_LOCAL;
        } else {
            mMode = MODE_REMOTE;
        }
        if (invalidate) {
            reset();
        }
        //开wifiLock
        if (!mWifiLock.isHeld()) {
            mWifiLock.setReferenceCounted(false);
            mWifiLock.acquire();
        }
        mMedia = media;

        if (mNotificationManager != null) {
            if (invalidate) {
                mNotificationManager.onMediaChanged(media);
            }
            mNotificationManager.onPlaybackStateChanged(STATE_PLAYING);
            mNotificationManager.startNotification();
        }
        mState = STATE_PLAYING;
//        mCompositeDisposable.add(Observable.just(mMedia)
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(new Consumer<Media>() {
//                    @Override
//                    public void accept(Media media) throws Exception {
//                        Glide.with(AbsMusicService.this).load(media.getImage()).asBitmap().listener(new RequestListener<String, Bitmap>() {
//                            @Override
//                            public boolean onException(Exception e, String model, Target<Bitmap> target, boolean isFirstResource) {
//                                showNotification(null);
//                                return false;
//                            }
//
//                            @Override
//                            public boolean onResourceReady(Bitmap resource, String model, Target<Bitmap> target, boolean isFromMemoryCache, boolean isFirstResource) {
//                                showNotification(resource);
//                                return false;
//                            }
//                        }).preload();
//                    }
//                }));
        //notify Client
        start(invalidate);
    }

    protected void pause(){
        if (mNotificationManager != null) {
            mNotificationManager.onPlaybackStateChanged(STATE_PAUSED);
        }
        mState = STATE_PAUSED;
    }

    protected abstract void start(boolean invalidate);

    protected void stop(){
        if (mNotificationManager != null) {
            mNotificationManager.onPlaybackStateChanged(STATE_STOPPED);
        }
        mState = STATE_STOPPED;
    };

    public Media getPlayingMedia() {
        return mMedia;
    }

    public abstract AbsMusicServiceBinder getBinder();

    private void showNotification(Bitmap cover) {
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                getNotificationIntent(),
                PendingIntent.FLAG_UPDATE_CURRENT);
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.layout_notification);
        remoteViews.setTextViewText(R.id.tv_title, mMedia.getTitle());
        remoteViews.setTextViewText(R.id.tv_sub_title, mMedia.getArtist());
        remoteViews.setImageViewBitmap(R.id.iv_cover, cover);
        Notification notification = new NotificationCompat.Builder(this)
                .setContentIntent(pi)
                .setCustomContentView(remoteViews)
                .setAutoCancel(false)
                .setSmallIcon(R.drawable.ic_notification)
                .build();
        startForeground(22, notification);
    }

    protected abstract Intent getNotificationIntent();

    protected abstract void reset();

    public int getMode() {
        return mMode;
    }

    private boolean isLocalSrc(String src) {
        return !(src != null && src.startsWith("rtsp"));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mWifiLock != null) {
            mWifiLock.release();
        }
        mCompositeDisposable.dispose();
    }

    public abstract int getLeftVolume();

    public abstract int getRightVolume();

    public abstract void setVolume(int left, int right);

    public abstract void moveToPrev();

    public abstract void moveToNext();
}
