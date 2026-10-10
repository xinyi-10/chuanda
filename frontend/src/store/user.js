import { defineStore } from 'pinia'
import * as authApi from '@/api/auth'

/** 用户状态：token 持久化到 localStorage，用户信息内存态 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: null
  }),

  getters: {
    isLogin: (state) => !!state.token
  },

  actions: {
    async login(form) {
      const data = await authApi.login(form)
      this.setToken(data.token)
    },

    async register(form) {
      const data = await authApi.register(form)
      this.setToken(data.token)
    },

    setToken(token) {
      this.token = token
      localStorage.setItem('token', token)
    },

    async fetchInfo() {
      this.userInfo = await authApi.getInfo()
      return this.userInfo
    },

    logout() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('token')
    }
  }
})
