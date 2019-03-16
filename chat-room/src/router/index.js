import Vue from 'vue'
import Router from 'vue-router'
import Login from '@/components/Login'
import ChatRoom from '@/components/ChatRoom'

Vue.use(Router)

export default new Router({
  routes: [
    {
      path: '/',
      redirect: '/chatroom'
    },{
    //   path: '/login',
    //   name: 'Login',
    //   component: Login
    // },{
      path: '/chatroom',
      name: 'ChatRoom',
      component: ChatRoom
    }
  ]
})
