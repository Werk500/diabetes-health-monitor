import { defineStore } from 'pinia'
import { userApi } from '../api'

export const useUserStore = defineStore('user', {
  state: () => ({
    accessToken: localStorage.getItem('accessToken') || '',
    userInfo: JSON.parse(localStorage.getItem('user') || '{}')
  }),
  getters: {
    isAdmin: (state) => state.userInfo.role === 1,
    isLoggedIn: (state) => !!state.accessToken
  },
  actions: {
    setToken(token) {
      this.accessToken = token
      localStorage.setItem('accessToken', token)
      localStorage.removeItem('token')
    },
    setUserInfo(user) {
      this.userInfo = user
      localStorage.setItem('user', JSON.stringify(user))
    },
    async logout() {
      try {
        await userApi.logout()
      } catch (_) {
        // 即使服务端会话已过期，也要清理本地状态
      } finally {
        this.accessToken = ''
        this.userInfo = {}
        localStorage.removeItem('accessToken')
        localStorage.removeItem('token')
        localStorage.removeItem('user')
      }
    }
  }
})
