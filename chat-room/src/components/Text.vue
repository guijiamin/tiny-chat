<template>
<div class="text">
  <div style="border-top: 1px solid #d6d6d6;padding:4px 10px 0px;cursor: pointer" @click="toogleDialogEmoji"><img width="24" height="24" src="../assets/iconset.png" class="avatar"></div>
  <textarea placeholder="按 Enter 发送" v-model="content" @keyup="onKeyup"></textarea>
  <div :hidden="dialogHidden" style="position:absolute;top:-436px">
    <VEmojiPicker :pack="emojisNative" labelSearch @select="onSelectEmoji"/>
  </div>
</div>
</template>

<script>
import VEmojiPicker from "v-emoji-picker"
import packData from "v-emoji-picker/data/emojis.json"
import {MSG_ID, MSG_TYPE} from '../utils/Constants.js'
export default {
  name: "exampleInputEmoji",
  components: {
    VEmojiPicker
  },
  data () {
    return {
      content: '',
      dialogHidden: true
    };
  },
  methods: {
    onKeyup (e) {
      if (e.keyCode === 13 && this.content.length) {
        // let msg = {
        //   msgtype: '3',
        //   data: this.content
        // }
        // let data = {
        //   rid: this.self_user.rid,
        //   uid: this.self_user.uid,
        //   name: this.self_user.name,
        //   img: this.self_user.img,
        //   msgid: '203',
        //   msg: JSON.stringify(msg)
        // }
        // this.sendMsg(JSON.stringify(data));

        // let message = {
        //   rid: this.self_user.rid,
        //   uid: this.self_user.uid,
        //   name: this.self_user.name,
        //   img: this.self_user.img,
        //   content: this.content,
        //   self: true
        // }
        // this.$store.commit('ws_chat_come', message)
        // this.content = ''
        let msg = new proto.Msg()
        msg.setMsgid(MSG_ID.BROADCAST)
        msg.setMsgtype(MSG_TYPE.CHAT)

        let fuser = new proto.User()
        fuser.setRid(this.self_user.rid)
        fuser.setUid(this.self_user.uid)
        fuser.setName(this.self_user.name)
        fuser.setImg(this.self_user.img)
        msg.setFuser(fuser)
        msg.setTuser(fuser)

        let chat = {
          rid: this.self_user.rid,
          uid: this.self_user.uid,
          name: this.self_user.name,
          img: this.self_user.img,
          content: this.content,
        }
        msg.getExtendMap().set("chat", JSON.stringify(chat))
        this.sendMsg(msg.serializeBinary())
        this.content = ''
      }
    },
    toogleDialogEmoji() {
      this.dialogHidden = !this.dialogHidden
    },
    onSelectEmoji(dataEmoji) {
      this.content += dataEmoji.emoji;
    },
    sendMsg(val) {
      this.WS && this.WS.send(val)
    }
  },
  computed: {
    emojisNative() {
      return packData;
    },
    WS() {
      return this.$store.state.init.WS
    },
    self_user() {
      return this.$store.state.init.self_user
    }
  }
};
</script>

<style lang="less" scoped>
.text {
  height: 160px;

  textarea {
    padding: 0px 10px;
    height: 124px;
    width: 580px;
    border: none;
    outline: none;
    font-family: "Micrsofot Yahei";
    resize: none;
    background-color: #eee;
  }
}
</style>