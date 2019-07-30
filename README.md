# tiny-chat
![ad](https://github.com/guijiamin/tiny-chat/blob/master/sys.png)

### 1、chat-room
> 网页聊天室平台
##### 1.1 如何与服务端（signal-proxy）建连
选择当前最优一个服务器（暂时未接入），采用websocket与服务端建立连接，收发消息
##### 1.2 如何与服务端（signal-proxy）维持长连接
定时任务发送15号消息给服务端，能持续收到服务端回应说明长连接可用，若某种阈值内（例如连续3次）没有收到任何回应，说明长连接不可用，需要重新建连
##### 1.3 重连策略
a）首次连接超时重连
b）保活失败重连

### 2、signal-proxy
> 边缘接入层
##### 2.1 如何与客户端交互
监听websocket连接，通过该连接收发消息
##### 2.2 如何管理客户端长连接
使用时间轮管理客户端连接，当收到客户端报活消息，则激活该连接，一个时间轮回时间内未得到激活的连接，服务端将主动断掉连接
##### 2.3 如何与中心服务交互
a）与中心路由层（signal-router）建立tcp长连接（心跳保活，断连重连）
b）通过生产-消费事件模式，与中心路由层（signal-router）交互数据

### 3、signal-router
> 中心路由层：收取和转发边缘接入层消息
##### 3.1 如何与边缘接入层（signal-proxy）交互
开启tcp服务，接收边缘接入层（signal-proxy）tcp连接，通过该连接收发消息
##### 3.2 如何管理边缘接入层（signal-proxy）长连接
使用时间轮管理边缘接入层（signal-proxy）连接，维护方式同2.2
##### 3.3 如何与中心业务处理层交互
通过http-client发送消息到中心业务处理层（signal-worker）

### 4、signal-worker
> 中心业务处理层：持久化处理
##### 4.1 如何接收需要持久化消息
采用spring-boot构建一个http-server收发消息
##### 4.2 如何持久化数据
使用redis持久化数据

### 5、signal-common
> 聊天服务公共模块
##### 5.1 定义消息协议（protobuf）
##### 5.2 维护公用常量和方法

### 6、参考
##### 6.1 java-websocket
https://github.com/TooTallNate/Java-WebSocket
##### 6.2 tcp-socket
https://github.com/jzj1993/JavaTcpSocket
##### 6.3 emoji
https://github.com/joaoeudes7/V-Emoji-Picker
##### 6.4 vue-chat
https://github.com/coffcer/vue-chat