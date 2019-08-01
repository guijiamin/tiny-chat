package com.study.signalrouter.service;

import com.googlecode.protobuf.format.JsonFormat;
import com.study.signalcommon.component.PacketTransceiver;
import com.study.signalcommon.constant.GlobalConstants;
import com.study.signalcommon.protobuf.MessageProto;
import com.study.signalcommon.util.Tool;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.SerializableEntity;
import org.apache.http.util.EntityUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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
    private String ip;

    public String getIp() {
        return this.ip;
    }

    private Socket socket;

    private boolean isSocketAvailable() {
        boolean flag = this.socket != null
                && this.socket.isBound()
                && !this.socket.isClosed()//需要获取closeLock
                && this.socket.isConnected()
                && !this.socket.isInputShutdown()
                && !this.socket.isOutputShutdown();
        if (!flag) {
            log.error("socket is not available");
        }
        return flag;
    }

    private DataInputStream in;
    private DataOutputStream out;

    public SocketTransceiver(Socket socket) throws IOException {
        this.socket = socket;
        this.ip = socket.getInetAddress().getHostAddress();
        //一创建收发器就提取输入输出流，避免每次运行时创建，异常抛出至上层
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
    }

    /**
     * 新建一个线程，开启客户端收发
     */
    public void start() {
        new Thread(this).start();
    }

    /**
     * 断开客户端收发（主动）
     */
    public void close() {
        try {
            this.socket.close();
        } catch (IOException e) {
            log.error("close socket exception：{}", e.getMessage());
        } finally {
            this.socket = null;
            //回调，删除proxy2socket中当前对象数据
            this.onDisconnect(this.ip);
        }
    }


    public void send(int msgid, byte[] bytes) {
        if (isSocketAvailable()) {
            try {
                out.write(GlobalConstants.HEADER.MAGIC);
                out.write(Tool.shortToByteArray((short) bytes.length));
                out.write((byte) msgid);
                out.write(bytes);
            } catch (IOException e) {
                log.error("write data io exception: {}", e.getMessage());
                //TODO 写异常待处理
            } catch (Exception e) {
                log.error("write data other exception: {}", e.getMessage());
            }
        }
    }

    public void run() {
        while (isSocketAvailable()) {
            try {
                int index = 0;
                if (in.available() > 0
                        && Tool.findMagic(in.readByte(), index)
                        && Tool.findMagic(in.readByte(), index + 1)
                        && Tool.findMagic(in.readByte(), index + 2)
                        && Tool.findMagic(in.readByte(), index + 3)) {
                    byte[] contentLenBuffer = new byte[GlobalConstants.HEADER.CONTENT_BYTE_LEN];
                    for (int i = 0; i < contentLenBuffer.length; i++) {
                        contentLenBuffer[i] = in.readByte();
                    }
                    short len = Tool.byteArrayToShort(contentLenBuffer);

                    int msgid = in.read();
                    //在最外层区分心跳和业务消息
                    if (msgid != GlobalConstants.MSG_ID.KEEPALIVE) {
                        byte[] messageProtoBuffer = new byte[len];
                        for (int i = 0; i < len; i++) {
                            messageProtoBuffer[i] = in.readByte();
                        }
                        //收到消息
                        MessageProto.Msg msg = PacketTransceiver.parseMessage(messageProtoBuffer);
                        if (msg == null) return;
                        switch (msgid) {
                            case GlobalConstants.MSG_ID.UNICAST://收到proxy的单播消息
                                break;
                            case GlobalConstants.MSG_ID.BROADCAST://收到proxy的广播消息
                                int msgtype = msg.getMsgtype();
                                switch (msgtype) {
                                    case GlobalConstants.MSG_TYPE.LEAVE://离开教室广播
                                        this.onUserLeaveRoom(this.ip, msg.getFuser().getRid(), new byte[0]);
                                        break;
                                    case GlobalConstants.MSG_TYPE.ENTER://进教室广播
                                        //发送给worker
                                        Future<HttpResponse> future = TcpServer.executor.submit(() -> {
                                                HttpPost httpPost = new HttpPost("http://localhost:8989/broadcast/enter");
                                                httpPost.addHeader("Content-Type", "application/json; charset=utf-8");
                                                httpPost.setEntity(new SerializableEntity(msg.getFuser()));
                                                HttpResponse response = null;
                                                try {
                                                    response = TcpServer.httpclient.execute(httpPost);
                                                } catch (IOException e) {
                                                    log.error(e.getMessage());
                                                }
                                                return response;
                                        });
                                        try {
                                            HttpResponse response = future.get();
                                            //TODO
//                                            byte[] responseMsg = EntityUtils.toByteArray(response.getEntity());
//                                            Map<String, String> extend = new HashMap<>();
//                                            extend.put("data", JsonFormat.printToString(msg.getFuser()));
//
//                                            byte[] routerMsg = PacketTransceiver.packMessage(GlobalConstants.MSG_ID.BROADCAST, GlobalConstants.MSG_TYPE.ENTER, extend, msg.getFuser(), msg.getTuser());
//
//                                            this.onUserEnterRoom(this, msg.getFuser().getRid(), msg.getFuser().getUid(), responseMsg, routerMsg);
                                        } catch (InterruptedException | ExecutionException e) {
                                            log.error("future get exception: {}", e.getMessage());
                                        }
                                        break;
                                    case GlobalConstants.MSG_TYPE.CHAT:
                                        this.onUserChat(true);
                                        break;
                                    default:
                                        break;
                                }
                                break;
                            default:
                                break;
                        }
                    } else {
                        this.onProxyHeartBeat(this.ip);
                        this.send(GlobalConstants.MSG_ID.KEEPALIVE, new byte[0]);//回应0字节报活
                    }
                }
            } catch (IOException e) {
                log.error("read data io exception: {}", e.getMessage());
                //TODO 读数据异常待处理
            } catch (Exception e) {
                log.error("read data other exception: {}", e.getMessage());
            }
        }
    }

    public abstract void onProxyHeartBeat(String proxy);

    public abstract void onUserEnterRoom(SocketTransceiver socket, String rid, String uid, byte[] responseMsg, byte[] routerMsg);

    public abstract void onUserLeaveRoom(String proxy, String rid, byte[] msg);

    public abstract void onUserChat(boolean isBroadCast);

    public abstract void onDisconnect(String proxy);
}
