package com.study.signalworker.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.InvalidProtocolBufferException;
import com.study.signalcommon.protobuf.MessageProto;
import com.study.signalworker.service.ChatService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * Decription
 * <p>
 * </p>
 * DATE 2019/7/17.
 *
 * @author guijiamin.
 */
@RestController
public class ChatController {
    @Resource
    private ChatService chatService;

    @PostMapping("/chat")
    public byte[] chat(@RequestBody byte[] reqMsg) throws InvalidProtocolBufferException {
        return chatService.chat(MessageProto.Msg.parseFrom(reqMsg)).toByteArray();
    }
}
