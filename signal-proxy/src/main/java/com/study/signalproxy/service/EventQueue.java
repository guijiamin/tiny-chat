package com.study.signalproxy.service;

import com.study.signalproxy.dto.Event;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * Decription
 * <p>
 * </p>
 * DATE 2019/3/21.
 *
 * @author guijiamin.
 */
public class EventQueue {
    private static EventQueue instance;

    private ConcurrentLinkedQueue<Event> channel = new ConcurrentLinkedQueue<>();

    private EventQueue() {}

    /**
     * 双重校验单例模式
     * @return
     */
    public static EventQueue getInstance() {
        if (instance == null) {
            synchronized (EventQueue.class) {
                if (instance == null) {
                    instance = new EventQueue();
                }
                return instance;
            }
        }
        return instance;
    }

    /**
     * 生产事件
     * @param event
     * @return
     */
    public boolean produce(Event event) {
        return this.channel.add(event);//插入队尾
    }

    /**
     * 消费事件
     * @return
     */
    public Event consume() {
        return this.channel.poll();//从队头移出
    }

    public int size() {
        return this.channel.size();
    }
}
