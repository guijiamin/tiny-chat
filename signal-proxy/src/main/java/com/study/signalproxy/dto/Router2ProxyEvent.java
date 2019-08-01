package com.study.signalproxy.dto;

import com.google.protobuf.InvalidProtocolBufferException;
import com.study.signalcommon.constant.GlobalConstants;
import com.study.signalcommon.protobuf.MessageProto;
import com.study.signalproxy.ProxyServer;
import lombok.extern.slf4j.Slf4j;

/**
 * Decription
 * <p>
 * </p>
 * DATE 2019/3/21.
 *
 * @author guijiamin.
 */
@Slf4j
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
            String frid = msg.getFuser().getRid();
            String fuid = msg.getFuser().getUid();
            //区分一下回应和广播
            switch (this.msgid) {
                case GlobalConstants.MSG_ID.REPLY://如果是回应，就发送给发送者
                    if (ProxyServer.onlineMap.containsKey(frid)) {
                        if (ProxyServer.onlineMap.get(frid).containsKey(fuid)) {
                            ProxyServer.onlineMap.get(frid).get(fuid).send(this.msg);
                        }
                    } else {
                        log.warn("receive reply msg from router, rid:{} is not in onlineMap", frid);
                    }
                    break;
                case GlobalConstants.MSG_ID.KEEPALIVE://保活消息上层处理了
                    break;
                case GlobalConstants.MSG_ID.BROADCAST://如果是广播
                    switch (msg.getMsgtype()) {
                        case GlobalConstants.MSG_TYPE.LEAVE://如果是广播离开教室，发送给除发送者其他人
                        case GlobalConstants.MSG_TYPE.ENTER://如果是广播进教室，发送给除发送者其他人
                        case GlobalConstants.MSG_TYPE.CHAT://如果是广播聊天消息，发送给除发送者其他人
                            if (ProxyServer.onlineMap.containsKey(frid)) {
                                ProxyServer.onlineMap.get(frid).entrySet()
                                        .stream()
                                        .filter(i -> !i.getKey().equals(fuid))
                                        .forEach(i -> i.getValue().send(this.msg));
                            } else {
                                log.warn("receive broadcast msg from router, rid:{} is not in onlineMap", frid);
                            }
                            break;
                        default:
                            break;
                    }
                    break;
                case GlobalConstants.MSG_ID.UNICAST://如果是单播
                    switch (msg.getMsgtype()) {
                        case GlobalConstants.MSG_TYPE.CHAT://如果是单播聊天消息，发送给指定人
                            String trid = msg.getTuser().getRid();
                            String tuid = msg.getTuser().getUid();
                            if (ProxyServer.onlineMap.containsKey(trid)) {
                                ProxyServer.onlineMap.get(trid)
                                        .forEach((key, val) -> {
                                            if (key.equals(tuid)) {
                                                val.send(this.msg);
                                            }
                                        });
                            } else {
                                log.warn("receive unicast msg from router, rid:{} is not in onlineMap", trid);
                            }
                            break;
                        default:
                            break;
                    }
                    break;
                default:
                    break;
            }
        } catch (InvalidProtocolBufferException e) {
            System.out.println("解析错误：" + e.getMessage());
        }
    }
}
