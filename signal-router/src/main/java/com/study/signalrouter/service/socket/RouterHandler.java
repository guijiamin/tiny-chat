package com.study.signalrouter.service.socket;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Decription
 * <p>
 * </p>
 * DATE 2019/3/18.
 *
 * @author guijiamin.
 */
@Data
public class RouterHandler implements Runnable {
    private InputStream is;

    private OutputStream os;

    public RouterHandler() {
    }

    public RouterHandler(InputStream is, OutputStream os) {
        this.is = is;
        this.os = os;
    }

    public void run() {
        //读取请求串
        String request = "";
        byte[] buffer = new byte[1024];
        int length = 0;
        try {
            if ((length = is.read(buffer)) > 0) {
                request = new String(buffer, 0, length);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //解析请求串
//        JSONObject jsonObject = JSONObject.parseObject(request);
        System.out.println(request);
        //TODO http发送给worker，等待回应，超时重试，3次以上则回复给proxy异常

        //TODO 回复
        try {
            os.write("reply".getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
