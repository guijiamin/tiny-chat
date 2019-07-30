package com.study.signalrouter;

import com.study.signalcommon.constant.GlobalConstants;
import com.study.signalrouter.service.socket.TcpServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SignalRouterApplication {

	public static void main(String[] args) {
		SpringApplication.run(SignalRouterApplication.class, args);
		new TcpServer(GlobalConstants.SERVER_PORT.ROUTER).start();
	}

}
