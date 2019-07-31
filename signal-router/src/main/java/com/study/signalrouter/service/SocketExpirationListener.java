package com.study.signalrouter.service;

import com.study.signalcommon.component.ExpirationListener;
import com.study.signalrouter.service.SocketTransceiver;

/**
 * Decription
 * <p>
 * </p>
 * DATE 2019/3/18.
 *
 * @author guijiamin.
 */
public class SocketExpirationListener<E> implements ExpirationListener<E> {
    @Override
    public void expired(E conn) {
        SocketTransceiver socketTransceiver = (SocketTransceiver) conn;
        socketTransceiver.close();
    }
}
