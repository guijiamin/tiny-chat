package com.study.signalproxy.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.study.signalproxy.constant.GlobalConstants;
import com.study.signalproxy.dto.Event;
import com.study.signalproxy.dto.Proxy2RouterEvent;
import com.study.signalproxy.util.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.InetSocketAddress;
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
    private RedisTemplate<String, String> rd = (RedisTemplate<String, String>) SpringUtil.getBean("rd");

    private static Map<String, HashMap<String, WebSocket>> onlineMap = new ConcurrentHashMap<>();
    private TimeWheel<String, WebSocket> timeWheel;
    //TODO 第二版：接入中心转发层
    private TcpClient router;

    public ProxyServer() {
        this(8787);
    }

    public ProxyServer(int port) {
        super(new InetSocketAddress(port));
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
        String name = param.get("name");
        String img = param.get("img");

        if (onlineMap.containsKey(rid)) {//如果在线列表里包括这个房间，加入该房间
            HashMap<String, WebSocket> userConnMap = onlineMap.get(rid);
            if (userConnMap.containsKey(uid)) {
                log.info("A new connection opened and kick old connection, rid: {}, uid: {}", rid, uid);
                userConnMap.get(uid).close(1000);
            }
            log.info("New connection {} join old rid: {}, uid: {}", uid, rid);
            userConnMap.put(uid, conn);
        } else {//如果在线列表里不包括这个房间，新建房间
            log.info("New connection {} join new rid: {}", uid, rid);
            HashMap<String, WebSocket> userConnMap = new HashMap<>();
            userConnMap.put(uid, conn);
            onlineMap.put(rid, userConnMap);
        }
        timeWheel.add(rid + GlobalConstants.SYMBOL.SMILE + uid, conn);//添加到心跳时间轮
        System.out.println("当前房间个数:" + onlineMap.size());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("A connection is closed: " + conn.getResourceDescriptor() + ", exit code: " + code + ", reason: " + reason);

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

        handleClose(rid, uid);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        log.info("Receive msg: {} from: {}", message, conn.getRemoteSocketAddress());
        JSONObject req = JSONObject.parseObject(message);
        String msgid = req.getString("msgid");
        String rid = req.getString("rid");
        String uid = req.getString("uid");
        String name = req.getString("name");
        String img = req.getString("img");
//        //第一版：只有signal-proxy
//        JSONObject reqMsg = req.getJSONObject("msg");
//        if (msgid.equals(GlobalConstants.MSG_ID.ENTERROOM)) {
//            JSONObject val = new JSONObject() {{
//                put("rid", rid);
//                put("uid", uid);
//                put("name", name);
//                put("img", img);
//            }};
//            rd.opsForHash().put("USERS_" + rid, uid, val.toJSONString());
//            rd.expire(rid, 1, TimeUnit.DAYS);
//            //进教室消息：给自己回应当前教室列表，教室其它用户发送广播203-1
//            Map<Object, Object> roomInfo = rd.opsForHash().entries("USERS_" + rid);
//            List<String> messages = rd.opsForList().range("MESSAGES_" + rid, 0, -1);
//            for (Map.Entry<String, WebSocket> entry : onlineMap.get(rid).entrySet()) {
//                if (entry.getKey().equals(uid)) {
//                    String users = roomInfo.values().toString();
//                    entry.getValue().send(packageReply(rid, uid, GlobalConstants.MSG_ID.ENTERROOM, new JSONObject() {{
//                        put("users", users);
//                        put("messages", messages.toString());
//                    }}.toString()));
//                } else {
//                    JSONObject resp = new JSONObject();
//                    resp.put("msgid", GlobalConstants.MSG_ID.BROADCAST);
//                    resp.put("rid", rid);
//                    resp.put("uid", uid);
//                    JSONObject respMsg = new JSONObject();
//                    respMsg.put("msgtype", GlobalConstants.MSG_TYPE.ENTER);
//                    respMsg.put("srcmsgid", GlobalConstants.MSG_ID.ENTERROOM);
//                    respMsg.put("data", new JSONObject() {{
//                        put("rid", rid);
//                        put("uid", uid);
//                        put("name", name);
//                        put("img", img);
//                    }});
//                    resp.put("msg", respMsg);
//                    entry.getValue().send(JSON.toJSONString(resp));
//                }
//            }
//        } else if (msgid.equals(GlobalConstants.MSG_ID.UNICAST)) {
//        } else if (msgid.equals(GlobalConstants.MSG_ID.BROADCAST)) {
//            //广播消息：给自己响应，其它用户推送
//            String msgtype = reqMsg.getString("msgtype");
//            String data = reqMsg.getString("data");
//            if (msgtype.equals(GlobalConstants.MSG_TYPE.CHAT)) {
//                for (Map.Entry<String, WebSocket> entry : onlineMap.get(rid).entrySet()) {
//                    if (entry.getKey().equals(uid)) {
//                        entry.getValue().send(packageReply(rid, uid, GlobalConstants.MSG_ID.BROADCAST, null));
//                    } else {
//                        JSONObject resp = new JSONObject();
//                        resp.put("msgid", GlobalConstants.MSG_ID.BROADCAST);
//                        resp.put("rid", rid);
//                        resp.put("uid", uid);
//                        resp.put("img", img);
//                        resp.put("name", name);
//                        JSONObject respMsg = new JSONObject();
//                        respMsg.put("msgtype", GlobalConstants.MSG_TYPE.CHAT);
//                        respMsg.put("srcmsgid", GlobalConstants.MSG_ID.BROADCAST);
//                        respMsg.put("data", data);
//                        resp.put("msg", respMsg);
//                        entry.getValue().send(JSON.toJSONString(resp));
//                    }
//                }
//            }
//            JSONObject msg = new JSONObject();
//            msg.put("rid", rid);
//            msg.put("uid", uid);
//            msg.put("img", img);
//            msg.put("name", name);
//            msg.put("content", data);
//            Long currentIndex = rd.opsForList().rightPush("MESSAGES_" + rid, JSON.toJSONString(msg));
//            if (currentIndex > 5) {
//                rd.opsForList().leftPop("MESSAGES_" + rid);
//            }
//        } else if (msgid.equals(GlobalConstants.MSG_ID.KEEPALIVE)) {
//            //心跳消息：给自己响应
//            conn.send(packageReply(rid, uid, GlobalConstants.MSG_ID.KEEPALIVE, null));
//            //加入到时间轮里
//            timeWheel.add(rid + GlobalConstants.SYMBOL.SMILE + uid, conn);
//        }
        //TODO 第二版：接入中心转发层
        if (msgid.equals(GlobalConstants.MSG_ID.KEEPALIVE)) {
            //心跳消息：给自己响应
            conn.send(packageReply(rid, uid, GlobalConstants.MSG_ID.KEEPALIVE, null));
            //加入到时间轮里
            timeWheel.add(rid + GlobalConstants.SYMBOL.SMILE + uid, conn);
        } else {
            //收到客户端消息生产Proxy2RouterEvent到消息队列中
            //格式为：rid|^_^|uid|^_^|message
            Event event = new Proxy2RouterEvent(this.router, rid + GlobalConstants.SYMBOL.SMILE + uid + GlobalConstants.SYMBOL.SMILE + message);
            EventQueue.getInstance().produce(event);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("An error occured on connection " + conn.getRemoteSocketAddress() + ":" + conn.getResourceDescriptor() + ", ex: " + ex);
    }

    @Override
    public void onStart() {
        System.out.println("ProxyServer started successfully");
        //心跳时间轮
        this.timeWheel = new TimeWheel<String, WebSocket>(1, 60, TimeUnit.SECONDS);
        this.timeWheel.addExpirationListener(new WebSocketExpirationListener<WebSocket>());
        this.timeWheel.start();

        //TODO 第二版：建立proxy到router的tcp连接（包括接收router消息线程和定时发送心跳线程）
        this.router = new TcpClient("127.0.0.1", 5555);
        this.router.connect();
        //开启消息事件消费者
        new Thread(new EventConsumer()).start();
    }

    private void handleClose(String rid, String uid) {
        if (!onlineMap.containsKey(rid)) {
            return;
        }

        for (Map.Entry<String, WebSocket> entry : onlineMap.get(rid).entrySet()) {
            if (!entry.getKey().equals(uid)) {
                try {
                    JSONObject resp = new JSONObject();
                    resp.put("msgid", GlobalConstants.MSG_ID.BROADCAST);
                    resp.put("rid", rid);
                    resp.put("uid", uid);
                    JSONObject respMsg = new JSONObject();
                    respMsg.put("msgtype", GlobalConstants.MSG_TYPE.LEAVE);
                    respMsg.put("srcmsgid", GlobalConstants.MSG_ID.BROADCAST);
                    respMsg.put("data", JSONObject.parseObject(rd.opsForHash().get("USERS_" + rid, uid).toString()));
                    resp.put("msg", respMsg);
                    entry.getValue().send(resp.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        rd.opsForHash().delete("USERS_" + rid, uid);//有人退出，从redis中删除
        if (onlineMap.get(rid).containsKey(uid)) {
            onlineMap.get(rid).remove(uid);

            if (onlineMap.get(rid).size() == 0) {//如果删除后当前房间没有用户，将当前房间列表删除
                onlineMap.remove(rid);
            }
        }
    }

    private String packageReply(String rid, String uid, String srcmsgid, String data) {
        JSONObject resp = new JSONObject();
        resp.put("msgid", GlobalConstants.MSG_ID.REPLY);
        resp.put("rid", rid);
        resp.put("uid", uid);
        JSONObject respMsg = new JSONObject();
        respMsg.put("srcmsgid", srcmsgid);
        if (srcmsgid.equals(GlobalConstants.MSG_ID.ENTERROOM)) {
            respMsg.put("data", data);//暂时先推送房间的人数
        } else {
            respMsg.put("data", "ok");//给自己的响应为ok
        }
        resp.put("msg", respMsg);
        return JSON.toJSONString(resp);
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

    public static void main(String[] args) throws IOException {
        WebSocketServer server = new ProxyServer(8787);
        server.run();
    }
}

