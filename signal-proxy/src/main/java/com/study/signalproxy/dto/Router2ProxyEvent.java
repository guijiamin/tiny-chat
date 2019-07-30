package com.study.signalproxy.dto;

import com.google.protobuf.InvalidProtocolBufferException;
import com.study.signalcommon.constant.GlobalConstants;
import com.study.signalcommon.protobuf.MessageProto;
import com.study.signalproxy.service.ProxyServer;

/**
 * Decription
 * <p>
 * </p>
 * DATE 2019/3/21.
 *
 * @author guijiamin.
 */
public class Router2ProxyEvent implements Event {
    private int msgid;
    private byte[] msg;

    public Router2ProxyEvent(int msgid, byte[] msg) {
        this.msgid = msgid;
        this.msg = msg;
    }

    @Override
    public void processor() {
        try {
            MessageProto.Msg msg = MessageProto.Msg.parseFrom(this.msg);
            System.out.println("收到router2Proxy消息：" + msg);
            String rid = msg.getFuser().getRid();
            String uid = msg.getFuser().getUid();
            //区分一下回应和广播
            switch (this.msgid) {
                case GlobalConstants.MSG_ID.REPLY://如果是回应，就发送给发送者
                    if (ProxyServer.onlineMap.containsKey(rid)) {
                        if (ProxyServer.onlineMap.get(rid).containsKey(uid)) {
                            ProxyServer.onlineMap.get(rid).get(uid).send(this.msg);
                        }
                    } else {
                        System.out.println("没有当前房间100");
                    }
                    break;
                case GlobalConstants.MSG_ID.ENTERROOM://理论上不会收到该消息了
                    break;
                case GlobalConstants.MSG_ID.KEEPALIVE://报活消息上层处理了
                    break;
                case GlobalConstants.MSG_ID.BROADCAST://如果是广播
                    int msgtype = msg.getMsgtype();
                    switch (msgtype) {
                        case GlobalConstants.MSG_TYPE.ENTER://如果是广播进教室，发送给除发送者其他人
                            if (ProxyServer.onlineMap.containsKey(rid)) {
                                ProxyServer.onlineMap.get(rid).entrySet()
                                        .stream()
                                        .filter(i -> !i.getKey().equals(uid))
                                        .forEach(i -> i.getValue().send(this.msg));
                            } else {
                                System.out.println("没有当前房间103");
                            }
                            break;
                        case GlobalConstants.MSG_TYPE.LEAVE:
                            break;
                        case GlobalConstants.MSG_TYPE.CHAT:
                            break;
                        default:
                            break;
                    }
                    break;
                case GlobalConstants.MSG_ID.UNICAST:
                    break;
                default:
                    break;
            }
        } catch (InvalidProtocolBufferException e) {
            System.out.println("解析错误：" + e.getMessage());
        }
    }
}
