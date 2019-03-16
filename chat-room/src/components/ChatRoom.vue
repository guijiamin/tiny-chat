<template>
<div class="room-container">
  <!-- <div style="z-index=999" v-if="!isLogin">
    <el-form :model="loginForm" class="login-form" label-position="left" ref="loginForm">
      <h3 class="title">登录</h3>
      <el-form-item prop="roomId" required>
        <el-input type="text" placeholder="请输入房间号" v-model="loginForm.roomId"></el-input>
      </el-form-item>
      <el-form-item prop="userId" required>
        <el-input type="text" placeholder="请输入用户名" v-model="loginForm.userId"></el-input>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" style="width:100%" @click="enter">进入</el-button>
      </el-form-item>
    </el-form>
  </div>
  <div v-else> -->
    <div class="room-board">
      <div>
        房间号：<span>{{currentRid}}</span>
        ，当前用户：<span>{{currentUid}}</span>
        ，当前房间人数：<span>{{roomCount}}</span>
      </div>
    </div>
    <div class="chat-zone">
      <div class="chat-list">
        <template v-for="item in chatList">
            <div class="chat-basic chat-self" v-if="item.source=='self'">{{item.content}}</div>
            <div class="chat-basic chat-oppo" v-else-if="item.source=='oppo'">{{item.content}}</div>
            <div class="clear"></div>
        </template>
      </div>
      <div class="control-region">
        <textarea class="chat-input" placeholder="在这里输入文字，进行聊天" v-on:keyup.enter="sendChat" v-model.trim="message"></textarea>
        <button class="send-btn" v-on:click="sendChat">发送</button>
      </div>
    </div> 
  <!-- </div> -->
</div>
</template>

<script>
export default {
  data() {
    return {
      // isLogin: false,
      // loginForm: {},
      currentRid: '',
      currentUid: '',
      currentAlias: '',
      roomCount: '',
      message: '',
      chatList: [],
      WS: undefined,
      tKeepalive: undefined,
    }
  },
  computed:{
  },
  watch: {
  },
  methods: {
    // enter() {
    //   this.$refs['loginForm'].validate((valid) => {
    //     if(valid) {
    //       this.isLogin = true
    //     }
    //   })
    // },
    createWebsocket() {
      this.WS && this.WS == undefined
      this.WS = new WebSocket("ws://localhost:8787?rid="+ this.currentRid + "&uid=" + this.currentUid + '&alias=' + this.currentAlias)
      this.WS.onopen = this.wsOnopen
      this.WS.onmessage = this.wsOnmessage
      this.WS.onclose = this.wsOnclose
    },
    wsOnopen(evt) {
      console.log("open...")
      let param = {
        msgid: "201",
        frid: this.currentRid,
        fuid: this.currentUid
      }
      this.WS.send(JSON.stringify(param))
    },
    wsOnmessage(evt) {
      console.log("receive message...", evt.data)
      
      let data = JSON.parse(evt.data)
      if (data.msgid == '200') {
        if (data.msg.srcmsgid == '201') {
          this.roomCount = data.msg.data
          // this.tKeepalive = setInterval(() => {
          //   let param = {
          //     msgid: "15",
          //     frid: this.currentRid,
          //     fuid: this.currentUid
          //   }
          //   console.log(this.WS)
          //   this.WS.send(JSON.stringify(param))
          // }, 5000)
        } else if (data.msg.srcmsgid == '15') {
          console.log("receive server heartbeat...")
        }
      } else if (data.msgid == '201') {
      } else if (data.msgid == '202') {
      } else if (data.msgid == '203') {
        if (data.msg.msgtype == '1') {
          this.roomCount = data.msg.data
        } else if (data.msg.msgtype == '2') {
          this.roomCount = data.msg.data
        } else if (data.msg.msgtype == '3') {
          this.chatList.push({
            source: 'oppo',
            content: data.fuid + " says: " + data.msg.data
          })
        }
      }
      
    },
    wsOnclose(evt) {
      console.log("close")
      this.WS == undefined
    },
    sendChat() {
      this.message = this.message.replace(/^\s+|\s+$/g,'')
      if(!this.message.length){
        return
      }
      let msg = this.message

      //发给对方
      let param = {
        frid: this.currentRid,
        fuid: this.currentUid,
        msgid: '203',
        msg: {
          msgtype: '3',
          trid: this.currentRid,
          tuid: this.currentUid,
          data: msg
        }
      }

      this.WS.send(JSON.stringify(param))
      //推送自己
      this.chatList.push({
        source: 'self',
        content: this.currentUid + ' says: ' + msg
      })
      this.message = ''
    },
    getQuery(url, name) {
      var reg = new RegExp('[?&]' + name + '=([^#&\\?]+)');
      if (!reg.test(url)) {
        return false;
      }
      return url.match(reg)[1];
    }
  },
  mounted() {
    console.log('init')
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
    let alias = this.getQuery(url, 'alias')
    console.log("1234")
    this.currentRid = rid
    this.currentUid = uid
    this.currentAlias = alias
    this.createWebsocket()
  }
}
</script>

