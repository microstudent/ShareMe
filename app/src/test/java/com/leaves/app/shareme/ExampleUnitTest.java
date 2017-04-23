package com.leaves.app.shareme;

import android.util.Log;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        byte[] bytes = new byte[22];
        setLong(bytes, 17274128600033L, 16, 20);
//        System.out.println(Integer.toBinaryString(444));
//        System.out.println(bytes[1]);
//        System.out.println(Integer.toBinaryString((int) byteToLong(bytes, 0, 2)));
        assertEquals(172748600033L, byteToLong(bytes, 16, 4));
    }

    private static void setLong(byte[] buffer, long n, int begin, int end) {
        for (end--; end >= begin; end--) {
            buffer[end] = (byte) (n % 256);
            n >>= 8;
        }
    }

    /**
     * byte数组中取int数值，本方法适用于(低位在前，高位在后)的顺序，和intToBytes（）配套使用
     *
     * @param src    byte数组
     * @param offset 从数组的第offset位开始
     * @return int数值
     */
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