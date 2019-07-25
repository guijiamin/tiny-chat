package com.study.signalworker.service;

import com.alibaba.fastjson.JSONObject;
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

    public JSONObject chat(JSONObject req) {
        String msgid = req.getString("msgid");
        String rid = req.getString("rid");
        String uid = req.getString("uid");
        String name = req.getString("name");
        String img = req.getString("img");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("msgid", "200");
        jsonObject.put("rid", rid);
        jsonObject.put("uid", uid);
        JSONObject msg = new JSONObject();
        msg.put("srcmsgid", "201");
        switch (msgid) {
            case "201":
                redisTemplate.opsForHash().put("SIGNAL_WORKER:USER_LIST:" + rid, uid, req.toJSONString());
                List<Object> values = redisTemplate.opsForHash().values("SIGNAL_WORKER:USER_LIST:" + rid);
                msg.put("data", values);
                break;
            case "202":
                break;
            case "203":
                break;
            default:
                break;
        }
        jsonObject.put("msg", msg);
        return jsonObject;
    }
}
