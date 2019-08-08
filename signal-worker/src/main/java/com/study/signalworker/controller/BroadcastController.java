package com.study.signalworker.controller;

import com.google.protobuf.InvalidProtocolBufferException;
import com.study.signalcommon.dto.Chat;
import com.study.signalcommon.dto.JsonResult;
import com.study.signalcommon.protobuf.MessageProto;
import com.study.signalworker.service.BroadcastService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

/**
 * Decription
 * <p>
 * </p>
 * DATE 2019/7/17.
 *
 * @author guijiamin.
 */
@Slf4j
@RestController()
@RequestMapping("/broadcast")
public class BroadcastController {
    @Resource
    private BroadcastService broadcastService;

    //用户离开：删除一个用户
    @PostMapping("/leave")
    public JsonResult leave(@RequestBody byte[] user) {
        JsonResult jsonResult = new JsonResult(0);
        try {
            if (broadcastService.leave(MessageProto.User.parseFrom(user))) {
                jsonResult.setFlag(1);
            }
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }

        //只需要返回状态码，无实体数据
        return jsonResult;
    }

    //用户进入：增加一个用户
    @PostMapping(value = "/enter")
    public JsonResult enter(@RequestBody byte[] user) {
        JsonResult jsonResult = new JsonResult(0);
        try {
            Map<String, String> roomInfo = broadcastService.enter(MessageProto.User.parseFrom(user));
            if (roomInfo.size() > 0) {
                jsonResult.setFlag(2).setData(roomInfo);
            }
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
        //需要实体数据，包括用户列表和聊天历史记录，使用map<string,string>
        return jsonResult;
    }

    //用户广播聊天
    @PostMapping("/chat")
    public JsonResult chat(@RequestBody Chat chat) {
        JsonResult jsonResult = new JsonResult(0);
        if (broadcastService.chat(chat)) {
            jsonResult.setFlag(1);
        }
        //只需要返回状态码，无实体数据
        return jsonResult;
    }
}
