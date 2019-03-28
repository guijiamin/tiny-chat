<template><div>
  <div style="text-align: center;border-bottom: 1px solid #d6d6d6;padding:15px 0px 5px;font-size: 15px">
    <marquee scrollamount="3">
      <span>当前房间：{{current_rid}}</span>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
      <span>在线人数：{{current_size}}</span>
    </marquee>
  </div>
  <div class="message" id="message">
    <ul v-if="message_list" style="margin:0px;padding:0px">
      <li v-for="item in message_list">
        <p class="time" v-if="item.ts">
          <span>{{item.ts}}</span>
        </p>
        <div class="main" :class="{ self: item.self }">
          <img class="avatar" width="30" height="30" :src="item.img"/>
          <div style="font-size: 14px;color: #aaa" v-text="item.name"></div>
          <div class="text">{{ item.content }}</div>
        </div>
      </li>
    </ul>
  </div>
</div></template>
<script>
export default {
  data() {
    return {
      message_list: []
    }
  },
  computed: {
    current_rid() {
      return this.$store.state.init.current_rid
    },
    current_size() {
      return this.$store.state.init.online_users.size()
    },
    messages() {
      return this.$store.state.init.messages
    },
    self_user() {
      return this.$store.state.init.self_user
    }
  },
  watch: {
    messages() {
      // console.log(this.messages)
      this.message_list = this.messages.map(val => {
          let msg = JSON.parse(JSON.stringify(val))
          if (this.self_user.uid == val.uid) {
            msg.self = true
          }

          msg.img = require('../assets/' + val.img)
          return msg
      })
      this.$nextTick(() => {
        let container = this.$el.querySelector('#message')
        container.scrollTop = container.scrollHeight
      })
      console.log(this.message_list)
    }
  },
  methods: {

  },
}
</script>
<style lang="less" scoped>
.message {
  height: 375px;
  padding: 10px 15px;
  overflow-y: scroll;
  li {
    margin-bottom: 15px;
    list-style-type: none;
  }
  .time {
    margin: 7px 0;
    text-align: center;

    > span {
      display: inline-block;
      padding: 0 18px;
      font-size: 12px;
      color: #fff;
      border-radius: 2px;
      background-color: #dcdcdc;
    }
  }
  .avatar {
    float: left;
    margin: 0 10px 0 0;
    border-radius: 3px;
  }
  .text {
    display: inline-block;
    position: relative;
    padding: 0 10px;
    max-width: ~'calc(100% - 40px)';
    min-height: 30px;
    line-height: 2.5;
    font-size: 12px;
    text-align: left;
    word-break: break-all;
    background-color: #fafafa;
    border-radius: 4px;

    &:before {
      content: " ";
      position: absolute;
      top: 9px;
      right: 100%;
      border: 6px solid transparent;
      border-right-color: #fafafa;
    }
  }

  .self {
    text-align: right;

    .avatar {
      float: right;
      margin: 0 0 0 10px;
    }
    .text {
      background-color: #b2e281;
      color: #4f4f4f;
      &:before {
        right: inherit;
        left: 100%;
        border-right-color: transparent;
        border-left-color: #b2e281;
      }
    }
  }
}
</style>