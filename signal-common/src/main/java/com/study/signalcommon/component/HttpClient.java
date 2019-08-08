package com.study.signalcommon.component;

import com.alibaba.fastjson.JSONObject;
import com.study.signalcommon.dto.JsonResult;
import com.study.signalcommon.protobuf.MessageProto;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.*;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.SerializableEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.Serializable;
import java.net.UnknownHostException;

/**
 * Decription
 * <p>
 * </p>
 * DATE 2019/8/3.
 *
 * @author guijiamin.
 */
@Slf4j
public class HttpClient {
//    private final static int timeOut = 10 * 1000;
//    private static CloseableHttpClient httpClient = null;
//    private final static Object syncLock = new Object();
//
//    private static void config(HttpRequestBase httpRequestBase) {
//        RequestConfig requestConfig = RequestConfig.custom()
//                .setConnectionRequestTimeout(timeOut)
//                .setConnectTimeout(timeOut)
//                .setSocketTimeout(timeOut)
//                .build();
//        httpRequestBase.setConfig(requestConfig);
//    }
//
//    private static CloseableHttpClient createHttpClient(
//            int maxTotal,
//            int maxPerRoute,
//            int maxRoute,
//            String addr,
//            int port
//    ) {
//        ConnectionSocketFactory factory = PlainConnectionSocketFactory.getSocketFactory();
//        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create().register("http", factory).build();
//        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
//        cm.setMaxTotal(maxTotal);//将最大连接数增加
//        cm.setDefaultMaxPerRoute(maxPerRoute);//将每个路由基础的连接增加
//        HttpHost httpHost = new HttpHost(addr, port);
//        cm.setMaxPerRoute(new HttpRoute(httpHost), maxRoute);//将目标主机的最大连接数增加
//        //请求重试处理
//        HttpRequestRetryHandler httpRequestRetryHandler = new HttpRequestRetryHandler() {
//            @Override
//            public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
//                if (executionCount >= 5) {//如果重试超过5次就放弃
//                    return false;
//                }
//                if (exception instanceof NoHttpResponseException) {
//                    return true;//如果服务器丢掉了连接就重试
//                }
//                if (exception instanceof InterruptedIOException) {
//                    return false;//如果超时就放弃
//                }
//                if (exception instanceof UnknownHostException) {
//                    return false;//如果目标服务器不可达就放弃
//                }
//                if (exception instanceof ConnectTimeoutException) {
//                    return false;//如果连接被拒绝就放弃
//                }
//                if (exception instanceof ConnectionPoolTimeoutException) {
//                    return true;//如果从连接池中获取连接超时（即连接池中已满）就重试
//                }
//
//                HttpClientContext clientContext = HttpClientContext.adapt(context);
//                HttpRequest request = clientContext.getRequest();
//                //如果请求是幂等的就再次尝试
//                if (!(request instanceof HttpEntityEnclosingRequest)) {
//                    return true;
//                }
//                return false;
//            }
//        };
//
//        return HttpClients.custom()
//                .setConnectionManager(cm)
//                .setRetryHandler(httpRequestRetryHandler)
//                .build();
//    }
//
//    public static CloseableHttpClient getHttpClient(String addr, int port) {
//        if (httpClient == null) {
//            synchronized (syncLock) {
//                if (httpClient == null) {
//                    httpClient = createHttpClient(200, 40, 100, addr, port);
//                }
//            }
//        }
//        return httpClient;
//    }
//    public static String post(String url, int port, Map<String, Object> params) {
//        HttpPost httpPost = new HttpPost(url);
//        config(httpPost);
//        //TODO setPostParam
//        CloseableHttpResponse response = null;
//        try {
//            response = getHttpClient(url, port).execute(httpPost, HttpClientContext.create());
//            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
//                HttpEntity entity = response.getEntity();
//                String result = EntityUtils.toString(entity, "utf-8");
////                JsonResult jsonResult = JSONObject.parseObject(result, JsonResult.class);
////                if (jsonResult.getFlag() == 1) {
////
////                } else if (jsonResult.getFlag() == 2) {
////                    jsonResult.getData()
////                }
//                EntityUtils.consume(entity);//
//                return result;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                if (response != null) {
//                    response.close();
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        return null;
//    }

    private final PoolingHttpClientConnectionManager poolConnManager;
    private final HttpRequestRetryHandler httpRequestRetryHandler;
    private final CloseableHttpClient httpClient;

    public HttpClient() {
        this.poolConnManager = new PoolingHttpClientConnectionManager();
        this.poolConnManager.setMaxTotal(1000);
        this.poolConnManager.setDefaultMaxPerRoute(100);

        this.httpRequestRetryHandler = (IOException exception, int executionCount, HttpContext context) -> {
            if (executionCount >= 5) {
                return false;
            }
            if (exception instanceof NoHttpResponseException) {
                return true;
            }
            if (exception instanceof InterruptedIOException) {
                return false;
            }
            if (exception instanceof UnknownHostException) {
                return false;
            }
            if (exception instanceof ConnectTimeoutException) {
                return false;
            }
            HttpClientContext clientContext = HttpClientContext.adapt(context);
            HttpRequest request = clientContext.getRequest();

            if (!(request instanceof HttpEntityEnclosingRequest)) {
                return true;
            }
            return false;
        };

        this.httpClient = HttpClients.custom()
                .setConnectionManager(this.poolConnManager)
                .setRetryHandler(this.httpRequestRetryHandler)
                .build();
    }

    public JsonResult doPost(String url, byte[] req) {
        JsonResult jsonResult = null;
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(new ByteArrayEntity(req));
        httpPost.addHeader("Content-Type", "application/json; charset=utf-8");
        CloseableHttpResponse response = null;
        try {
            response = this.httpClient.execute(httpPost);
            log.info("response: {}", response);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                //TODO
                jsonResult = JSONObject.parseObject(EntityUtils.toString(entity), JsonResult.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (response != null) {
                try {
                    EntityUtils.consume(response.getEntity());
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return jsonResult;
    }

    public static void main(String[] args) {
        HttpClient httpClient = new HttpClient();
        MessageProto.User.Builder user = MessageProto.User.newBuilder();
        user.setRid("123");
        user.setUid("455");
        user.setName("hh");
        user.setImg("avatar1.svg");

        httpClient.doPost("http://localhost:8989/broadcast/enter", user.build().toByteArray());
    }
}
