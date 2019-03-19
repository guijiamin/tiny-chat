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
    private Map<String, Socket> proxyMap = new ConcurrentHashMap<>();
    private TimeWheel<String, Socket> timeWheel;

    public void start() {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(5555);
            System.out.println("RouterServer is start...");
            timeWheel = new TimeWheel<>(1, 60, TimeUnit.SECONDS);
//            timeWheel.start();
            while (true) {
                Socket socket = serverSocket.accept();
                InetSocketAddress remoteSocketAddress = (InetSocketAddress)socket.getRemoteSocketAddress();

                proxyMap.put(remoteSocketAddress.getAddress().getHostAddress(), socket);
                System.out.println(remoteSocketAddress.getAddress().getHostAddress() + "已连接，当前有：" + proxyMap.size() + "个proxy连接");
                //每个连接都启用一个新的线程来处理
                new Thread(new RouterHandler(socket.getInputStream(), socket.getOutputStream())).start();
            }
        } catch (IOException e) {

        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        new RouterServer().start();
    }
}
