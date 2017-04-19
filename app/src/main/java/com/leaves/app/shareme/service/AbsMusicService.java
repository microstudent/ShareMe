package com.leaves.app.shareme.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.wifi.WifiManager;
import android.support.v4.app.NotificationCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.leaves.app.shareme.R;
import com.leaves.app.shareme.bean.Media;

/**
 * Created by Leaves on 2017/4/17.
 */

public abstract class AbsMusicService extends Service {
    public static final int MODE_LOCAL = 0;
    public static final int MODE_REMOTE = 1;

    private int mMode;
    private WifiManager.WifiLock mWifiLock;
    protected Media mMedia;

    @Override
    public void onCreate() {
        super.onCreate();
        mWifiLock = ((WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "musicLock");
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
        mWifiLock.setReferenceCounted(false);
        mWifiLock.acquire();

        mMedia = media;
        Glide.with(this).load(media.getImage()).asBitmap().listener(new RequestListener<String, Bitmap>() {
            @Override
            public boolean onException(Exception e, String model, Target<Bitmap> target, boolean isFirstResource) {
                showNotification(null);
                return false;
            }

            @Override
            public boolean onResourceReady(Bitmap resource, String model, Target<Bitmap> target, boolean isFromMemoryCache, boolean isFirstResource) {
                showNotification(resource);
                return false;
            }
        }).preload();
        //notify Client
        start(invalidate);
    }

    protected abstract void pause();

    protected abstract void start(boolean invalidate);

    protected abstract void stop();

    private void showNotification(Bitmap cover) {
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                getNotificationIntent(),
                PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new NotificationCompat.Builder(this)
                .setContentIntent(pi)
                .setLargeIcon(cover)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentText(mMedia.getTitle())
                .setSubText(mMedia.getArtist())
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
}
