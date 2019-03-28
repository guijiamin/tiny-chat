package com.study.signalproxy.dto;

/**
 * Decription
 * <p>
 * </p>
 * DATE 2019/3/21.
 *
 * @author guijiamin.
 */
public class Router2ProxyEvent implements Event {
    private String msg;

    public Router2ProxyEvent(String msg) {
        this.msg = msg;
    }

    @Override
    public void processor() {
        System.out.println("收到router2Proxy消息：" + this.msg);
    }
}
