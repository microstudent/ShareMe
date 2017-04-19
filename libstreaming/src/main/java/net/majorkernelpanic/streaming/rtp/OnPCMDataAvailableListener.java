package net.majorkernelpanic.streaming.rtp;

/**
 * Created by Leaves on 2017/4/17.
 */

public interface OnPCMDataAvailableListener {
    void onPCMDataAvailable(long rtpTime, byte[] data);
}
