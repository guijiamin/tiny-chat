package com.study.signalcommon.protobuf;

import com.google.protobuf.InvalidProtocolBufferException;
import com.study.signalcommon.constant.GlobalConstants;
import org.junit.Test;

import java.io.*;
import java.math.BigInteger;

/**
 * Decription
 * <p>
 * </p>
 * DATE 2019/7/25.
 *
 * @author guijiamin.
 */
public class ProtobufTest {

    @Test
    public void test() throws Exception {
        MessageProto.Msg.Builder msg = MessageProto.Msg.newBuilder();
        msg.setMsgid(100);
//        msg.setSrcmsgid(101);
//        msg.setMsgtype(1);
//        msg.putExtend("chats", "1");
//        msg.putExtend("users", "4");

//        MessageProto.User.Builder fuser = MessageProto.User.newBuilder();
//        fuser.setRid("jz123");
//        fuser.setUid("123");
//        fuser.setName("gjm");
//        fuser.setImg("avatar1.svg");
//        msg.setFuser(fuser.build());

//        MessageProto.User.Builder tuser = MessageProto.User.newBuilder();
//        tuser.setRid("jz123");
//        tuser.setUid("123");
//        tuser.setName("gjm");
//        tuser.setImg("avatar1.svg");
//        msg.setTuser(tuser.build());
        System.out.println("=====开始输出=====");

        System.out.println(msg);

        MessageProto.Msg msg1 = MessageProto.Msg.parseFrom(msg.build().toByteArray());

        System.out.println("!!!");
        System.out.println(msg1);
//        ByteArrayOutputStream bos = new ByteArrayOutputStream();
//        //1、写入魔数
//        bos.write(GlobalConstants.NUM.MAGIC);
//
//        //2、写入正文长度和正文字节
//        byte[] msgBytes = msg.build().toByteArray();
//        byte[] lenBytes = intToByteArray(msgBytes.length);
//        bos.write(lenBytes);
//        bos.write(msgBytes);
//        bos.close();
//        System.out.println("=====结束输出=====");
//
//        System.out.println("=====开始输入=====");
//        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
//        //找到魔数
//        int index = 0;
//        if (findMagic(bis.read(), index++)
//                && findMagic(bis.read(), index++)
//                && findMagic(bis.read(), index++)
//                && findMagic(bis.read(), index)) {
//            byte[] lenBuffer = new byte[4];
//            for (int i = 0; i < 4; i++) {
//                lenBuffer[i] = (byte) bis.read();
//            }
//            int length = byteArrayToInt(lenBuffer);
//            System.out.println(length);
//            byte[] msgBuffer = new byte[length];
//            while (bis.read(msgBuffer) > 0) {
//            }
//            MessageProto.Msg parse = MessageProto.Msg.parseFrom(msgBuffer);
//            System.out.println(parse.toString());
//        }
//        System.out.println("=====结束输入=====");
    }

    public static boolean findMagic(int b, int index) {
        if (b == GlobalConstants.HEADER.MAGIC[index]) {
            return true;
        }
        return false;
    }

    public static byte[] intToByteArray(int i) {
        byte[] targets = new byte[4];
        targets[3] = (byte) (i);
        targets[2] = (byte) (i >> 8);
        targets[1] = (byte) (i >> 16);
        targets[0] = (byte) (i >> 24);
        for (byte b : targets) {
            System.out.println("byte: " + (b));
        }
        return targets;
    }

    public static int byteArrayToInt(byte[] bytes) {
        int b0 = bytes[0] & 0xFF;
        int b1 = bytes[1] & 0xFF;
        int b2 = bytes[2] & 0xFF;
        int b3 = bytes[3] & 0xFF;
        System.out.println("int: " + b0);
        System.out.println("int: " + b1);
        System.out.println("int: " + b2);
        System.out.println("int: " + b3);
        return (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
    }
}
