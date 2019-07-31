package com.study.signalrouter.service;

import com.study.signalcommon.component.TimeWheel;
import com.study.signalcommon.constant.GlobalConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

/**
 * Decription
 * <p>
 * </p>
 * DATE 2019/3/21.
 *
 * @author guijiamin.
 */
@Slf4j
public class TcpServer {
    private final int port;
    private ServerSocket server;

    private boolean isServerAvailable() {
        boolean flag = this.server != null
                && this.server.isBound()
                && !this.server.isClosed();
        if (!flag) {
            System.out.println("server is not available");
        }
        return flag;
    }

    private final static Map<String, Map<String, SocketTransceiver>> roomUserToSocket = new ConcurrentHashMap<>();
    private final static TimeWheel<String, SocketTransceiver> socketTimeWheel = new TimeWheel<String, SocketTransceiver>(1, 60, TimeUnit.SECONDS, new SocketExpirationListener<SocketTransceiver>());
    //TODO 线程池优化
    public final static ThreadPoolExecutor executor = new ThreadPoolExecutor(
            10, 100,
            60L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>());
    //TODO http客户端优化
    public final static HttpClient httpclient = HttpClients.createDefault();

    public TcpServer(int port) {
        this.port = port;
    }

    /**
     * 启动服务器
     */
    public void start() {
        try {
            this.server = new ServerSocket(port);
//            log.info("tcpserver is started");
        } catch (IOException e) {
//            log.error("tcpserver starts error: {}", e.getMessage());
            System.exit(-1);//服务开启异常，退出
        }

        boolean available = isServerAvailable();
        if (available) {
            //服务启动后开启时间轮维护客户端连接
            socketTimeWheel.start();
//            log.info("timewheel is started");
        }

        while (available) {
            try {
                final Socket socket = server.accept();//阻塞模式获取客户端连接
//                log.info("accept service from {}", service.getInetAddress().getHostAddress());
                //接收到客户端连接就创建一个客户端收发器
                SocketTransceiver client = new SocketTransceiver(socket) {
                    //用户进教室回调
                    //1、维护roomUserToSocket列表
                    //2、遍历roomUserToSocket列表，当前用户发送100回应消息，当前房间其它用户发送103/1广播消息
                    @Override
                    public void onUserEnterRoom(SocketTransceiver socket, String rid, String uid, byte[] responseMsg, byte[] routerMsg) {
                        if (roomUserToSocket.containsKey(rid)) {
                            roomUserToSocket.get(rid).put(uid, socket);
                        } else {
                            HashMap<String, SocketTransceiver> map = new HashMap<>();
                            map.put(uid, socket);
                            roomUserToSocket.put(rid, map);
                        }
                        roomUserToSocket.get(rid).forEach((key, val) -> {
                            if (key.equals(uid)) {
                                val.send(GlobalConstants.MSG_ID.REPLY, responseMsg);
                                System.out.println("发送回应消息给" + rid + "-" + uid);
                            } else {
                                val.send(GlobalConstants.MSG_ID.BROADCAST, routerMsg);
                                System.out.println("发送广播消息给" + rid + "-" + uid);
                            }
                        });
                    }

                    //用户离开教室消息回调
                    //1、维护roomUserToSocket列表
                    //2、遍历roomUserToSocket列表，当前用户发送100回应消息，当前房间其它用户发送103/2广播消息
                    @Override
                    public void onUserLeaveRoom(String proxy, String rid, byte[] msg) {

                    }

                    //用户发送聊天消息（包括广播和单播）回调
                    //广播：遍历roomUserToSocket列表，当前用户发送100回应消息，当前房间其它用户发送103/3广播消息
                    //单播：从roomUserToSocket找到对端用户，发送104单播消息
                    @Override
                    public void onUserChat(boolean isBroadCast) {
                        if (isBroadCast) {
                            //TODO 广播
                        } else {
                            //TODO 单播
                        }
                    }

                    //proxy心跳消息回调
                    //激活时间轮
                    @Override
                    public void onProxyHeartBeat(String proxy) {
                        socketTimeWheel.add(proxy, this);
                    }

                    //proxy断连回调
                    //从时间轮里删除
                    @Override
                    public void onDisconnect(String proxy) {
                        socketTimeWheel.remove(proxy);
                    }
                };
                //开启一个客户端线程
                client.start();
                //接收到客户端连接加入时间轮，当收到proxy的心跳包会激活时间轮
                socketTimeWheel.add(client.getIp(), client);
            } catch (IOException e) {
//                log.error("accept service exception：{}", e.getMessage());
            }
        }
    }

    /**
     * 关闭服务器
     */
    public void close() {
        try {
            this.server.close();
        } catch (IOException e) {
//            log.error("tcpserver close exception：{}", e.getMessage());
        }
    }
}
