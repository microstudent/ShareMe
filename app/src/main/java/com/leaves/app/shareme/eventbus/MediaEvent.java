package com.leaves.app.shareme.eventbus;

import com.leaves.app.shareme.bean.Media;

import java.util.ArrayList;

/**
 * Created by Leaves on 2016/11/13.
 */

public class MediaEvent {
    private int action;
    private Media mMedia;

    public MediaEvent(int action, Media media) {
        this.action = action;
        mMedia = media;
    }

    public int getAction() {
        return action;
    }

    public Media getMedia() {
        return mMedia;
    }
}
