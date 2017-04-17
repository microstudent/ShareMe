package net.majorkernelpanic.streaming.rtcp;

/**
 * Created by Leaves on 2017/4/17.
 */

public interface OnRTCPUpdateListener {
    void onRTCPUpdate(long ntpTime, long rtpTime);
}
