package com.leaves.app.shareme.service;

import android.os.Binder;

import com.leaves.app.shareme.bean.Media;

import net.majorkernelpanic.streaming.PlaytimeProvider;

/**
 * Created by Leaves on 2017/4/17.
 */

public abstract class AbsMusicServiceBinder extends Binder{
    public abstract void play(Media media);

    public abstract void pause();

    public abstract void stop();
}
