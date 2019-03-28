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
    private String msg;

    public Proxy2RouterEvent(TcpClient router, String msg) {
        this.router = router;
        this.msg = msg;
    }

    @Override
    public void processor() {
        //将消息发送给router
        router.send(msg);
    }
}
