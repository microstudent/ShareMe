package net.majorkernelpanic.streaming;

import android.util.Log;

import java.math.BigInteger;
import java.util.Arrays;

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

    public static void logByte(byte[] src, int offset, int length) {
        Log.d("ByteUtils", "receive" + binary(Arrays.copyOfRange(src, offset, offset + length), 16));
    }

    public static String binary(byte[] bytes, int radix){
        return new BigInteger(1, bytes).toString(radix);// 这里的1代表正数
    }
}
