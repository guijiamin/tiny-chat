package com.study.signalcommon.proto;

import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.Test;

import java.util.List;

/**
 * Decription
 * <p>
 * </p>
 * DATE 2019/7/25.
 *
 * @author guijiamin.
 */
public class MsgProtoTest {

    @Test
    public void test() throws InvalidProtocolBufferException{
        MessageProto.Msg.Builder msg = MessageProto.Msg.newBuilder();
        msg.setMsgid("200");
        msg.setMsgtype("1");
        msg.putExtend("chats", "1234");
        msg.putExtend("users", "4567");

        MessageProto.User.Builder fuser = MessageProto.User.newBuilder();
        fuser.setRid("jz123");
        fuser.setUid("123");
        fuser.setName("gjm");
        fuser.setImg("avatar1.svg");
        msg.setFuser(fuser.build());

        MessageProto.User.Builder tuser = MessageProto.User.newBuilder();
        tuser.setRid("jz123");
        tuser.setUid("123");
        tuser.setName("gjm");
        tuser.setImg("avatar1.svg");
        msg.setTser(tuser.build());

        byte[] bytes = msg.build().toByteArray();
        MessageProto.Msg parseMsg = MessageProto.Msg.parseFrom(bytes);
        System.out.println(parseMsg.getExtendMap().get("chats"));
    }
}
