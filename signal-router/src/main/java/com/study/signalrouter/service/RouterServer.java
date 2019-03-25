package com.study.signalrouter.service.socket;

import com.study.signalrouter.service.TimeWheel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Decription
 * <p>
 * </p>
 * DATE 2019/3/18.
 *
 * @author guijiamin.
 */
public class RouterServer {

    public static void main(String[] args) {
//        TcpServer server = new TcpServer(5555) {
//            @Override
//            public void onConnect(SocketTransceiver client) {
//                System.out.println("client" + client.getAddr().getHostAddress() + " connect...");
//                connectedProxyMap.put(client.getAddr().getHostAddress(), client.socket);
//                timeWheel.add(client.getAddr().getHostAddress(), client.socket);
//            }
//
//            @Override
//            public void onConnectFailed() {
//                System.out.println("client connect failed...");
//            }
//
//            @Override
//            public void onReceive(SocketTransceiver client, String s) {
//                System.out.println("receive client :" + client.getAddr().getHostAddress() + ", msg: " + s);
//            }
//
//            @Override
//            public void onDisconnect(SocketTransceiver client) {
//                System.out.println("client disconnect...");
//            }
//
//            @Override
//            public void onServerStop() {
//                System.out.println("server stop...");
//            }
//        };
//        System.out.println("server start...");
//        server.start();
//        timeWheel = new TimeWheel<>(1, 60, TimeUnit.SECONDS);
//        timeWheel.start();
        //创建serversocket服务监听

    }
}