<style scoped>
.room-container {
  width: 100%;
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  text-align: center;
  /*background-color: #141a48;*/
  background-image: url('../assets/login-bg.png');
  background-repeat: no-repeat;
  background-size: cover;
  overflow: hidden;
}
.login-form {
  -webkit-border-radius: 5px;
  border-radius: 5px;
  -moz-border-radius: 5px;
  background-clip: padding-box;
  margin: 150px auto;
  width: 300px;
  padding: 35px 35px 15px 35px;
  background: #fff;
  border: 1px solid #eaeaea; 
}
.room-board {
  width: 100%;
  height: 40px;
  color:#fff;
  margin: 30px auto 5px;
}
.chat-zone {
  width: 600px;
  height: 600px;
  /*float: left;*/
  background-color: #a3c3da;
  background-image: url('../assets/dialog@1x.png');
  background-repeat: no-repeat;
  background-position: center center;
  margin: 10px auto;
  /*position: relative;*/
  text-align: left;
}
.chat-list {
  height: 510px;
  overflow-y: auto;
  overflow-x: hidden;
  margin-top: 6px;
  margin-left: 9px;
  margin-right: 14px;
  padding-top: 10px;
}
.name{
  /*clear:both;*/
  width:100%;
  height:20px;
  font-size: 14px;
  text-align: right;
  color: #808080;
  /*visibility: hidden;*/
  /*font-size:0;*/
  /*line-height: 0;*/
}
.chat-basic {
  position: relative;
  border-radius: 4px;
  font-size: 13px;
  line-height: 20px;
  margin-bottom: 10px;
  padding: 6px 14px;
  max-width: 100px;
  display: inline-block;
  word-wrap: break-word;
}
.chat-self {
  background: #96ed60;
  float:right;
  margin-right: 4px;
}
.chat-self:before {
  content:'';
  position:absolute;
  width:0;
  height:0;
  font-size: 0;
  line-height: 0;
  border:solid rgba(255,255,255,0);
  border-left-color: #96ed60;
  border-width:5px 4px;
  right:-8px;
  top:50%;
  margin-top:-5px;
}
.chat-self:after {
  content:'';
  width:100%;
  height:1px;
  font-size:0;
  line-height: 0;
  visibility: hidden;
  clear: both;
}
.chat-oppo {
  background: #fff;
  float:left;
  margin-left: 8px;
}
.chat-oppo:before {
    content:'';
    position:absolute;
    width:0;
    height:0;
    font-size: 0;
    line-height: 0;
    border:solid rgba(255,255,255,0);
    border-right-color: #fff;
    border-width:5px 4px;
    left:-8px;
    top:50%;
    margin-top:-5px;
}
.chat-oppo:after {
    content:'';
    width:100%;
    height:1px;
    font-size:0;
    line-height: 0;
    visibility: hidden;
    clear: both;
}
.clear{
  clear:both;
  width:100%;
  height:1px;
  visibility: hidden;
  font-size:0;
  line-height: 0;
}
.control-region {
  padding-left:6px;
  box-sizing:border-box;
}
.chat-input {
  color: #8c8c8c;
  font-size: 14px;
  line-height: 1.5;
  background-color: #eef4fb;
  border: 1px solid #a7b2ba;
  border-radius: 4px;
  width: 520px;
  height: 70px;
  padding: 10px 8px;
  box-sizing:border-box;
  float: left;
  overflow: hidden;
  resize: none;
}
.send-btn {
  float: left;
  margin-left: 6px;
  width: 60px;
  height: 70px;
  background-color: #eef4fb;
  border-radius: 4px;
  border: 0;
  color: #8c8c8c;
  font-size: 14px;
}
</style>