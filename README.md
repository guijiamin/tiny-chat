# tiny-chat

### 1、chat-room
> web聊天平台

### 2、signal-proxy
> 聊天服务接入层
##### 2.1 websocket与用户建连
> java-websocket库：https://github.com/TooTallNate/Java-WebSocket
##### 2.2 时间轮维护长连接
##### 2.3 java-socket转发消息到中心（signal-router）

### 3、signal-router
> 聊天服务路由层
##### 3.1 java-socket建立tcp-server
##### 3.2 时间轮维护长连接

### 4、signal-worker
> 聊天服务业务处理层
##### 4.1 spring-boot构建http-server
##### 4.2 redis持久化业务数据