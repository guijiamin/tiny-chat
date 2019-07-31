package com.study.signalproxy;

import com.study.signalcommon.component.PacketTransceiver;
import com.study.signalcommon.component.TimeWheel;
import com.study.signalcommon.constant.GlobalConstants;
import com.study.signalcommon.protobuf.MessageProto;
import com.study.signalcommon.util.Tool;
import com.study.signalproxy.dto.Event;
import com.study.signalproxy.dto.Proxy2RouterEvent;
import com.study.signalproxy.service.*;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Decription
 * <p>
 * </p>
 * DATE 19/3/4.
 *
 * @author guijiamin.
 */
@Slf4j
public class ProxyServer extends WebSocketServer {
    public final static Map<String, HashMap<String, WebSocket>> onlineMap = new ConcurrentHashMap<>();
    protected final static TimeWheel<String, WebSocket> webSocketTimeWheel = new TimeWheel<String, WebSocket>(1, 60, TimeUnit.SECONDS, new WebSocketExpirationListener<WebSocket>());
    protected final static TimeWheel<String, TcpClient> tcpClientTimeWheel = new TimeWheel<String, TcpClient>(1, 120, TimeUnit.SECONDS, new TcpClientExpirationListener<TcpClient>());

    private TcpClient router;

    public ProxyServer() {
        this(GlobalConstants.SERVER_PORT.PROXY);
    }

    public ProxyServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onStart() {
        log.info("ProxyServer started successfully");
        //心跳时间轮
//        webSocketTimeWheel.start();
        tcpClientTimeWheel.start();
        //建立proxy到router的tcp连接（包括接收router消息线程和定时发送心跳线程）
        this.router = new TcpClient("127.0.0.1", GlobalConstants.SERVER_PORT.ROUTER);
        this.router.start();
        //开启线程消费事件消息
        new Thread(new EventConsumer()).start();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        log.info("A new connection opened: {}, {}", conn.getRemoteSocketAddress(), conn.getResourceDescriptor());

        Map<String, String> param = parse(conn.getResourceDescriptor());
        if (param == null || !param.containsKey("rid") || !param.containsKey("uid") || !param.containsKey("name") || !param.containsKey("img")) {
            return;
        }
        String rid = param.get("rid");
        String uid = param.get("uid");

        if (onlineMap.containsKey(rid)) {//如果在线列表里包括这个房间，加入该房间
            HashMap<String, WebSocket> userConnMap = onlineMap.get(rid);
            if (userConnMap.containsKey(uid)) {
                log.info("A new connection opened and kick old connection, rid: {}, uid: {}", rid, uid);
                userConnMap.get(uid).close(1000);//如果是重复连接，踢掉原有连接
            }
            log.info("New connection {} join old rid: {}", uid, rid);
            userConnMap.put(uid, conn);
        } else {//如果在线列表里不包括这个房间，新建房间
            log.info("New connection {} join new rid: {}", uid, rid);
            HashMap<String, WebSocket> userConnMap = new HashMap<>();
            userConnMap.put(uid, conn);
            onlineMap.put(rid, userConnMap);
        }
        //添加到心跳时间轮
//        webSocketTimeWheel.add(rid + GlobalConstants.SYMBOL.AT + uid, conn);
        System.out.println("当前房间个数:" + onlineMap.size());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        log.warn("A connection is closed: " + conn.getResourceDescriptor() + ", exit code: " + code + ", reason: " + reason);

        Map<String, String> param = parse(conn.getResourceDescriptor());
        if (param == null || !param.containsKey("rid") || !param.containsKey("uid")) {
            return;
        }
        String rid = param.get("rid");
        if (!onlineMap.containsKey(rid)) {
            return;
        }

        String uid = param.get("uid");
        if (!onlineMap.get(rid).containsKey(uid)) {
            return;
        }
        //从时间轮里移除当前用户
//        webSocketTimeWheel.remove(rid + GlobalConstants.SYMBOL.AT + uid);

        //对于当前房间其它用户，需要发送广播消息
        for (Map.Entry<String, WebSocket> entry : onlineMap.get(rid).entrySet()) {
            if (!entry.getKey().equals(uid)) {
                //TODO 发送103/2消息给router
            }
        }
        if (onlineMap.get(rid).containsKey(uid)) {
            onlineMap.get(rid).remove(uid);
            if (onlineMap.get(rid).size() == 0) {//如果删除后当前房间没有用户，将当前房间列表删除
                onlineMap.remove(rid);
            }
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        log.info("receive string msg: {} from: {}", message, conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        MessageProto.Msg msg = PacketTransceiver.parseMessage(message.array());
        log.info("receive byte msg: {} from: {}", msg, conn.getRemoteSocketAddress());

        if (msg == null) return;

        int msgid = msg.getMsgid();
        String frid = msg.getFuser().getRid();
        String fuid = msg.getFuser().getUid();
        if (Tool.isEmpty(msgid) || Tool.isEmpty(frid) || Tool.isEmpty(fuid)) {
            log.warn("unvalid message");
            return;
        }
        if (msgid == GlobalConstants.MSG_ID.KEEPALIVE) {//收到客户端心跳消息
            //1、回应
            conn.send(PacketTransceiver.packMessage(GlobalConstants.MSG_ID.KEEPALIVE, GlobalConstants.MSG_TYPE.ENTER, msg.getFuser(), msg.getTuser()));
            //2、激活时间轮
//            webSocketTimeWheel.add(frid + GlobalConstants.SYMBOL.AT + fuid, conn);
        } else {//收到客户端除心跳以外消息
            //1、使用指定的router生成事件（为便于选择不同的router发送）
            System.out.println("生产消息事件：msgid=" + msgid);
            Event event = new Proxy2RouterEvent(this.router, msgid, message.array());
            //2、生产Proxy2RouterEvent到消息队列中
            EventQueue.getInstance().produce(event);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        log.error("an error occured on conn " + conn.getRemoteSocketAddress() + ":" + conn.getResourceDescriptor() + ", ex: " + ex);
    }

    private Map<String, String> parse(String req) {
        String[] urlParts = req.split("\\?");
        if (urlParts.length <= 1) {
            return null;
        }
        Map<String, String> paramMap = new HashMap<String, String>();
        String[] params = urlParts[1].split("&");
        for (String param : params) {
            String[] kv = param.split("=");
            paramMap.put(kv[0], kv[1]);
        }
        return paramMap;
    }

    public static void main(String[] args) {
        new ProxyServer(GlobalConstants.SERVER_PORT.PROXY).start();
    }
}

