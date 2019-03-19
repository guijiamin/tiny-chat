package com.study.signalproxy.service;

import org.java_websocket.WebSocket;

/**
 * Decription
 * <p>
 * </p>
 * DATE 2019/3/8.
 *
 * @author guijiamin.
 */
public class WebSocketExpirationListener<E> implements ExpirationListener<E> {
    @Override
    public void expired(E conn) {
        WebSocket ws = (WebSocket) conn;
        ws.close(1000, "No HeartBeat");
    }
}
