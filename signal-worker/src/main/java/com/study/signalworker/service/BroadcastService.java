package com.study.signalworker.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.googlecode.protobuf.format.JsonFormat;
import com.study.signalcommon.constant.GlobalConstants;
import com.study.signalcommon.dto.Chat;
import com.study.signalcommon.protobuf.MessageProto;
import com.study.signalcommon.util.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Decription
 * <p>
 * </p>
 * DATE 2019/7/17.
 *
 * @author guijiamin.
 */
@Slf4j
@Service
public class BroadcastService {
    @Resource(name = "rd")
    private RedisTemplate<String, String> redisTemplate;

    public boolean leave(MessageProto.User user) {
        try {
            String userListRedisKey = Tool.getUserListKeyByWorker(user.getRid());
            Long delete = redisTemplate.opsForHash().delete(userListRedisKey, user.getUid());
            redisTemplate.expire(userListRedisKey, 1, TimeUnit.MINUTES);//设置一天过期
            if (delete > 0) {
                return true;
            }
        } catch (Exception e) {
            log.error("{}", e.getMessage());
        }
        return false;
    }

    public Map<String, String> enter(MessageProto.User user) {
        Map<String, String> roomInfo = new HashMap<>();
        try {
            String rid = user.getRid();
            String userListRedisKey = Tool.getUserListKeyByWorker(rid);
            String chatListRedisKey = Tool.getChatListKeyByWorker(rid);
            redisTemplate.opsForHash().put(userListRedisKey, user.getUid(), JsonFormat.printToString(user));
            redisTemplate.expire(userListRedisKey, 1, TimeUnit.MINUTES);//设置一天过期
            roomInfo.put(GlobalConstants.KEY.USERS, redisTemplate.opsForHash().values(userListRedisKey).toString());
            roomInfo.put(GlobalConstants.KEY.CHATS, redisTemplate.opsForList().range(chatListRedisKey, 0, -1).toString());
        } catch (Exception e) {
            log.error("{}", e.getMessage());
        }
        return roomInfo;
    }

    public boolean chat(Chat chat) {
        try {
            String chatListRedisKey = Tool.getChatListKeyByWorker(chat.getRid());
            Long currentIndex = redisTemplate.opsForList().rightPush(chatListRedisKey, JSONObject.toJSONString(chat));
            redisTemplate.expire(chatListRedisKey, 1, TimeUnit.MINUTES);//设置最后一条消息后一天过期
            if (currentIndex > 5) {
                redisTemplate.opsForList().leftPop(chatListRedisKey);
            }
            if (currentIndex >= 0) {
                return true;
            }
        } catch (Exception e) {
            log.error("{}", e.getMessage());
        }
        return false;
    }
}
