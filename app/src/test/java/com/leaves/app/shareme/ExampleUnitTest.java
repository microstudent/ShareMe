package com.leaves.app.shareme;

import android.util.Log;

import net.majorkernelpanic.streaming.ByteUtils;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    private static final long OFFSET_1900_TO_1970 = ((365L * 70L) + 17L) * 24L * 60L * 60L;

    @Test
    public void addition_isCorrect() throws Exception {
        byte[] bytes = new byte[16];
        long ntpts = 31651261654L;

        writeTimeStamp(bytes, 0, ntpts);
        assertEquals(readTimeStamp(bytes, 0), ntpts);
    }


    /**
     * Writes system time (milliseconds since January 1, 1970) as an NTP time stamp
     * at the given offset in the buffer.
     */
    private void writeTimeStamp(byte[] buffer, int offset, long time) {
        long seconds = time / 1000L;
        long milliseconds = time - seconds * 1000L;
        seconds += OFFSET_1900_TO_1970;
        // write seconds in big endian format
        buffer[offset++] = (byte)(seconds >> 24);
        buffer[offset++] = (byte)(seconds >> 16);
        buffer[offset++] = (byte)(seconds >> 8);
        buffer[offset++] = (byte)(seconds >> 0);
        long fraction = milliseconds * 0x100000000L / 1000L;
        // write fraction in big endian format
        buffer[offset++] = (byte)(fraction >> 24);
        buffer[offset++] = (byte)(fraction >> 16);
        buffer[offset++] = (byte)(fraction >> 8);
        // low order bits should be random data
        buffer[offset++] = (byte)(Math.random() * 255.0);
    }

    /**
     * Reads the NTP time stamp at the given offset in the buffer and returns
     * it as a system time (milliseconds since January 1, 1970).
     */
    private long readTimeStamp(byte[] buffer, int offset) {
        long seconds = read32(buffer, offset);
        long fraction = read32(buffer, offset + 4);
        return ((seconds - OFFSET_1900_TO_1970) * 1000) + ((fraction * 1000L) / 0x100000000L);
    }

    /**
     * Reads an unsigned 32 bit big endian number from the given offset in the buffer.
     */
    private long read32(byte[] buffer, int offset) {
        byte b0 = buffer[offset];
        byte b1 = buffer[offset+1];
        byte b2 = buffer[offset+2];
        byte b3 = buffer[offset+3];
        // convert signed bytes to unsigned values
        int i0 = ((b0 & 0x80) == 0x80 ? (b0 & 0x7F) + 0x80 : b0);
        int i1 = ((b1 & 0x80) == 0x80 ? (b1 & 0x7F) + 0x80 : b1);
        int i2 = ((b2 & 0x80) == 0x80 ? (b2 & 0x7F) + 0x80 : b2);
        int i3 = ((b3 & 0x80) == 0x80 ? (b3 & 0x7F) + 0x80 : b3);
        return ((long)i0 << 24) + ((long)i1 << 16) + ((long)i2 << 8) + (long)i3;
    }
}