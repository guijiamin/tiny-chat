<template><div id="chatroom">
  <div class="sidebar">
    <card></card>
    <list></list>
  </div>
  <div class="main">
    <message></message>
    <textbox></textbox>
  </div>
</div></template>

<script>
import Card from './Card.vue';
import List from './List.vue';
import Message from './Message.vue';
import Text from './Text.vue';
import {CustomMap} from '../utils/CustomMap.js';
import {MSG_ID, MSG_TYPE} from '../utils/Constants.js';
import message from '../proto/message.js';
export default {
  data() {
    return {
      WS: undefined,
      ADDR: 'localhost:8787',
      lastReadTs: 0,
      HEARTBEAT_INTERVAL: 3000,
    }
  },
  components: { 
    'card': Card, 
    'list': List,
    'message': Message,
    'textbox': Text
  },
  methods: {
    getQuery(url, name) {
      let reg = new RegExp('[?&]' + name + '=([^#&\\?]+)');
      if (!reg.test(url)) {
        return false;
      }
      return url.match(reg)[1];
    },
    createWebsocket() {
      this.WS && this.WS == undefined
      this.WS = new WebSocket('ws://' + this.ADDR + '?rid=' + this.self_user.rid + '&uid=' + this.self_user.uid + '&name=' + this.self_user.name + '&img=' + this.self_user.img)
      this.WS.onopen = this.wsOnopen
      this.WS.onmessage = this.wsOnmessage
      this.WS.onclose = this.wsOnclose
    },
    wsOnopen(evt) {
      console.log('打开连接', evt)
      this.$store.commit('ws_open', this.WS)
    },
    wsOnmessage(evt) {
      this.lastReadTs = new Date().getTime()
      let self = this
      let reader = new FileReader()
      reader.readAsArrayBuffer(evt.data)
      reader.onload = function (e) {
        let buf = new Uint8Array(reader.result)
        let msg = proto.Msg.deserializeBinary(buf)
        console.log('收到消息', msg.toObject())

        switch (msg.getMsgid()) {
          case MSG_ID.REPLY:
            console.log('receive reply')
            let msgtype = msg.getMsgtype()
            switch (msg.getSrcmsgid()) {
              case MSG_ID.UNICAST:
                break;
              case MSG_ID.BROADCAST:
                switch (msgtype) {
                  case MSG_TYPE.ENTER://收到自己发送的进教室广播消息的回应消息
                    self.$store.commit('ws_enter_room', JSON.parse(msg.getExtendMap().get('users')))//提取用户列表
                    break;
                  case MSG_TYPE.CHAT://收到自己发送的聊天广播消息的回应消息
                    break;
                  default:
                    break;
                }
                break;
              default:
                break;
            }
            break;
          case MSG_ID.KEEPALIVE:
            console.log('receive heartbeat')
            break;
          case MSG_ID.UNICAST:
            console.log('receive unicast')
            break;
          case MSG_ID.BROADCAST:
            console.log('receive broadcast')
            switch (msg.getMsgtype()) {
              case MSG_TYPE.LEAVE://收到有人离开教室的广播消息
                console.log('someone leave')
                self.$store.commit('ws_one_leave', JSON.parse(msg.getExtendMap().get('data')))
                break;
              case MSG_TYPE.ENTER://收到有人进教室的广播消息
                console.log('someone enter')
                self.$store.commit('ws_one_enter', JSON.parse(msg.getExtendMap().get('data')))
                break;
              case MSG_TYPE.CHAT://收到有人发聊天的广播消息
                console.log('someone chat')
                self.$store.commit('ws_one_chat', JSON.parse(msg.getExtendMap().get('data')))
                break;
              default:
                break;
            }
            break;
          default:
            break;
        }
      }
    },
    wsOnclose(evt) {
      console.log('关闭连接', evt)
      //TODO 重连
    },
    sendMsg(val) {
      this.WS.send(val)
    },
    keepAlive() {
      if (this.lastReadTs > 0 && (new Date().getTime() - this.lastReadTs > this.HEARTBEAT_INTERVAL)) {
        let msg = new proto.Msg()
        msg.setMsgid(MSG_ID.KEEPALIVE)
        let fuser = new proto.User()
        fuser.setRid(this.self_user.rid)
        fuser.setUid(this.self_user.uid)
        fuser.setName(this.self_user.name)
        msg.setFuser(fuser)
        //序列化
        let bytes = msg.serializeBinary();
        this.sendMsg(bytes)
      }
    }
  },
  computed: {
    init_timestamp() {
      return this.$store.state.init.init_timestamp
    },
    self_user() {
      return this.$store.state.init.self_user
    },
    ws_open_timestamp() {
      return this.$store.state.init.ws_timestamp
    }
  },
  watch:  {
    init_timestamp(val) {
      console.log('开始创建ws连接', val)
      //获取到参数，创建ws连接
      this.createWebsocket()
    },
    ws_open_timestamp(val) {
      console.log('ws连接已打开', val)
      //开启定时任务
      setInterval(() => this.keepAlive(), 1000)
      //发送进教室广播消息103/1
      let msg = new proto.Msg()
      msg.setMsgid(MSG_ID.BROADCAST)
      msg.setMsgtype(MSG_TYPE.ENTER)

      let fuser = new proto.User()
      fuser.setRid(this.self_user.rid)
      fuser.setUid(this.self_user.uid)
      fuser.setName(this.self_user.name)
      fuser.setImg(this.self_user.img)
      msg.setFuser(fuser)
      msg.setTuser(fuser)
      //序列化发送
      this.sendMsg(msg.serializeBinary())
    }
  },
  mounted() {
    console.log('初始化房间...')
    let url = location.href
    let rid = this.getQuery(url, 'rid')
    if (!rid) {
      alert("房间号不能为空！")
      return
    }
    let uid = this.getQuery(url, 'uid')
    if (!uid) {
      alert("用户不能为空！")
      return
    }
    let name = this.getQuery(url, 'name')
    if (!name) {
      name = 'unkown'
    }
    let img = this.getQuery(url, 'img')
    if (!img) {
      img = 'avatar1.svg'
    }
    let user = {
      rid: rid,
      uid: uid,
      name: name,
      img: img
    }
    this.$store.commit('init', user)
  }
}
</script>

<style lang="less" scoped>
#chatroom {
  margin: 20px auto;
  width: 800px;
  height: 600px;

  overflow: hidden;
  border-radius: 3px;

  .sidebar, .main {
    height: 100%;
  }
  .sidebar {
    float: left;
    width: 200px;
    color: #f4f4f4;
    background-color: #2e3238;
  }
  .main {
    position: relative;
    overflow: hidden;
    background-color: #eee;
  }
  .text {
    position: absolute;
    width: 100%;
    bottom: 0;
    left: 0;
  }
  .message {
    height: ~'calc(100% - 180px)';
  }
}
</style>