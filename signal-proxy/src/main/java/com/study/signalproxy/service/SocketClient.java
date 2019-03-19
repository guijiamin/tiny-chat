package com.study.signalproxy.service;

import lombok.Data;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * Decription
 * <p>
 * </p>
 * DATE 2019/3/18.
 *
 * @author guijiamin.
 */
@Data
public class SocketClient {
    private Socket socket = null;
    private InputStream in = null;
    private OutputStream out = null;
    private String addr;
    private Integer port;

    public SocketClient() {
    }

    public SocketClient(String addr, Integer port) {
        this.addr = addr;
        this.port = port;
    }

    private void start() throws IOException {
        if (socket == null || socket.isClosed()) {
            socket = new Socket();
            socket.connect(new InetSocketAddress(addr, port));
        }
        in = socket.getInputStream();
        out = socket.getOutputStream();

        keepAliveWatchDog();
        recvMsgWatchDog();
    }

    private void recvMsgWatchDog() {
        new Thread(() -> {
            while (true) {
                try {
                    int available = in.available();
                    if (available > 0) {
                        byte[] bytes = new byte[available];
                        in.read(bytes);
                        System.out.println(new String(bytes, "utf-8"));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();


    }

    private void keepAliveWatchDog() {
        try {
            String hearBeat = "heartBeat:0001";
            new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(10 * 1000);
                        out.write(hearBeat.getBytes());
                        out.flush();
                    } catch (InterruptedException | IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
    }
}
