package com.study.signalproxy;

import com.study.signalproxy.service.ProxyServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.Resource;
import java.net.InetSocketAddress;

@SpringBootApplication
public class SignalProxyApplication {

	public static void main(String[] args) {
		SpringApplication.run(SignalProxyApplication.class, args);
		ProxyServer proxyServer = new ProxyServer(8787);
		proxyServer.run();
	}

}
