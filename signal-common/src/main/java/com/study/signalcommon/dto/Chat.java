package com.study.signalcommon.dto;

import com.study.signalcommon.protobuf.MessageProto;
import lombok.Data;

/**
 * Decription
 * <p>
 * </p>
 * DATE 2019/8/1.
 *
 * @author guijiamin.
 */
@Data
public class Chat {
    String rid;
    String uid;
    String name;
    String img;
    String content;

    public Chat() {

    }

    public Chat(MessageProto.User user, String content) {
        this.rid = user.getRid();
        this.uid = user.getUid();
        this.name = user.getName();
        this.img = user.getImg();
        this.content = content;
    }
}
