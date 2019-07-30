package com.study.signalcommon.component;

import com.google.protobuf.InvalidProtocolBufferException;
import com.study.signalcommon.protobuf.MessageProto;

import java.util.Map;

/**
 * Decription
 * <p>
 * </p>
 * DATE 2019/7/25.
 *
 * @author guijiamin.
 */
public class PacketTransceiver {
    public static MessageProto.Msg parseMessage(byte[] message) {
        try {
            return MessageProto.Msg.parseFrom(message);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] packMessage(Integer msgid, Integer msgtype, MessageProto.User fuser, MessageProto.User tuser) {
        return MessageProto.Msg.newBuilder()
                .setMsgid(msgid)
                .setMsgtype(msgtype)
                .setFuser(fuser)
                .setTuser(tuser)
                .build()
                .toByteArray();
    }

    public static byte[] packMessage(Integer msgid, Integer msgtype, Map<String, String> extend, MessageProto.User fuser, MessageProto.User tuser) {
        return MessageProto.Msg.newBuilder()
                .setMsgid(msgid)
                .setMsgtype(msgtype)
                .putAllExtend(extend)
                .setFuser(fuser)
                .setTuser(tuser)
                .build()
                .toByteArray();
    }

    public static MessageProto.User generateUser(String rid, String uid, String name, String img) {
        return MessageProto.User.newBuilder().setRid(rid).setUid(uid).setName(name).setImg(img).build();
    }
}
