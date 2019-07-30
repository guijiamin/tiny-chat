package com.study.signalproxy.dto;

import com.study.signalproxy.service.TcpClient;

/**
 * Decription
 * <p>
 * </p>
 * DATE 2019/3/21.
 *
 * @author guijiamin.
 */
public class Proxy2RouterEvent implements Event {
    private TcpClient router;
    private int msgid;
    private byte[] msg;

    public Proxy2RouterEvent(TcpClient router, int msgid, byte[] msg) {
        this.router = router;
        this.msgid = msgid;
        this.msg = msg;
    }

    @Override
    public void processor() {
        //将消息发送给router
        this.router.send(msgid, msg);
    }
}
