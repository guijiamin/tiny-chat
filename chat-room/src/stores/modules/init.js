import {CustomMap} from '../../utils/CustomMap.js'

const state = {
  WS: undefined,
  self_user: {},
  init_timestamp: 0,
  ws_timestamp: 0,
  online_users: new CustomMap(),
  current_rid: '',
  chats: []
}

const mutations = {
  init(state, user) {
    // user.img = require('../../assets/' + user.img)
    state.self_user = user
    state.current_rid = user.rid
    state.init_timestamp = new Date().getTime()
  },
  ws_open(state, ws) {
    state.WS = ws
    state.ws_timestamp = new Date().getTime()
  },
  ws_enter_room(state, info) {
    let users = JSON.parse(info.get('users'))
    // state.users = users
    // let users = JSON.parse(info.users)
    // console.log(info.messages)
    // let messages = JSON.parse(info.messages)
    users.forEach(i => {
      state.online_users.put(i.rid + '@' + i.uid + '@' + i.name, i)
    })
    let chats = JSON.parse(info.get('chats'))
    // state.messages = messages
    chats.forEach(i => {
      if (i.uid == state.self_user.uid) {
        i.self = true
      }
    })
    state.chats = chats
    console.log('初始化教室状态', state.online_users, state.chats)
  },
  ws_one_enter(state, user) {
    state.online_users.put(user.rid + '@' + user.uid + '@' + user.name, user)
  },
  ws_one_leave(state, user) {
    state.online_users.remove(user.rid + '@' + user.uid + '@' + user.name)
    console.log(state.online_users)
    console.log(state.online_users.size())
  },
  ws_one_chat(state, msg) {
    state.chats.push(msg)
    console.log(state.chats)
  }
}

const actions = {
  INIT(context, payload) {
    context.commit('init', payload)
  },
}

const getAvatarNum = function(num) {
  let n = num % 18
  // if (n == 1) {
  //   return n + 1
  // }
  return 18 - n
}

export default {
  state,
  mutations,
  actions
}