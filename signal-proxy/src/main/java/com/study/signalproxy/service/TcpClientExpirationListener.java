package com.study.signalproxy.service;

import com.study.signalcommon.component.ExpirationListener;

/**
 * Decription
 * <p>
 * </p>
 * DATE 2019/7/30.
 *
 * @author guijiamin.
 */
public class TcpClientExpirationListener<E> implements ExpirationListener<E> {
    @Override
    public void expired(E conn) {
        TcpClient tcpClient = (TcpClient) conn;
        tcpClient.reStart();
    }
}
