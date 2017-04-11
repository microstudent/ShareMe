package com.leaves.app.shareme.eventbus;

/**
 * Created by Leaves on 2016/11/14.
 */

public class TimeSeekEvent {
    long currentTime;
    long duration;

    public TimeSeekEvent(long currentTime, long duration) {
        this.currentTime = currentTime;
        this.duration = duration;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(long currentTime) {
        this.currentTime = currentTime;
    }
}
