package com.study.signalproxy.service;

import com.study.signalproxy.constant.GlobalConstants;
import com.study.signalproxy.dto.Router2ProxyEvent;
import lombok.extern.slf4j.Slf4j;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

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
    private String server;
    private int port;
    private boolean runFlag = false;
    //    private SocketTransceiver transceiver;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    public TcpClient(String server, int port) {
        this.server = server;
        this.port = port;
    }

    public void connect() {
        try {
            this.socket = new Socket(server, port);
            in = new DataInputStream(this.socket.getInputStream());
            out = new DataOutputStream(this.socket.getOutputStream());
            this.runFlag = true;
        } catch (IOException e) {
            log.error("create tcp socket exception: {}", e.getMessage());
            this.runFlag = false;
        }
        new Thread(new ReceiveMsgWatchDog()).start();
        new Thread(new KeepAliveWatchDog()).start();
    }

    public void disConnect() {
        this.runFlag = false;
        try {
            this.in.close();
            this.out.close();
            this.socket.close();
            this.in = null;
            this.out = null;
            this.socket = null;
        } catch (IOException e) {
            log.error("proxy close socket with router");
        }
    }

    private class ReceiveMsgWatchDog implements Runnable {
        public void run() {
            System.out.println("ReceiveMsgWatchDog is running...");
            while (TcpClient.this.runFlag) {
                try {
                    final String data = in.readUTF();
                    //收到消息，扔到消息队列
                    Router2ProxyEvent event = new Router2ProxyEvent(data);
                    EventQueue.getInstance().produce(event);
                } catch (IOException e) {
                    log.error("read input stream exception: {}", e.getMessage());
                }
            }
        }
    }

    private class KeepAliveWatchDog implements Runnable {
        public void run() {
            System.out.println("KeepAliveWatchDog is running...");
            while (TcpClient.this.runFlag) {
                try {
                    Thread.sleep(10 * 1000);
                    TcpClient.this.send(GlobalConstants.MSG_KEEPALIVE);
                } catch (InterruptedException e) {
                    log.error("proxy send heartbeat to router exception: {}", e.getMessage());
                }
            }
        }
    }

    public boolean send(String s) {
        if (out != null) {
            try {
                out.writeUTF(s);
                out.flush();
            } catch (IOException e) {
                log.error("write data to output stream exception: {}", e.getMessage());
            }
        }
        return false;
    }
}
