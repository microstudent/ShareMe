package com.leaves.app.shareme.service;

import com.leaves.app.shareme.bean.Media;

/**
 * Created by Leaves on 2017/5/7.
 */

public interface MusicPlayerListener {
    void onMusicPause();

    void onMusicStart(Media media);
}
