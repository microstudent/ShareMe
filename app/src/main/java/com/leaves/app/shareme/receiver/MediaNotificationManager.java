/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.leaves.app.shareme.receiver;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.leaves.app.shareme.R;
import com.leaves.app.shareme.SApplication;
import com.leaves.app.shareme.bean.Media;
import com.leaves.app.shareme.service.AbsMusicService;
import com.leaves.app.shareme.service.AbsMusicServiceBinder;
import com.leaves.app.shareme.ui.activity.MainActivity;
import com.leaves.app.shareme.util.LogHelper;
import com.leaves.app.shareme.util.ResourceHelper;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;


/**
 * Keeps track of a notification and updates it automatically for a given
 * MediaSession. Maintaining a visible notification (usually) guarantees that the music service
 * won't be killed during playback.
 */
public class MediaNotificationManager extends BroadcastReceiver {
    private static final String TAG = LogHelper.makeLogTag(MediaNotificationManager.class);

    private static final int NOTIFICATION_ID = 412;
    private static final int REQUEST_CODE = 100;

    public static final String ACTION_PAUSE = "com.leaves.app.shareme.receiver.pause";
    public static final String ACTION_PLAY = "com.leaves.app.shareme.receiver.play";
    public static final String ACTION_PREV = "com.leaves.app.shareme.receiver.prev";
    public static final String ACTION_NEXT = "com.leaves.app.shareme.receiver.next";

    private final AbsMusicService mService;

    private final NotificationManagerCompat mNotificationManager;

    private final PendingIntent mPauseIntent;
    private final PendingIntent mPlayIntent;
    private final PendingIntent mPreviousIntent;
    private final PendingIntent mNextIntent;

    private final int mNotificationColor;

    private boolean mStarted = false;
    private Media mMedia;
    private int mPlaybackState;

    public MediaNotificationManager(AbsMusicService service) throws RemoteException {
        mService = service;

        mNotificationColor = ResourceHelper.getThemeColor(mService, R.attr.colorPrimary,
                Color.DKGRAY);

        mNotificationManager = NotificationManagerCompat.from(service);

        String pkg = mService.getPackageName();
        mPauseIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                new Intent(ACTION_PAUSE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mPlayIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                new Intent(ACTION_PLAY).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mPreviousIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                new Intent(ACTION_PREV).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mNextIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                new Intent(ACTION_NEXT).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);

        // Cancel all notifications to handle the case where the Service was killed and
        // restarted by the system.
        mNotificationManager.cancelAll();
    }

