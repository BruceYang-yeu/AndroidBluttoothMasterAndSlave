package me.czvn.blelibrary.utils;

/**
 * Created by andy on 2016/1/13.
 *
 */
public final class MsgCommonUtil {

    public static byte[] merge(byte[] bytes1, byte[] bytes2) {
        int length1 = bytes1.length;
        int length2 = bytes2.length;
        byte[] bytes = new byte[length1 + length2];
        for (int i = 0; i < length1; i++) {
            bytes[i] = bytes1[i];
        }
        for (int i = 0; i < length2; i++) {
            bytes[i + length1] = bytes2[i];
        }
        return bytes;
    }

    public static int goInt(byte[] bytes) {
        return Integer.parseInt(new String(bytes));
    }


    public static byte[] goBytes(int i) {
        return String.valueOf(i).getBytes();
    }


}
