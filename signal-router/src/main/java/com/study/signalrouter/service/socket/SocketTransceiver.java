package com.study.signalrouter.service.socket;

import com.alibaba.fastjson.JSONObject;
import com.googlecode.protobuf.format.JsonFormat;
import com.study.signalcommon.component.PacketTransceiver;
import com.study.signalcommon.constant.GlobalConstants;
import com.study.signalcommon.protobuf.MessageProto;
import com.study.signalcommon.util.Tool;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Decription
 * <p>
 * 客户端收发器
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
//        try {
//            this.in = new DataInputStream(this.socket.getInputStream());
//            this.out = new DataOutputStream(this.socket.getOutputStream());
//        } catch (IOException e) {
//            log.error("Convert stream exception：{}", e.getMessage());
//        }
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
        System.out.println("为每个proxy开启新线程");
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


    public void send(int msgid, byte[] bytes) {
        if (this.out != null) {
            try {
                this.out.write(GlobalConstants.HEADER.MAGIC);
                this.out.write(Tool.shortToByteArray((short) bytes.length));
                this.out.write((byte) msgid);
                this.out.write(bytes);
                this.out.flush();
            } catch (IOException e) {
                log.error("写出流异常：{}", e.getMessage());
            }
        }
    }

    public void run() {
        try {
            this.in = new DataInputStream(this.socket.getInputStream());
            this.out = new DataOutputStream(this.socket.getOutputStream());
        } catch (IOException e) {
            log.error("Convert stream exception：{}", e.getMessage());
            this.runFlag = false;
        }
        while (this.runFlag) {
            try {
                int index = 0;
                if (this.in.available() > 0
                        && Tool.findMagic(this.in.readByte(), index)
                        && Tool.findMagic(this.in.readByte(), index + 1)
                        && Tool.findMagic(this.in.readByte(), index + 2)
                        && Tool.findMagic(this.in.readByte(), index + 3)) {
                    byte[] contentLenBuffer = new byte[GlobalConstants.HEADER.CONTENT_BYTE_LEN];
                    for (int i = 0; i < contentLenBuffer.length; i++) {
                        contentLenBuffer[i] = this.in.readByte();
                    }
                    short len = Tool.byteArrayToShort(contentLenBuffer);
                    System.out.println("消息体长度：len="+len);
                    int msgid = this.in.read();
                    System.out.println("读取到消息：msgid="+msgid);
                    if (msgid != GlobalConstants.MSG_ID.KEEPALIVE) {
                        byte[] messageProtoBuffer = new byte[len];
                        for (int i = 0; i < len; i++) {
                            messageProtoBuffer[i] = this.in.readByte();
                        }
                        //收到消息
                        MessageProto.Msg msg = PacketTransceiver.parseMessage(messageProtoBuffer);
                        switch (msgid) {
                            //收到用户进教室消息
                            //1、发送给worker持久化
                            //2、找到该房间的所有proxy连接，发送103/1消息
                            case GlobalConstants.MSG_ID.ENTERROOM:
                                System.out.println("准备发送给worker");
                                //TODO 发送给worker
                                Future<HttpResponse> future = TcpServer.executor.submit(new Callable<HttpResponse>() {
                                    @Override
                                    public HttpResponse call() {
                                        HttpPost httpPost = new HttpPost("http://localhost:8989/chat");
                                        httpPost.addHeader("Content-Type", "application/json; charset=utf-8");
                                        httpPost.setEntity(new ByteArrayEntity(messageProtoBuffer));
                                        HttpResponse response = null;
                                        try {
                                            response = TcpServer.httpclient.execute(httpPost);
                                        } catch (IOException e) {
                                            log.error(e.getMessage());
                                        }
                                        return response;
                                    }
                                });
                                try {
                                    HttpResponse response = future.get();
                                    log.info("回应：{}", response);
                                    //TODO 回调
                                    byte[] responseMsg = EntityUtils.toByteArray(response.getEntity());
                                    Map<String, String> extend = new HashMap<>();
                                    extend.put("data", JsonFormat.printToString(msg.getFuser()));
                                    byte[] routerMsg = PacketTransceiver.packMessage(GlobalConstants.MSG_ID.BROADCAST, GlobalConstants.MSG_TYPE.ENTER, extend, msg.getFuser(), msg.getTuser());
                                    this.onUserEnterRoom(this, msg.getFuser().getRid(), msg.getFuser().getUid(), responseMsg, routerMsg);
                                } catch (InterruptedException | ExecutionException e) {
                                    log.error("Futer get exception: {}", e.getMessage());
                                }
                                break;
                            //收到用户广播消息
                            //1、发送给worker持久化
                            case GlobalConstants.MSG_ID.BROADCAST:
                                break;
                            case GlobalConstants.MSG_ID.UNICAST:
                                break;
                            default:
                                break;
                        }
                    } else {
                        this.onProxyHeartBeat(this.ip);
                        this.send(GlobalConstants.MSG_ID.KEEPALIVE,
                                PacketTransceiver.packMessage(
                                        GlobalConstants.MSG_ID.KEEPALIVE,
                                        GlobalConstants.MSG_TYPE.NOTHING,
                                        GlobalConstants.USER.HEARTBEAT,
                                        GlobalConstants.USER.HEARTBEAT
                                ));
                    }
                } else {
                    //错误消息
//                    System.out.println("Not magic, abort bytes");
                }
            } catch (IOException e) {
                log.error("Read input stream exception: {}", e.getMessage());
//                e.printStackTrace();
//                this.runFlag = false;
            }
//            try {
//                final String msg = in.readUTF();
//                String[] splitMsg = msg.split(GlobalConstants.REGEX.SMILE);
//                if (splitMsg.length == 3) {
//                    String headerRid = splitMsg[0];
//                    String headerUid = splitMsg[1];
//                    JSONObject msgBody = JSONObject.parseObject(splitMsg[2]);
//                    Integer msgid = msgBody.getInteger("msgid");
//                    switch (msgid) {
//                        case GlobalConstants.MSG_ID.KEEPALIVE://收到心跳消息
//                            this.onProxyHeartBeat(this.ip);//回调抛出到最上层
////                            this.send(GlobalConstants.MSG_KEEPALIVE);//回应给下游
//                            break;
//                        case GlobalConstants.MSG_ID.ENTERROOM://收到进教室消息
//                            //TODO 以http发送给worker，收到回应后处理
////                            this.send("{\"msgid\":\"200\",\"msg\":{\"data\": \"收到proxy的进教室消息\",\"srcmsgid\":\"201\"}}");//处理：回复给自己状态
////                            this.onUserEnterRoom(this.ip, headerRid, headerUid);//处理：回调房间列表广播给当前房间其它用户
//                            Future<HttpResponse> future = TcpServer.executor.submit(new Callable<HttpResponse>() {
//                                @Override
//                                public HttpResponse call() {
//                                    HttpPost httpPost = new HttpPost("http://localhost:8080/chat");
//                                    httpPost.addHeader("Content-Type", "application/json; charset=utf-8");
//                                    httpPost.setEntity(new StringEntity(msgBody.toString(), "utf-8"));
//                                    HttpResponse response = null;
//                                    try {
//                                        response = TcpServer.httpclient.execute(httpPost);
//                                    } catch (IOException e) {
//                                        log.error(e.getMessage());
//                                    }
//                                    return response;
//                                }
//                            });
//                            try {
//                                HttpResponse response = future.get();
//                                log.info("回应：{}", EntityUtils.toString(response.getEntity()));
//                            } catch (Exception e) {
//
//                            }
//                            break;
//                        case GlobalConstants.MSG_ID.BROADCAST://收到广播消息，发送给router
//                            //TODO 以http发送给worker，收到回应后处理
////                            this.send("{\"msgid\":\"200\",\"msg\":{\"data\": \"收到proxy的广播消息\",\"srcmagid\":\"203\"}}");//处理：回复确认消息给自己
//                            //处理：回调房间列表广播给当前房间其它用户
//
//                            break;
//                        case GlobalConstants.MSG_ID.UNICAST://收到单播消息
//                            break;
//                        default:
//                            break;
//                    }
//                }
//            } catch (IOException e) {
//                log.error("读取输入流异常：{}", e.getMessage());
//                this.runFlag = false;
//            }
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
            log.error("断开连接异常：{}", e.getMessage());
        }
        //回调
        this.onDisconnect(this.ip);
    }

    public abstract void onProxyHeartBeat(String proxy);

//    public abstract void onUserEnterRoom(String proxy, String rid, String uid);

    public abstract void onUserEnterRoom(SocketTransceiver socket, String rid, String uid, byte[] responseMsg, byte[] routerMsg);

//    public abstract void onUserLeaveRoom(String proxy, String rid, String uid);

    public abstract void onUserLeaveRoom(String proxy, String rid, byte[] msg);

    public abstract void onDisconnect(String proxy);
}
