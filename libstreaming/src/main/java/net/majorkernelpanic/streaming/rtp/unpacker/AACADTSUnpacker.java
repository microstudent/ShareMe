package net.majorkernelpanic.streaming.rtp.unpacker;

import android.util.Log;

import java.util.Arrays;

/**
 *
 * Created by Leaves on 2017/4/2.
 */

public class AACADTSUnpacker extends AbstractUnpacker {

    private boolean isRunning = true;

    @Override
    public void start() {
        while (isRunning) {
            byte[] result;
            try {
                result = mSocket.consumeData();
                Log.d("AACADTSUnpacker", "result:" + Arrays.toString(result));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void stop() {
        isRunning = false;
    }
}
