package com.study.signalcommon.util;

import com.study.signalcommon.constant.GlobalConstants;

/**
 * Decription
 * <p>
 * </p>
 * DATE 2019/7/27.
 *
 * @author guijiamin.
 */
public class Tool {
    public static boolean findMagic(byte i, int index) {
        return GlobalConstants.HEADER.MAGIC[index] == i;
    }

    /**
     * 将short数值转换为byte数组
     * byte[0]为最高位
     *
     * @param i
     * @return
     */
    public static byte[] shortToByteArray(short i) {
        byte[] bytes = new byte[2];
        bytes[1] = (byte) (i);
        bytes[0] = (byte) (i >> 8);
//        for (byte b : bytes) {
//            System.out.println("转换字节：" + b);
//        }
        return bytes;
    }

    /**
     * 将byte数组转换为short数值
     * byte[0]为最高位
     *
     * @param bytes
     * @return
     */
    public static short byteArrayToShort(byte[] bytes) {
        return (short) (((bytes[0] & 0xFF) << 8) | (bytes[1] & 0xFF));
    }

    public static String getUserListKeyByWorker(String rid) {
        return GlobalConstants.REDIS_KEY.SIGNAL_WORKER +
                GlobalConstants.SYMBOL.COLON +
                GlobalConstants.REDIS_KEY.USER_LIST +
                GlobalConstants.SYMBOL.COLON +
                rid;
    }

    public static String getChatListKeyByWorker(String rid) {
        return GlobalConstants.REDIS_KEY.SIGNAL_WORKER +
                GlobalConstants.SYMBOL.COLON +
                GlobalConstants.REDIS_KEY.CHAT_LIST +
                GlobalConstants.SYMBOL.COLON +
                rid;
    }

    public static boolean isEmpty(Object str) {
        return (str == null || "".equals(str));
    }
}
