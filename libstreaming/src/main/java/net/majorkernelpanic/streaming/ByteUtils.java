package net.majorkernelpanic.streaming;

/**
 * Created by Leaves on 2017/4/13.
 */

public class ByteUtils {
    public static long byteToLong(byte[] src, int offset, int length) {
//        for (end--; end >= begin; end--) {
//            buffer[end] = (byte) (n % 256);
//            n >>= 8;
//        }
        int n = 0;
        for (int i = offset, j = 0; j < length; i++, j++) {
            n <<= 8;
            n |= src[i] & 0xFF;
        }
        return n;
    }
}
