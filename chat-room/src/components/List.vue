<template>
  <div class="list">
    <ul style="margin:0px;padding:0px">
      <li v-for="item in user_list">
        <img class="avatar"  width="30" height="30" :alt="item.name" :src="item.img">
        <p class="name">{{item.name}}</p>
      </li>
    </ul>
  </div>
</template>

<script>
export default {
  data() {
    return {
      user_list: [],
    }
  },
  methods: {

  },
  computed: {
    online_users() {
      return this.$store.state.init.online_users.values
    }
  },
  watch: {
    online_users() {
      this.user_list = this.online_users.map(val => {
          let user = {}
          user.name = val.name
          user.img = require('../assets/' + val.img)
          return user
      })
    }
  }
}  
</script>

<style scoped lang="less">
.list {
  li {
    padding: 12px 15px;
    border-bottom: 1px solid #292C33;
    cursor: pointer;
    transition: background-color .1s;
    list-style-type: none;
    font-size: 14px;
    margin: 0px;

    &:hover {
      background-color: rgba(255, 255, 255, 0.03);
    }
    &.active {
      background-color: rgba(255, 255, 255, 0.1);
    }
  }
  .avatar, .name {
    vertical-align: middle;
  }
  .avatar {
    border-radius: 2px;
  }
  .name {
    display: inline-block;
    margin: 0 0 0 15px;
  }
}  
</style>