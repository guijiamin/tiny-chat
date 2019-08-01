package com.study.signalproxy.service;

import com.study.signalcommon.component.PacketTransceiver;
import com.study.signalcommon.constant.GlobalConstants;
import com.study.signalcommon.util.Tool;
import com.study.signalproxy.ProxyServer;
import com.study.signalproxy.dto.Router2ProxyEvent;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Decription
 * <p>
 * </p>
 * DATE 2019/3/21.
 *
 * @author guijiamin.
 */
@Slf4j
public class TcpClient {
    private final String server;
    private final int port;

    private Socket socket;//存在多线程共用（一个读线程、多个写线程）
    private boolean isSocketAvailable() {
         boolean flag = this.socket != null
                && this.socket.isBound()
                && !this.socket.isClosed()//需要获取closeLock
                && this.socket.isConnected()
                && !this.socket.isInputShutdown()
                && !this.socket.isOutputShutdown();
         if (!flag) {
             System.out.println("service is not available");
         }
         return flag;
    }

    //    private Deque<Byte> readBuf = new LinkedList<>();
//    private Deque<Byte> writeBuf = new LinkedList<>();
//
//    private void doCleanBuf(boolean isRead) {
//        if (isRead) {
//            this.readBuf.clear();
//        } else {
//            this.writeBuf.clear();
//        }
//    }
//
//    private byte doReadBuf(byte b) {
//        this.readBuf.add(b);
//        if (this.readBuf.size() > 0) {//当缓冲区没有缓冲数据，就存入缓冲区，返回原字节
//            return b;
//        } else {//如果缓冲区有数据，将传入的添加至队尾，返回并删除队头第一个
//            return this.readBuf.poll();
//        }
//    }

    private volatile long lastReadTs;//多个线程共享（读线程更新、心跳（写）线程读取）
    private AtomicInteger errorCount = new AtomicInteger(0);//多个线程共享

    private final Runnable receiver = () -> {
        System.out.println("receiver is running...");
        //当该状态为true时，说明socket可用
        //理论上closeLock要执行完下面所有代码才释放
        //因此客户端不会在执行中途主动关闭连接（抛出异常），除非服务端主动关闭（如何解决）
        while (isSocketAvailable()) {
            try {
                DataInputStream in = new DataInputStream(socket.getInputStream());
                int index = 0;
                if (in.available() > 0) {
                    //收到字节则激活时间轮
                    ProxyServer.tcpClientTimeWheel.add(TcpClient.this.server + GlobalConstants.SYMBOL.AT + TcpClient.this.port, TcpClient.this);
                    //解决粘包问题
                    if (Tool.findMagic(in.readByte(), index)
                            && Tool.findMagic(in.readByte(), index + 1)
                            && Tool.findMagic(in.readByte(), index + 2)
                            && Tool.findMagic(in.readByte(), index + 3)
                            ) {
                        byte[] contentLenBuf = new byte[GlobalConstants.HEADER.CONTENT_BYTE_LEN];
                        for (int i = 0; i < GlobalConstants.HEADER.CONTENT_BYTE_LEN; i++) {
                            contentLenBuf[i] = in.readByte();
                        }
                        short contentLen = Tool.byteArrayToShort(contentLenBuf);

                        int msgid = in.readByte();
                        if (msgid != GlobalConstants.MSG_ID.KEEPALIVE) {
                            byte[] messageProtoBuf = new byte[contentLen];
                            for (int i = 0; i < contentLen; i++) {
                                messageProtoBuf[i] = in.readByte();
                            }
                            //收到消息，扔到消息队列
                            Router2ProxyEvent event = new Router2ProxyEvent(msgid, messageProtoBuf);
                            EventQueue.getInstance().produce(event);
                        }
                        //收到字节，激活时间标注
                        TcpClient.this.lastReadTs = System.currentTimeMillis();
                        //收到完整消息，减少错误次数
                        if (TcpClient.this.errorCount.decrementAndGet() < 0) {//当减到0以下重置为0
                            TcpClient.this.errorCount.set(0);
                        }
                    }
                }

            } catch (IOException e) {
                //TODO 上面任何读取过程都可能会异常，如果该条消息读到消息某个位置异常，下次读取的时候从该位置后续读，这条消息将被抛弃

                //读到流结尾会抛IOException，有几种可能性：
                //1、没有消息，但还读取（这种可能性可以排除，外层available已处理）
                //2、流由于各种原因关闭了

                //添加错误次数，便于重连
                TcpClient.this.errorCount.getAndIncrement();
                log.error("read data exception: {}", e.getMessage());
            }

        }
    };
    private final Runnable heartbeater = () -> {
        System.out.println("heartbeater is running...");
        while (true) {
            try {
                Thread.sleep(5 * 1000);
            } catch (InterruptedException e) {

            }
            //该线程里不需要关注socket状态，因为send方法内部关注了
            //距离上次读到数据超过阈值才发送一条心跳
            long currentTs = System.currentTimeMillis();
            if (TcpClient.this.lastReadTs > 0) {
                if ((currentTs - TcpClient.this.lastReadTs) > GlobalConstants.HEARTBEAT_INTERVAL) {
//                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSSZ");
//                    System.out.println(sdf.format(currentTs) + "," + sdf.format(TcpClient.this.lastReadTs) + "," + (currentTs - TcpClient.this.lastReadTs));
                    TcpClient.this.send(GlobalConstants.MSG_ID.KEEPALIVE, new byte[0]);//空字节保活
                } else {
                    //如果阈值范围内错误次数一道道3次以上，则直接重连
                    if (TcpClient.this.errorCount.get() > 3) {
                        TcpClient.this.reStart();
                        System.out.println("失败次数超过3次，重连");
                    }
                }
            }
        }
    };

