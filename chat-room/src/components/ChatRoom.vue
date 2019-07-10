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
import {CustomMap} from '../utils/Map.js'
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
      console.log('收到消息', evt.data)
  
      let data = JSON.parse(evt.data)
      if (data.msgid == '200') {
        // console.log(data.msg.data)
        if (data.msg.srcmsgid == '201') {
          this.$store.commit('ws_enter_room', JSON.parse(data.msg.data))
        }
        this.keepAlive()
      } else if (data.msgid == '203') {
        let msgtype = data.msg.msgtype
        if (msgtype == '1') {//someone enter
          // console.log(data.msg.data)
          //增加在线列表
          this.$store.commit('ws_one_enter', data.msg.data)
        } else if (msgtype == '2') {//someone leave
          // console.log(data.msg.data)
          //删除在线列表
          this.$store.commit('ws_one_leave', data.msg.data)
        } else if (msgtype == '3') {//someone msg
          // console.log(data.msg)
          let message = {
            rid: data.rid,
            uid: data.uid,
            name: data.name,
            img: data.img,
            content: data.msg.data
          }
          this.$store.commit('ws_chat_come', message)
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
      let msg = {
        "msgid": "15",
        "rid": this.self_user.rid,
        "uid": this.self_user.uid,
        "name": this.self_user.name,
        "img": this.self_user.img
      }
      setInterval(() => {this.sendMsg(JSON.stringify(msg))}, 5000)
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
      console.log('trigger init_timestamp:' + val)
      //获取到参数，创建ws连接
      this.createWebsocket()
    },
    ws_open_timestamp(val) {
      console.log('trigger ws_open_timestamp', val)
      //发送201进教室
      let msg = {
        "msgid": "201",
        "rid": this.self_user.rid,
        "uid": this.self_user.uid,
        "name": this.self_user.name,
        "img": this.self_user.img
      }
      this.sendMsg(JSON.stringify(msg))
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