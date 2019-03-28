package com.study.signalrouter.service.socket;

import com.study.signalrouter.service.SocketExpirationListener;
import com.study.signalrouter.service.TimeWheel;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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
    private int port;
    private boolean runFlag;
    private ServerSocket server;
    private Map<String, SocketTransceiver> proxy2socket = new ConcurrentHashMap<>();
    private Map<String, Map<String, String>> roomUser2proxy = new ConcurrentHashMap<>();
    private TimeWheel<String, Socket> timeWheel;

    public TcpServer(int port) {
        this.port = port;
    }

    /**
     * 启动服务器
     */
    public void start() {
        System.out.println("tcp server is ready to start...");
        this.runFlag = true;
        try {
            server = new ServerSocket(port);
        } catch (IOException e) {
            this.runFlag = false;
        }
        if (this.runFlag) {
            this.timeWheel = new TimeWheel<String, Socket>(1, 60, TimeUnit.SECONDS);
            this.timeWheel.addExpirationListener(new SocketExpirationListener<Socket>());
            this.timeWheel.start();
            System.out.println("tcp server timewheel is started");
        }
        //阻塞模式获取客户端连接
        while (this.runFlag) {
            try {
                final Socket socket = server.accept();
                //接收到客户端连接就创建一个客户端收发器
                SocketTransceiver client = new SocketTransceiver(socket) {
                    @Override
                    public void onUserEnterRoom(String proxy, String rid, String uid) {
                        if (TcpServer.this.roomUser2proxy.containsKey(rid)) {

                            new HashSet<>(TcpServer.this.roomUser2proxy.get(rid).values()).forEach(ip -> {
                                if (TcpServer.this.proxy2socket.containsKey(ip)) {
                                    TcpServer.this.proxy2socket.get(ip).send("{\"msgid\":\"203\",\"rid\":\"jz123\",\"uid\":\"123\",\"alias\":\"lihua\",\"msg\":{\"msgtype\":\"1\",\"data\":{\"rid\":\"jz123\",\"uid\":\"123\",\"alias\":\"lihua\"}}}");
                                }
                            });

                        }
                        Map<String, String> user2proxy = new HashMap<>();
                        user2proxy.put(uid, proxy);
                        TcpServer.this.roomUser2proxy.put(rid, user2proxy);
                    }

                    @Override
                    public void onUserLeaveRoom(String proxy, String rid, String uid) {
                        if (TcpServer.this.roomUser2proxy.containsKey(rid) && TcpServer.this.roomUser2proxy.get(rid).containsKey(uid)) {
                            TcpServer.this.roomUser2proxy.get(rid).remove(uid);
                        }
                    }

                    @Override
                    public void onProxyHeartBeat(String proxy) {
                        timeWheel.add(proxy, socket);
                    }

                    @Override
                    public void onDisconnect(String proxy) {
                        TcpServer.this.proxy2socket.remove(proxy);
                    }
                };
                //开启一个客户端线程
                client.start();
                //添加到客户端连接列表里
                this.proxy2socket.put(client.getIp(), client);
            } catch (IOException e) {
                log.error("接收客户端连接异常：{}", e.getMessage());
            }
        }
        //停止服务器后this.runFlag=false，断开与每个客户端的连接
        try {
            server.close();
        } catch (IOException e) {
            log.error("服务端关闭异常：{}", e.getMessage());
        }
    }

    /**
     * 关闭服务器
     */
    public void stop() {
        this.runFlag = false;
    }

//    public void run() {
//        while (runFlag) {
//            try {
//                final Socket socket = server.accept();
//                //接收到客户端连接就创建一个客户端收发器
//                SocketTransceiver client = new SocketTransceiver(socket) {
//                    @Override
//                    public void onReceive(InetAddress addr, String s) {
////                        TcpServer.this.onReceive(this, s);
//                    }
//
//                    @Override
//                    public void onDisconnect(InetAddress addr) {
//                        clients.remove(this);
////                        TcpServer.this.onDisconnect(this);
//                    }
//                };
//                //开启一个客户端线程
//                client.start();
//                //添加到客户端连接列表里
//                this.clients.add(client);
//                //回调
////                this.onConnect(client);
//            } catch (IOException e) {
//                log.error("接收客户端连接异常：{}", e.getMessage());
//                //回调
////                this.onConnectFailed();
//            }
//        }
//        //每次执行时，runFlag=false会走到下边
//        //停止服务器后，断开与每个客户端的连接
//        try {
//            //遍历clients
//            for (SocketTransceiver client : clients) {
//                client.stop();
//            }
//            clients.clear();
//            server.close();
//        } catch (Exception e) {
//            log.error("服务端关闭异常：{}", e.getMessage());
//        }
//        //回调
////        this.onServerStop();
//    }

//    public abstract void onConnect(SocketTransceiver client);
//
//    public abstract void onConnectFailed();
//
//    public abstract void onReceive(SocketTransceiver client, String s);
//
//    public abstract void onDisconnect(SocketTransceiver client);
//
//    public abstract void onServerStop();
}
