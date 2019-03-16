package com.study.signalproxy.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.study.signalproxy.constant.GlobalConstants;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
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

    private RedisTemplate redisTemplate;

    public void setRedisTemplate(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private Map<String, HashMap<String, WebSocket>> onlineMap = new ConcurrentHashMap<>();
    private TimeWheel<String, WebSocket> timeWheel;

    public ProxyServer(InetSocketAddress address) {
        super(address);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("新的连接建立: " + conn.getRemoteSocketAddress() + "，" + conn.getResourceDescriptor());
        Map<String, String> param = parse(conn.getResourceDescriptor());
        if (param == null || !param.containsKey("rid") || !param.containsKey("uid")) {
            return;
        }
        String rid = param.get("rid");
        String uid = param.get("uid");

        String alias = param.get("alias");
        if (onlineMap.containsKey(rid)) {
            HashMap<String, WebSocket> userConnMap = onlineMap.get(rid);
            if (userConnMap.containsKey(uid)) {
                System.out.println("重复建立连接，踢掉旧连接: " + rid + "，" + uid);
                userConnMap.get(uid).close(1000);
            }
            System.out.println("原有房间加入新成员: " + rid + "，" + uid);
            userConnMap.put(uid, conn);
        } else {
            System.out.println("创建新房间: " + rid + "，" + uid);
            HashMap<String, WebSocket> userConnMap = new HashMap<>();
            userConnMap.put(uid, conn);
            onlineMap.put(rid, userConnMap);
        }
        timeWheel.add(rid + "@" + uid, conn);
        System.out.println("当前房间个数:" + onlineMap.size());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("closed " + conn.getRemoteSocketAddress() + " with exit code " + code + " additional info: " + reason);

        Map<String, String> param = parse(conn.getResourceDescriptor());

        if (param == null || !param.containsKey("rid") || !param.containsKey("uid")) {
            return;
        }
        String rid = param.get("rid");

        if (!onlineMap.containsKey(rid)) {
            return;
        }
        String uid = param.get("uid");

        handleClose(rid, uid);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("收到消息:  " + conn.getRemoteSocketAddress() + ": " + message);

        JSONObject req = JSONObject.parseObject(message);
        String msgid = req.getString("msgid");
        String frid = req.getString("frid");
        String fuid = req.getString("fuid");
        JSONObject reqMsg = req.getJSONObject("msg");

        if (msgid.equals(GlobalConstants.MSG_ID.ENTERROOM)) {
            //进教室消息：给自己回应当前教室列表，教室其它用户发送广播203-1
            handleBroadcast(frid, fuid, GlobalConstants.MSG_ID.ENTERROOM, GlobalConstants.MSG_TYPE.ENTER, null);
        } else if (msgid.equals(GlobalConstants.MSG_ID.UNICAST)) {

        } else if (msgid.equals(GlobalConstants.MSG_ID.BROADCAST)) {
            //广播消息：给给自己响应，其它用户推送
            handleBroadcast(frid, fuid, GlobalConstants.MSG_ID.BROADCAST, GlobalConstants.MSG_TYPE.CHAT, reqMsg.getString("data"));
        } else if (msgid.equals(GlobalConstants.MSG_ID.KEEPALIVE)) {
            //心跳消息：给自己响应
            conn.send(packageReply(frid, fuid, GlobalConstants.MSG_ID.KEEPALIVE));
            //加入到时间轮里
            timeWheel.add(frid + "@" + fuid, conn);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("an error occured on connection " + conn.getRemoteSocketAddress() + ":" + conn.getResourceDescriptor());
    }

    @Override
    public void onStart() {
        System.out.println("server started successfully");
        timeWheel = new TimeWheel<String, WebSocket>(1, 60, TimeUnit.SECONDS);
        timeWheel.addExpirationListener(new WebSocketExpirationListener<WebSocket>());

        timeWheel.start();
    }

    private void handleBroadcast(String rid, String uid, String srcmsgid, String msgtype, String data) {
        if (onlineMap.containsKey(rid)) {
            //遍历当前房间的所有用户
            for (Map.Entry<String, WebSocket> entry : onlineMap.get(rid).entrySet()) {
                if (entry.getKey().equals(uid)) {//给自己发送响应
                    entry.getValue().send(packageReply(rid, uid, srcmsgid));
                } else {//给其它用户发送指定消息
                    JSONObject resp = new JSONObject();
                    resp.put("msgid", GlobalConstants.MSG_ID.BROADCAST);
                    resp.put("frid", rid);
                    resp.put("fuid", uid);
                    JSONObject respMsg = new JSONObject();
                    respMsg.put("msgtype", msgtype);
                    respMsg.put("srcmsgid", srcmsgid);
                    respMsg.put("trid", rid);
                    respMsg.put("tuid", uid);
                    if (data == null) {
                        respMsg.put("data", String.valueOf(onlineMap.get(rid).size()));//暂时先推送房间的人数
                    } else {
                        respMsg.put("data", data);
                    }
                    resp.put("msg", respMsg);
                    entry.getValue().send(JSON.toJSONString(resp));
                }
            }
        }
    }

    private void handleClose(String rid, String uid) {
        if (!onlineMap.containsKey(rid)) {
            return;
        }

        if (onlineMap.get(rid).containsKey(uid)) {
            onlineMap.get(rid).remove(uid);

            if (onlineMap.get(rid).size() == 0) {//如果删除后当前房间没有用户，将当前房间列表删除
                onlineMap.remove(rid);
            }
        }

        handleBroadcast(rid, uid, GlobalConstants.MSG_ID.BROADCAST, GlobalConstants.MSG_TYPE.LEAVE, null);
    }

    private String packageReply(String rid, String uid, String srcmsgid) {
        JSONObject resp = new JSONObject();
        resp.put("msgid", GlobalConstants.MSG_ID.REPLY);
        resp.put("frid", rid);
        resp.put("fuid", uid);
        JSONObject respMsg = new JSONObject();
        respMsg.put("srcmsgid", srcmsgid);
        respMsg.put("trid", rid);
        respMsg.put("tuid", uid);
        if (srcmsgid.equals(GlobalConstants.MSG_ID.ENTERROOM)) {
            respMsg.put("data", String.valueOf(onlineMap.get(rid).size()));//暂时先推送房间的人数
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

    private class ExamineHeartThread implements Runnable {
        public void run() {
            while (true) {
                System.out.println("Start ExamineHeart...");
                try {
//                    long curTs = new Date().getTime();
//                    Iterator<UserEntity> iterator = connections.iterator();
//                    while (iterator.hasNext()) {
//                        UserEntity entity = iterator.next();
//                        long lastTs = entity.getLastHeartBeatTs();
//                        if (lastTs != 0 && (curTs - lastTs) > 20000) {
//                            onClose(entity.getWsConn(), 1000, "No Hearbeat", false);
//                        }
//                    }
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        String host = "localhost";
        int port = 8887;

        WebSocketServer server = new ProxyServer(new InetSocketAddress(host, port));
        server.run();
    }
}

