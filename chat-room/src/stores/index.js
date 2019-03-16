import Vue from 'vue';
import Vuex from 'vuex';

import init from './modules/init';

Vue.use(Vuex)

export default new Vuex.Store({
  modules: {
    init
  }
})