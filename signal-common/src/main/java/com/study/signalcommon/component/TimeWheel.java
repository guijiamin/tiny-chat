package com.study.signalcommon.component;

import lombok.Data;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Decription
 * <p>
 * </p>
 * DATE 2019/3/8.
 *
 * @author guijiamin.
 */
public class TimeWheel<K,V> {
    private final long tickDuration;
    private final int ticksPerWheel;
    private volatile int currentTickIndex = 0;

    private final CopyOnWriteArrayList<ExpirationListener<V>> expirationListeners = new CopyOnWriteArrayList<ExpirationListener<V>>();
    private final ArrayList<Slot<K,V>> wheel;
    private final Map<K, Slot<K,V>> indicator = new ConcurrentHashMap<K, Slot<K, V>>();

    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private Thread workerThread;

    public TimeWheel(long tickDuration, int ticksPerWheel, TimeUnit timeUnit) {
        if (timeUnit == null) {
            timeUnit = TimeUnit.MILLISECONDS;
        }

        if (tickDuration <= 0) {
            throw new IllegalArgumentException("tickDuration must be greater than 0: " + tickDuration);
        }

        if (ticksPerWheel <= 0) {
            throw new IllegalArgumentException("ticksPerWheel must be greater than 0: " + ticksPerWheel);
        }

        this.wheel = new ArrayList<Slot<K,V>>();
        this.tickDuration = TimeUnit.MILLISECONDS.convert(tickDuration, timeUnit);
        this.ticksPerWheel = ticksPerWheel;

        for (int i = 0; i < this.ticksPerWheel; i++) {
            wheel.add(new Slot<K, V>(i));
        }
        wheel.trimToSize();

        workerThread = new Thread(new TickWorker(), "Timing-Wheel");
    }

    public TimeWheel(long tickDuration, int ticksPerWheel, TimeUnit timeUnit, ExpirationListener<V> listener) {
        if (timeUnit == null) {
            timeUnit = TimeUnit.MILLISECONDS;
        }

        if (tickDuration <= 0) {
            throw new IllegalArgumentException("tickDuration must be greater than 0: " + tickDuration);
        }

        if (ticksPerWheel <= 0) {
            throw new IllegalArgumentException("ticksPerWheel must be greater than 0: " + ticksPerWheel);
        }

        this.wheel = new ArrayList<Slot<K,V>>();
        this.tickDuration = TimeUnit.MILLISECONDS.convert(tickDuration, timeUnit);
        this.ticksPerWheel = ticksPerWheel;

        for (int i = 0; i < this.ticksPerWheel; i++) {
            wheel.add(new Slot<K, V>(i));
        }
        wheel.trimToSize();

        workerThread = new Thread(new TickWorker(), "Timing-Wheel");

        expirationListeners.add(listener);
    }

    public void start() {
        if (shutdown.get()) {
            throw new IllegalArgumentException("Cannot be started once stopped");
        }

        if (!workerThread.isAlive()) {
            workerThread.start();
        }
    }

    public boolean stop() {
        if (!shutdown.compareAndSet(false, true)) {
            return false;
        }

        boolean interrupted = false;
        while (workerThread.isAlive()) {
            workerThread.interrupt();
            try {
                workerThread.join(100);//将工作者线程加入到本线程，最多等待工作者线程100ms
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }

        if (interrupted) {//当工作者线程中断成功后，才中断当前线程
            Thread.currentThread().interrupt();
        }
        return true;
    }

    public void add(K k, V v) {
        //不明白为什么要锁？感觉可以不用！
//        synchronized (k) {
        //检查是否已经存在，存在则移除
        checkAdd(k);
        //获取当前tick的前一个
        int previousTickIndex = getPreviousTickIndex();
        System.out.println("add idx: " + previousTickIndex + ", key: " + k);
        Slot<K, V> slot = wheel.get(previousTickIndex);
        slot.add(k, v);
        indicator.put(k, slot);
//        }
    }

    private void checkAdd(K k) {
        Slot<K, V> slot = indicator.get(k);
        if (slot != null) {
            slot.remove(k);
        }
    }

    private int getPreviousTickIndex() {
        lock.readLock().lock();
        try {
            int cti = currentTickIndex;
            if (cti == 0) {
                return ticksPerWheel - 1;
            }
            return cti - 1;
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean remove(K k) {
        //不明白为什么要锁？感觉可以不用！
//        synchronized (k) {
        Slot<K, V> slot = indicator.get(k);
        if (slot == null) {
            return false;
        }
        indicator.remove(k);
        slot.remove(k);
        return true;
//        }
    }

    public void addExpirationListener(ExpirationListener<V> listener) {
        expirationListeners.add(listener);
    }

    public void removeExpirationListener(ExpirationListener<V> listener) {
        expirationListeners.remove(listener);
    }

    private void notifyExpired(int idx) {
        Slot<K, V> slot = wheel.get(idx);
        System.out.println("notify: " + idx + ", size: " + wheel.get(getPreviousTickIndex()).elements.size());
        Map<K, V> elements = slot.getElements();
        //遍历当前槽的所有元素，从槽里删除，从引导表中删除（还需要比较内容，防止哈希碰撞删除错误）
        for (Map.Entry<K, V> entry : elements.entrySet()) {
            K key = entry.getKey();
            slot.remove(key);
            Slot<K, V> latesSlot = indicator.get(key);
            if (latesSlot.equals(slot)) {
                indicator.remove(key);
            }
            for (ExpirationListener<V> listener : expirationListeners) {
                listener.expired(entry.getValue());
            }
        }
    }

    @Data
    private static class Slot<K,V> {
        private int id;
        private Map<K,V> elements = new ConcurrentHashMap<K,V>();

        public Slot(int id) {
            this.id = id;
        }

        public void add(K k,V v) {
            elements.put(k, v);
        }

        public void remove(K k) {
            elements.remove(k);
        }

        public boolean equqls(Object obj) {
            if (this == obj) return true;

            if (obj == null) return false;

            if (getClass() != obj.getClass()) return false;

            Slot other = (Slot) obj;
            return id == other.id;
        }
    }

    private class TickWorker implements Runnable {
        private long startTime;
        private long tick;

        public void run() {
            startTime = System.currentTimeMillis();
            tick = 1;

            for (int i = 0; !shutdown.get(); i++) {
                if (i == wheel.size()) {
                    i = 0;
                }
                //拿到当前指针索引的锁
                lock.writeLock().lock();
                try {
                    currentTickIndex = i;
                } finally {
                    lock.writeLock().unlock();
                }
                notifyExpired(currentTickIndex);
                waitForNextTick();
            }
        }

        private void waitForNextTick() {
            for (; ; ) {
                long currentTime = System.currentTimeMillis();
                //不明白为什么这么算休眠时间=>走了多少个tick的时间-之间耗费时间差=当前tick的时间长度
                long sleepTime = tickDuration * tick - (currentTime - startTime);

                if (sleepTime <= 0) {
                    break;
                }

                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    return;
                }
            }
            tick++;
        }
    }
}
