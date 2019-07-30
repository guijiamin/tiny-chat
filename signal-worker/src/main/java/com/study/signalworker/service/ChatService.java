package com.study.signalworker.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.googlecode.protobuf.format.JsonFormat;
import com.study.signalcommon.constant.GlobalConstants;
import com.study.signalcommon.protobuf.MessageProto;
import com.study.signalcommon.util.Tool;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * Decription
 * <p>
 * </p>
 * DATE 2019/7/17.
 *
 * @author guijiamin.
 */
@Service
public class ChatService {
    @Resource(name = "rd")
    private RedisTemplate<String, String> redisTemplate;

//    public JSONObject chat(JSONObject req) {
//        String msgid = req.getString("msgid");
//        String rid = req.getString("rid");
//        String uid = req.getString("uid");
//        String name = req.getString("name");
//        String img = req.getString("img");
//        JSONObject jsonObject = new JSONObject();
//        jsonObject.put("msgid", "200");
//        jsonObject.put("rid", rid);
//        jsonObject.put("uid", uid);
//        JSONObject msg = new JSONObject();
//        msg.put("srcmsgid", "201");
//        switch (msgid) {
//            case "201":
//                redisTemplate.opsForHash().put("SIGNAL_WORKER:USER_LIST:" + rid, uid, req.toJSONString());
//                List<Object> values = redisTemplate.opsForHash().values("SIGNAL_WORKER:USER_LIST:" + rid);
//                msg.put("data", values);
//                break;
//            case "202":
//                break;
//            case "203":
//                break;
//            default:
//                break;
//        }
//        jsonObject.put("msg", msg);
//        return jsonObject;
//    }

    public MessageProto.Msg chat(MessageProto.Msg msg) {
        int msgid = msg.getMsgid();
        String rid = msg.getFuser().getRid();
        String uid = msg.getFuser().getUid();

        MessageProto.Msg.Builder response = MessageProto.Msg.newBuilder();
        response.setMsgid(GlobalConstants.MSG_ID.REPLY);
        response.setMsgtype(GlobalConstants.MSG_TYPE.NOTHING);
        response.setFuser(msg.getFuser());
        response.setTuser(msg.getTuser());
        switch (msgid) {
            case GlobalConstants.MSG_ID.ENTERROOM:
                String redisKey = Tool.getUserListKeyByWorker(rid);
                redisTemplate.opsForHash().put(redisKey, uid, JsonFormat.printToString(msg.getFuser()));
                List<Object> values = redisTemplate.opsForHash().values(redisKey);
                response.putExtend("data", values.toString());
                break;
            case GlobalConstants.MSG_ID.BROADCAST:
                break;
            case GlobalConstants.MSG_ID.UNICAST:
                break;
            default:
                break;
        }
        System.out.println(response.getExtendMap().get("data"));
        System.out.println(JSONArray.parseArray(response.getExtendMap().get("data")).size());
        return response.build();
    }
}
