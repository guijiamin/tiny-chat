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
      // ADDR: 'echo.websocket.org',
      ADDR: 'localhost:8787',
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
      console.log('收到消息', evt)
      let self = this
      let win = window
      let reader = new FileReader()
      reader.readAsArrayBuffer(evt.data)
      reader.onload = function (e) {
        let buf = new Uint8Array(reader.result)
        let msg = proto.Msg.deserializeBinary(buf)
        console.log('转换二进制', msg.toObject())
        win.m = msg
        console.log(typeof(msg))
        if (msg.getMsgid() == MSG_ID.REPLY) {
          console.log('receive reply')
          self.$store.commit('ws_enter_room', JSON.parse(msg.getExtendMap().get('data')))
          // this.keepAlive()
        } else if (msg.getMsgid() == MSG_ID.BROADCAST) {
          let msgtype = msg.getMsgtype()
          if (msgtype == MSG_TYPE.ENTER) {//someone enter
            console.log('someone enter')
            // console.log(data.msg.data)
            //增加在线列表
            self.$store.commit('ws_one_enter', JSON.parse(msg.getExtendMap().get('data')))
          } else if (msgtype == MSG_TYPE.LEAVE) {//someone leave
            console.log('someone leave')
            // console.log(data.msg.data)
            //删除在线列表
            // self.$store.commit('ws_one_leave', data.msg.data)
          } else if (msgtype == MSG_TYPE.CHAT) {//someone chat
            console.log('someone chat')
            // console.log(data.msg)
            // let message = {
            //   rid: data.rid,
            //   uid: data.uid,
            //   name: data.name,
            //   img: data.img,
            //   content: data.msg.data
            // }
            // this.$store.commit('ws_chat_come', message)
          }
        }
      }
    },
    wsOnclose(evt) {
      console.log('关闭连接', evt)
    },
    sendMsg(val) {
      this.WS.send(val)
    },
    keepAlive() {
      // let msg = {
      //   "msgid": "15",
      //   "rid": this.self_user.rid,
      //   "uid": this.self_user.uid,
      //   "name": this.self_user.name,
      //   "img": this.self_user.img
      // }
      // setInterval(() => {this.sendMsg(JSON.stringify(msg))}, 5000)
      // let m = new proto.Msg();
      // m.setMsgid(101);
      // m.setMsgtype(0);
      // // m.getExtendMap().set("1","2");
      // let fuser = new proto.User();
      // fuser.setRid("heartbeat");
      // fuser.setUid("heartbeat");
      // fuser.setName("heartbeat");
      // fuser.setImg("heartbeat");
      // m.setFuser(fuser);
      // m.setTuser(fuser);
      // console.log(m.toObject());
      // //序列化
      // let bytes = m.serializeBinary();
      // // console.log(bytes);
      // setInterval(() => {this.sendMsg(bytes)}, 5000)
      let m1 = new proto.Msg();
      m1.setMsgid(101);
      m1.setMsgtype(1);
      // m.getExtendMap().set("1","2");
      let fuser1 = new proto.User();
      fuser1.setRid("jz123");
      fuser1.setUid("123");
      fuser1.setName("gjm");
      fuser1.setImg("avatar1.svg");
      m1.setFuser(fuser1);
      m1.setTuser(fuser1);
      console.log(m1.toObject());
      //序列化
      let bytes1 = m1.serializeBinary();
      console.log(bytes1)
      setTimeout(() => this.sendMsg(bytes1),1000)
    },
    parseBlob(val) {
      let reader = new FileReader()
      reader.readAsArrayBuffer(val)
      reader.onload = function (e) {
        let buf = new Uint8Array(reader.result)
        let m = proto.Msg.deserializeBinary(buf).toObject()
        console.log('转换二进制', m)
        return m
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
      console.log('开始创建ws连接:' + val)
      //获取到参数，创建ws连接
      this.createWebsocket()
    },
    ws_open_timestamp(val) {
      console.log('ws连接已打开：' + val)
      //发送进教室消息
      let msg = new proto.Msg()
      msg.setMsgid(MSG_ID.ENTERROOM)
      msg.setMsgtype(MSG_TYPE.ENTER)
      let fuser = new proto.User()
      fuser.setRid(this.self_user.rid)
      fuser.setUid(this.self_user.uid)
      fuser.setName(this.self_user.name)
      fuser.setImg(this.self_user.img)
      msg.setFuser(fuser)
      msg.setTuser(fuser)
      console.log("发送进教室消息", msg.toObject())
      //序列化
      let bytes = msg.serializeBinary()
      this.sendMsg(bytes)
    }
  },
  mounted() {
    console.log('init chatroom')
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