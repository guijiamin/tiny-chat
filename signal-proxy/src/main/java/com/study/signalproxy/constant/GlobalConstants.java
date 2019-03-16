package com.study.signalproxy.constant;

/**
 * Decription
 * <p>
 * </p>
 * DATE 19/3/4.
 *
 * @author guijiamin.
 */
public class GlobalConstants {
    public static final class MSG_ID {
        public static final String KEEPALIVE = "15";
        public static final String REPLY = "200";
        public static final String ENTERROOM = "201";
        public static final String UNICAST = "202";
        public static final String BROADCAST = "203";
    }

    public static final class MSG_TYPE {
        public static final String ENTER = "1";
        public static final String LEAVE = "2";
        public static final String CHAT = "3";
    }

    public static final class PREFIX {
        public static final String ROOM_INFO = "ROOM_INFO_";
    }
}

