<template>
  <div class="login-container">
    <el-form :model="loginForm" class="login-form" label-position="left" ref="loginForm">
      <h3 class="title">登录</h3>
      <el-form-item prop="roomId">
        <el-input type="text" placeholder="请输入房间号" v-model="loginForm.roomId"></el-input>
      </el-form-item>
      <el-form-item prop="userId">
        <el-input type="text" placeholder="请输入用户名" v-model="loginForm.userId"></el-input>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" style="width:100%" @click="enter">进入</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import md5 from 'js-md5';
export default {
  data() {
    return {
      loginForm: {},
    }
  },
  methods: {
    enter() {
      this.$refs['loginForm'].validate((valid) => {
        if(valid) {
          this.$store.commit('login', this.loginForm)
          this.$router.push({ path: '/chatroom'})
        }
      })
    }
  },
}
</script>

<style scoped>
.login-container {
  width: 100%;
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  text-align: center;
  background-color: #141a48;
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
</style>