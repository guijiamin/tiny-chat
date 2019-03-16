package com.study.signalproxy;

import com.study.signalproxy.service.ProxyServer;
import com.study.signalproxy.util.SpringUtil;
import org.java_websocket.server.WebSocketServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;
import java.net.InetSocketAddress;

@SpringBootApplication
public class SignalProxyApplication {

	public static void main(String[] args) {
		SpringApplication.run(SignalProxyApplication.class, args);
		ProxyServer proxyServer = new ProxyServer(new InetSocketAddress(8787));

		proxyServer.setRedisTemplate((RedisTemplate) SpringUtil.getBean("rd"));
		proxyServer.run();
	}

}
