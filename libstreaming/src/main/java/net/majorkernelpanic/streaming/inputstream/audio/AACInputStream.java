package net.majorkernelpanic.streaming.inputstream.audio;

import net.majorkernelpanic.streaming.InputStream;
import net.majorkernelpanic.streaming.Stream;
import net.majorkernelpanic.streaming.rtp.unpacker.AACADTSUnpacker;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;

/**
 * 这个类将AAC音频解码
 * Created by Leaves on 2017/4/10.
 */

public class AACInputStream implements InputStream {

    private AACADTSUnpacker mAACADTSUnpacker;

    public AACInputStream() {
        mAACADTSUnpacker = new AACADTSUnpacker();
    }

    @Override
    public void start() throws IllegalStateException, IOException {
        mAACADTSUnpacker.start();
    }

    @Override
    public void stop() {
        mAACADTSUnpacker.stop();
    }

    @Override
    public void setListeningPorts(int rtpPort, int rtcpPort) {
        mAACADTSUnpacker.setDestination(rtpPort, rtcpPort);
    }


    @Override
    public void setOutputStream(OutputStream stream, byte channelIdentifier) {

    }

    @Override
    public int[] getLocalPorts() {
        return new int[0];
    }

    @Override
    public int getSSRC() {
        return 0;
    }

    @Override
    public long getBitrate() {
        return 0;
    }

    @Override
    public String getSessionDescription() throws IllegalStateException {
        return null;
    }

    @Override
    public boolean isStreaming() {
        return false;
    }
}
