import Vue from 'vue'
import Router from 'vue-router'
import Login from '@/components/Login'
import ChatRoom from '@/components/ChatRoom'
import Card from '@/components/Card'

Vue.use(Router)

export default new Router({
  routes: [
    {
      path: '/',
      redirect: '/chatroom'
    },{
      path: '/chatroom',
      name: 'ChatRoom',
      component: ChatRoom
    }
  ]
})
