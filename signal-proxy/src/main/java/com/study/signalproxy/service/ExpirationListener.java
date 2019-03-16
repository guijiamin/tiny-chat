package com.study.signalproxy.service;

/**
 * Decription
 * <p>
 * </p>
 * DATE 2019/3/8.
 *
 * @author guijiamin.
 */
public interface ExpirationListener<E> {
    void expired(E expiredObject);
}
