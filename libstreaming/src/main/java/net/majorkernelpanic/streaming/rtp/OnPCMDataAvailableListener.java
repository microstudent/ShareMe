package net.majorkernelpanic.streaming.rtp;

/**
 * Created by Leaves on 2017/4/17.
 */

public interface OnPCMDataAvailableListener {
    void onPCMDataAvaialable(long rtpTime, byte[] data);
}
