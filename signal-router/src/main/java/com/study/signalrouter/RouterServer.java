package com.study.signalrouter;

import com.study.signalcommon.constant.GlobalConstants;
import com.study.signalrouter.service.TcpServer;

import java.io.*;

/**
 * Decription
 * <p>
 * </p>
 * DATE 2019/3/18.
 *
 * @author guijiamin.
 */
public class RouterServer {

    public static void main (String[] args) throws FileNotFoundException,IOException {
        //创建serversocket服务监听
        new TcpServer(GlobalConstants.SERVER_PORT.ROUTER).start();
    }
}
