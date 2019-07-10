package com.study.signalrouter.service.socket;

import com.alibaba.fastjson.JSONObject;
import com.study.signalrouter.constant.GlobalConstants;
import lombok.extern.slf4j.Slf4j;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Decription
 * <p>
 *     客户端收发器
 * </p>
 * DATE 2019/3/21.
 *
 * @author guijiamin.
 */
@Slf4j
public abstract class SocketTransceiver implements Runnable {
    private Socket socket;
    private String ip;
    private DataInputStream in;
    private DataOutputStream out;
    private boolean runFlag;

    public SocketTransceiver(Socket socket) {
        this.socket = socket;
        this.ip = socket.getInetAddress().getHostAddress();
    }

    public Socket getSocket() {
        return this.socket;
    }

    public String getIp() {
        return this.ip;
    }
    /**
     * 新建一个线程，开启客户端收发
     * 如果开启失败，会断开连接和回调onDisconnect
     */
    public void start() {
        this.runFlag = true;
        new Thread(this).start();
    }

    /**
     * 断开客户端收发（主动）
     * 断开后通过修改runFlag，会断开连接和回调onDisconnect
     */
    public void stop() {
        this.runFlag = false;
        try {
            socket.shutdownInput();
            in.close();//不明白为啥在这需要关闭输入流，因为runFlag修改也会关闭
        } catch (IOException e) {
            log.error("断开客户端socket异常：{}", e.getMessage());
        }
    }

    /**
     * 给客户端发送消息
     * @param s
     * @return 发送成功返回true
     */
    public boolean send(String s) {
        if (out != null) {
            try {
                out.writeUTF(s);
                out.flush();
                return true;
            } catch (IOException e) {
                log.error("写出流异常：{}", e.getMessage());
            }
        }
        return false;
    }

    public void run() {
        try {
            in = new DataInputStream(this.socket.getInputStream());
            out = new DataOutputStream(this.socket.getOutputStream());
        } catch (IOException e) {
            log.error("转换输入输出流异常：{}", e.getMessage());
            this.runFlag = false;
        }
        while (this.runFlag) {
            try {
                final String msg = in.readUTF();
                //TODO 收到消息后处理和回应
                String[] splitMsg = msg.split(GlobalConstants.REGEX.SMILE);
                if (splitMsg.length == 3) {
                    String headerRid = splitMsg[0];
                    String headerUid = splitMsg[1];
                    JSONObject msgBody = JSONObject.parseObject(splitMsg[2]);
                    String msgid = msgBody.getString("msgid");
                    switch (msgid) {
                        case GlobalConstants.MSG_ID.KEEPALIVE://收到15心跳消息
                            this.onProxyHeartBeat(this.ip);//回调抛出到最上层
                            this.send(GlobalConstants.MSG_KEEPALIVE);//回应给下游
                            break;
                        case GlobalConstants.MSG_ID.ENTERROOM://收到201进教室消息
                            //TODO 以http发送给worker，收到回应后处理
                            this.send("{\"msgid\":\"200\",\"msg\":{\"data\": \"收到proxy的进教室消息\",\"srcmagid\":\"201\"}}");//处理：回复给自己状态
                            this.onUserEnterRoom(this.ip, headerRid, headerUid);//处理：回调房间列表广播给当前房间其它用户
                            break;
                        case GlobalConstants.MSG_ID.UNICAST://收到202单播消息
                            break;
                        case GlobalConstants.MSG_ID.BROADCAST://收到203广播消息，发送给router
                            //TODO 以http发送给worker，收到回应后处理
                            this.send("{\"msgid\":\"200\",\"msg\":{\"data\": \"收到proxy的广播消息\",\"srcmagid\":\"203\"}}");//处理：回复确认消息给自己
                            //处理：回调房间列表广播给当前房间其它用户

                            break;
                        default:
                            break;
                    }
                }
            } catch (IOException e) {
                log.error("读取输入流异常：{}", e.getMessage());
                this.runFlag = false;
            }
        }
        //每次执行时，runFlag=false会走到下边
        //断开连接
        try {
            in.close();
            out.close();
            socket.close();
            in = null;
            out = null;
            socket = null;
        } catch (IOException e) {
            e.printStackTrace();
            log.error("断开连接异常：{}", e.getMessage());
        }
        //回调
        this.onDisconnect(this.ip);
    }

    public abstract void onProxyHeartBeat(String proxy);

    public abstract void onUserEnterRoom(String proxy, String rid, String uid);

    public abstract void onUserLeaveRoom(String proxy, String rid, String uid);

    public abstract void onDisconnect(String proxy);
}
