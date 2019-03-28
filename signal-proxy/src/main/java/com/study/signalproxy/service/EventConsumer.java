package com.study.signalproxy.service;

import com.study.signalproxy.dto.Event;

/**
 * Decription
 * <p>
 * </p>
 * DATE 2019/3/21.
 *
 * @author guijiamin.
 */
public class EventConsumer implements Runnable {
    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(10 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println("消费之前队列大小：" + EventQueue.getInstance().size());
            //从队列获取事件，使用相应的事件处理器处理事件
            Event event = EventQueue.getInstance().consume();
            if (event != null) {
                System.out.println("EventConsumer consume...");
                event.processor();
            }
            System.out.println("消费之后队列大小：" + EventQueue.getInstance().size());
        }
    }
}
