package com.leaves.app.shareme.service;

import android.os.Binder;

import com.leaves.app.shareme.bean.Media;

import net.majorkernelpanic.streaming.PlaytimeProvider;

/**
 * Created by Leaves on 2017/4/17.
 */

public abstract class AbsMusicServiceBinder extends Binder{
    public abstract void play(Media media,boolean invalidate);

    public abstract void pause();

    public abstract void stop();

    public abstract boolean isConnectionAlive();

    public abstract void moveToNext();

    public abstract void moveToPrev();

    public abstract void setMusicPlayerListener(MusicPlayerListener musicPlayerListener);

    public abstract boolean isBusy();
}
