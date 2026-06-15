import { createStore } from 'vuex'

const store = createStore({
  state: {
    user: JSON.parse(sessionStorage.getItem('user') || '{}')
  },
  mutations: {
    setUser(state, user) {
      state.user = user
      sessionStorage.setItem('user', JSON.stringify(user))
    }
  },
  getters: {
    isLoggedIn: state => !!state.user.token,
    isAdmin: state => state.user.role === 'admin'
  }
})

export default store