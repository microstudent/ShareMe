package com.leaves.app.shareme.bean;

/**
 * Created by Leaves on 2017/4/18.
 */

public class Frame {
    private long ntpTime;
    private long rtpTime;
    private byte[] PCMData;

    public byte[] getPCMData() {
        return PCMData;
    }

    public void setPCMData(byte[] PCMData) {
        this.PCMData = PCMData;
    }

    public long getNtpTime() {
        return ntpTime;
    }

    public void setNtpTime(long ntpTime) {
        this.ntpTime = ntpTime;
    }

    public long getRtpTime() {
        return rtpTime;
    }

    public void setRtpTime(long rtpTime) {
        this.rtpTime = rtpTime;
    }
}
