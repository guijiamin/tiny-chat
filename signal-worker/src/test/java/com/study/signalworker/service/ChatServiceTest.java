package com.study.signalworker.service;

import com.study.signalcommon.component.PacketTransceiver;
import com.study.signalcommon.constant.GlobalConstants;
import com.study.signalcommon.protobuf.MessageProto;
import com.study.signalworker.SignalWorkerApplication;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import javax.annotation.Resource;

/**
 * Decription
 * <p>
 * </p>
 * DATE 2019/7/28.
 *
 * @author guijiamin.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SignalWorkerApplication.class)
@WebAppConfiguration
public class ChatServiceTest {
    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Resource
    private BroadcastService chatService;

    @Test
    public void chat() throws Exception {

        chatService.chat(MessageProto.Msg.newBuilder()
                .setMsgid(101)
                .setMsgtype(1)
                .setTuser(GlobalConstants.USER.HEARTBEAT)
                .setFuser(GlobalConstants.USER.HEARTBEAT)
                .build());
        chatService.chat(MessageProto.Msg.newBuilder()
                .setMsgid(101)
                .setMsgtype(1)
                .setTuser(PacketTransceiver.generateUser("rid_heartbeat", "456", "789", "000"))
                .setFuser(PacketTransceiver.generateUser("rid_heartbeat", "456", "789", "000"))
                .build());
    }

}