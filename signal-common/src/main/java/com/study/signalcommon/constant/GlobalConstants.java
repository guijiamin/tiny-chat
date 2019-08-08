package com.study.signalcommon.constant;

import com.study.signalcommon.protobuf.MessageProto;

/**
 * Decription
 * <p>
 * </p>
 * DATE 2019/7/25.
 *
 * @author guijiamin.
 */
public class GlobalConstants {
    public static final class SERVER_PORT {
        public static final int PROXY = 8787;
        public static final int ROUTER = 8888;
        public static final int WORKER = 8989;
    }

    public static final class MSG_ID {
        public static final int REPLY = 100;
//        public static final int ENTERROOM = 101;
        public static final int KEEPALIVE = 101;
        public static final int UNICAST = 102;
        public static final int BROADCAST = 103;
    }

    public static final class MSG_TYPE {
        public static final int LEAVE = 0;
        public static final int ENTER = 1;
//        public static final int LEAVE = 2;
        public static final int CHAT = 2;
    }

    public static final class HEADER {
        public static final byte[] MAGIC = {(byte) 0B1100_1010, (byte) 0B1111_1110, (byte) 0B1011_1010, (byte) 0B1011_1110};
        public static final byte[] VERSION = {(byte) 0B0000_0001};
        public static final int MSGID_BYTE_LEN = 1;
        public static final int CONTENT_BYTE_LEN = 2;
    }

    public static final class USER {
        public static final MessageProto.User HEARTBEAT = MessageProto.User.newBuilder()
                .setRid("rid_heartbeat")
                .setUid("uid_heartbeat")
                .setName("name_heartbeat")
                .setImg("img_heartbeat")
                .build();
    }

    public static final class REDIS_KEY {
        public static final String SIGNAL_WORKER = "SIGNAL_WORKER";
        public static final String USER_LIST = "USER_LIST";
        public static final String CHAT_LIST = "CHAT_LIST";
    }

    public static final class KEY {
        public static final String USERS = "users";
        public static final String CHATS = "chats";
        public static final String USER = "user";
        public static final String CHAT = "chat";
    }

    public static final class REGEX {
        public static final String SMILE = "\\|\\^\\_\\^\\|";
    }

    public static final class SYMBOL {
        public static final String SMILE = "|^_^|";
        public static final String COLON = ":";
        public static final String AT = "@";
    }

    public static final long HEARTBEAT_INTERVAL = 20000L;
}
