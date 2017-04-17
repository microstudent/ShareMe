package com.leaves.app.shareme.service;

import com.leaves.app.shareme.bean.Media;

import net.majorkernelpanic.streaming.PlaytimeProvider;

/**
 * Created by Leaves on 2017/4/17.
 */

public interface MusicServiceBinder {
    void play(Media media);

    void pause();

    void stop();

}
