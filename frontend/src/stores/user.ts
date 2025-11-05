
import { authApi } from '@/app/shared/api/Auth'
import { AuthResponse, LoginData, RegisterData } from '@/app/shared/api/types'
import { userApi } from '@/app/shared/api/User'
import { defineStore } from 'pinia'
import { useToast } from 'vue-toastification'

const toast = useToast()

export const useUserStore = defineStore('user', {
  state: () => ({
    user: null as AuthResponse | null,
    isAuthenticated: false,
    isLoading: false,
  }),

  getters: {
    currentUser: (state) => state.user,
  },

  actions: {
    async login(credentials: LoginData) {
      this.isLoading = true
      try {
        const userData = await authApi.login(credentials)
        this.user = userData
        this.isAuthenticated = true
        return userData
      } catch (error) {
        this.isAuthenticated = false
        this.user = null
        toast.error(error.response.data.message)
      } finally {
        this.isLoading = false
      }
    },

    async register(userData: RegisterData) {
      this.isLoading = true
      try {
        const newUser = await authApi.register(userData)
        this.user = newUser
        this.isAuthenticated = true
        return newUser
      } catch (error) {
        this.isAuthenticated = false
        this.user = null
        toast.error(error.response.data.message)
      } finally {
        this.isLoading = false
      }
    },

    async refreshTokens() {
      try {
        await authApi.refresh()
        await this.fetchUser()
        this.isAuthenticated = true
        return true
      } catch (error) {
        this.isAuthenticated = false
        this.user = null
        throw error
      }
    },

    async fetchUser() {
      try {
        const userData = await userApi.getCurrentUser()
        this.user = userData
        this.isAuthenticated = true
        return userData
      } catch (error) {
        this.isAuthenticated = false
        this.user = null
        throw error
      }
    },

    async logout() {
      try {
        await authApi.logout()
      } catch (error) {
        toast.error(error.response.data.message)
      } finally {
        this.clearUser()
      }
    },

    async initializeAuth() {
      try {
        await this.fetchUser()
        return true
      } catch (error) {
        try {
          await this.refreshTokens()
          return true
        } catch (refreshError) {
          this.clearUser()
          return false
        }
      }
    },

    clearUser() {
      this.user = null
      this.isAuthenticated = false
    },
  },
})
