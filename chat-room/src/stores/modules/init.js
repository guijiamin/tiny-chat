const state = {
  login_info: {},
  enter_timestamp: 0,
}

const mutations = {
  login(state, info) {
    state.login_info = info
    state.enter_timestamp = new Date().getTime()
    console.log(state.login_info, state.enter_timestamp)
  }
}

const actions = {
  LOGIN(context, payload) {
    context.commit('login', payload)
  }
}

export default {
  state,
  mutations,
  actions
}