    public TcpClient(String server, int port) {
        this.server = server;
        this.port = port;
    }

    /**
     * 开启服务：1、创建socket连接；2、开启接收和心跳监控线程
     */
    public void start() {
        System.out.println("tcpclient is start...");
        this.connect();
        this.watchDog();
    }

    /**
     * 重新开启服务：1、关闭当前socket连接；2、重新创建socket连接；3、不需要再开启监控线程
     */
    public void reStart() {
        System.out.println("tcpclient is restart...");
        this.close();
        if (this.socket != null) this.socket = null;
        this.connect();
    }

    private void connect() {
        try {
            this.socket = new Socket(server, port);
            //如果该socket状态为isClosed、!isConnected、isInputShutdown、etc.，会抛出IOException
//            this.in = new DataInputStream(this.service.getInputStream());
//            this.out = new DataOutputStream(this.service.getOutputStream());
        } catch (IOException e) {//连接失败，TODO 继续尝试重连
            log.error("connect exception: {}", e.getMessage());
        }
    }

    private void watchDog() {
        if (isSocketAvailable()) {
            //建立可用连接后，分别开启两个线程
            //一个线程收取消息，扔到事件队列
            //一个线程发送心跳，保活连接
            new Thread(receiver).start();
            new Thread(heartbeater).start();
//            ProxyServer.tcpClientTimeWheel.add(this.server + GlobalConstants.SYMBOL.AT + this.port, this);
        }
    }

    /**
     * 关闭连接时，需要获取closeLock，与查询状态的方法：isSocketAvailable相排斥
     */
    private void close() {
        try {
            //Closing this service will also close the service's inputstream and outputstream
            this.socket.close();//需要获取closeLock
        } catch (IOException e) {
            log.error("close error: {}", e.getMessage());
        } finally {//无论socket是否正常关闭，都将socket置为null
            this.socket = null;
        }
    }

    public void send(int msgid, byte[] bytes) {
        //当该状态为true时，说明socket可用
        //理论上closeLock要执行完下面所有代码才释放
        //因此客户端不会在执行中途主动关闭连接（抛出异常），除非服务端主动关闭（如何解决）
        if (isSocketAvailable()) {
            try {
                //理论上该步骤不会再抛出异常（由while条件保证）
                DataOutputStream out = new DataOutputStream(this.socket.getOutputStream());
                //下面每一步都会引发异常
                out.write(GlobalConstants.HEADER.MAGIC);
                out.write(Tool.shortToByteArray((short) bytes.length));
                out.write(msgid);
                out.write(bytes);
                log.info("send msgid: {}", msgid);
            } catch (IOException e) {
                //写异常添加错误次数，便于重连
                this.errorCount.getAndIncrement();
                log.error("write data exception: {}", e.getMessage());
            }
        }
    }
}
