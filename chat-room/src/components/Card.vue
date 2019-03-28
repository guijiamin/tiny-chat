<template>
  <div class="card">
    <header>
      <img width="40" height="40" :alt="name" :src="img" class="avatar">
      <p class="name">{{name}}</p>
    </header>
    <footer>
      <input type="text" class="search" placeholder="TODO..." @keyup="onSearch">
    </footer>
  </div>
</template>
<script>
export default {
  data() {
    return {
      name: '',
      img: '',
    }
  },
  methods: {
    onSearch(e) {
      // console.log(e.target.value)
    }
  },
  computed: {
    init_timestamp() {
      return this.$store.state.init.init_timestamp
    },
    self_user() {
      return this.$store.state.init.self_user
    }
  },
  watch:  {
    init_timestamp(val) {
      console.log('trigger init_timestamp', val, this.self_user)
      this.name = this.self_user.name
      this.img = require('../assets/' + this.self_user.img)
      // this.img = this.self_user.img
    }
  },
}
</script>
<style scoped lang="less">
.card {
  padding: 10px;
  border-bottom: solid 1px #24272C;

  footer {
    margin-top: 10px;
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
    font-size: 16px;
  }
  .search {
    padding: 0 10px;
    width: 85%;
    font-size: 12px;
    color: #fff;
    height: 30px;
    line-height: 30px;
    border: solid 1px #3a3a3a;
    border-radius: 4px;
    outline: none;
    background-color: #26292E;
  }
}
</style>