    /**
     * Posts the notification and starts tracking the session to keep it
     * updated. The notification will automatically be removed if the session is
     * destroyed before {@link #stopNotification} is called.
     */
    public void startNotification() {
        if (!mStarted) {
            mMedia = mService.getPlayingMedia();

            // The notification must be updated after setting started to true
            Observable.fromCallable(new Callable<Notification>() {
                @Override
                public Notification call() throws Exception {
                    return createNotification();
                }
            })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<Notification>() {
                        @Override
                        public void accept(Notification notification) throws Exception {
                            if (notification != null) {
                                IntentFilter filter = new IntentFilter();
                                filter.addAction(ACTION_NEXT);
                                filter.addAction(ACTION_PAUSE);
                                filter.addAction(ACTION_PLAY);
                                filter.addAction(ACTION_PREV);
                                mService.registerReceiver(MediaNotificationManager.this, filter);

                                mService.startForeground(NOTIFICATION_ID, notification);
                                mStarted = true;
                            }
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            Log.e(TAG, "show notification fail", throwable);
                            throwable.printStackTrace();
                        }
                    });
        }
    }

    /**
     * Removes the notification and stops tracking the session. If the session
     * was destroyed this has no effect.
     */
    public void stopNotification() {
        if (mStarted) {
            mStarted = false;
            try {
                mNotificationManager.cancel(NOTIFICATION_ID);
                mService.unregisterReceiver(this);
            } catch (IllegalArgumentException ex) {
                // ignore if the receiver is not registered.
            }
            mService.stopForeground(true);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        LogHelper.d(TAG, "Received intent with action " + action);
        AbsMusicServiceBinder binder = mService.getBinder();
        if (binder != null && binder.isBinderAlive()) {
            switch (action) {
                case ACTION_PAUSE:
                    binder.pause();
                    break;
                case ACTION_PLAY:
                    binder.play(mService.getPlayingMedia(), false);
                    break;
                case ACTION_NEXT:
                    binder.moveToNext();
                    break;
                case ACTION_PREV:
                    binder.moveToNext();
                    break;
                default:
                    LogHelper.w(TAG, "Unknown intent ignored. Action=", action);
            }
        }
    }


    private PendingIntent createContentIntent(Media media) {
        Intent openUI = new Intent(mService, MainActivity.class);
        openUI.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (media != null) {
            openUI.putExtra(MainActivity.EXTRA_CURRENT_MEDIA_DESCRIPTION, media);
        }
        return PendingIntent.getActivity(mService, REQUEST_CODE, openUI,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public void onPlaybackStateChanged(@NonNull int state) {
        mPlaybackState = state;
        LogHelper.d(TAG, "Received new playback state", state);
        if (state == AbsMusicService.STATE_STOPPED ||
                state == AbsMusicService.STATE_NONE) {
            stopNotification();
        } else {
            // The notification must be updated after setting started to true
            Observable.fromCallable(new Callable<Notification>() {
                @Override
                public Notification call() throws Exception {
                    return createNotification();
                }
            })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<Notification>() {
                        @Override
                        public void accept(Notification notification) throws Exception {
                            if (notification != null) {
                                mNotificationManager.notify(NOTIFICATION_ID, notification);
                            }
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            throwable.printStackTrace();
                        }
                    });
        }
    }

    public void onMediaChanged(Media media) {
        mMedia = media;
        // The notification must be updated after setting started to true
        Observable.fromCallable(new Callable<Notification>() {
            @Override
            public Notification call() throws Exception {
                return createNotification();
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Notification>() {
                    @Override
                    public void accept(Notification notification) throws Exception {
                        if (notification != null) {
                            mNotificationManager.notify(NOTIFICATION_ID, notification);
                        }
                    }
                });
    }

    private Notification createNotification() {
        if (mMedia == null || mPlaybackState == AbsMusicService.STATE_NONE) {
            return null;
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(mService);
        Bitmap art = null;
        if (mMedia.getImage() != null) {
            // This sample assumes the iconUri will be a valid URL formatted String, but
            // it can actually be any valid Android Uri formatted String.
            // async fetch the album art icon
            try {
                art = Glide.with(SApplication.getContext())
                        .load(mMedia.getImage())
                        .asBitmap()
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                        .get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        RemoteViews remoteViews = new RemoteViews(mService.getPackageName(), R.layout.layout_notification);
        remoteViews.setTextViewText(R.id.tv_title, mMedia.getTitle());
        remoteViews.setTextViewText(R.id.tv_sub_title, mMedia.getArtist());
        remoteViews.setImageViewBitmap(R.id.iv_cover, art);

        remoteViews.setOnClickPendingIntent(R.id.bt_next, mNextIntent);
        addPlayPauseAction(remoteViews);

        notificationBuilder
                .setColor(mNotificationColor)
                .setSmallIcon(R.drawable.ic_notification)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCustomContentView(remoteViews)
                .setContentIntent(createContentIntent(mMedia));

        setNotificationPlaybackState(notificationBuilder);
        return notificationBuilder.build();
    }

    private void addPlayPauseAction(RemoteViews views) {
        LogHelper.d(TAG, "updatePlayPauseAction");
        int icon;
        PendingIntent intent;
        if (mPlaybackState == AbsMusicService.STATE_PLAYING) {
            icon = R.drawable.ic_pause;
            intent = mPauseIntent;
        } else {
            icon = R.drawable.ic_play;
            intent = mPlayIntent;
        }
        views.setOnClickPendingIntent(R.id.bt_play_pause, intent);
        views.setImageViewResource(R.id.bt_play_pause, icon);
    }

    private void setNotificationPlaybackState(NotificationCompat.Builder builder) {
        LogHelper.d(TAG, "updateNotificationPlaybackState. mPlaybackState=" + mPlaybackState);
        if (!mStarted) {
            LogHelper.d(TAG, "updateNotificationPlaybackState. cancelling notification!");
            mService.stopForeground(true);
            return;
        }
        builder
                .setWhen(0)
                .setShowWhen(false)
                .setUsesChronometer(false);
        // Make sure that the notification can be dismissed by the user when we are not playing:
        builder.setOngoing(mPlaybackState == AbsMusicService.STATE_PLAYING);
    }
}
