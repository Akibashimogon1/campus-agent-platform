import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('mcp_access_token') || '',
    username: localStorage.getItem('mcp_username') || '',
    role: localStorage.getItem('mcp_role') || '',
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token),
  },
  actions: {
    setSession(loginResult) {
      this.token = loginResult.accessToken
      this.username = loginResult.username
      this.role = loginResult.role
      localStorage.setItem('mcp_access_token', this.token)
      localStorage.setItem('mcp_username', this.username)
      localStorage.setItem('mcp_role', this.role)
    },
    clearSession() {
      this.token = ''
      this.username = ''
      this.role = ''
      localStorage.removeItem('mcp_access_token')
      localStorage.removeItem('mcp_username')
      localStorage.removeItem('mcp_role')
    },
  },
})
