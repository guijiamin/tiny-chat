# tiny-chat
![ad](https://github.com/guijiamin/tiny-chat/structure.png)

### 1、chat-room
> web聊天平台

### 2、signal-proxy
> 聊天服务接入层
##### 2.1 websocket与用户建连
##### 2.2 时间轮维护长连接
##### 2.3 java-socket建立tcp-client转发消息到中心（signal-router）（开发中，待完善）

### 3、signal-router（开发中，待完善）
> 聊天服务路由层
##### 3.1 java-socket建立tcp-server
##### 3.2 时间轮维护长连接
##### 3.3 http-client发送消息到持久化业务处理中心（signal-worker）

### 4、signal-worker（开发中，待完善）
> 聊天服务业务处理层
##### 4.1 spring-boot构建http-server
##### 4.2 redis持久化业务数据

### 5、参考
##### 5.1 java-websocket
https://github.com/TooTallNate/Java-WebSocket
##### 5.2 tcp-socket
https://github.com/jzj1993/JavaTcpSocket