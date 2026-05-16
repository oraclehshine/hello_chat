import { defineStore } from 'pinia'

export const useAdminStore = defineStore('admin', {
  state: () => ({
    token: localStorage.getItem('adminToken') || '',
    displayName: localStorage.getItem('adminName') || '',
  }),
  getters: {
    loggedIn: (state) => Boolean(state.token),
  },
  actions: {
    setSession(token: string, displayName: string) {
      this.token = token
      this.displayName = displayName
      localStorage.setItem('adminToken', token)
      localStorage.setItem('adminName', displayName)
    },
    logout() {
      this.token = ''
      this.displayName = ''
      localStorage.removeItem('adminToken')
      localStorage.removeItem('adminName')
    },
  },
})

