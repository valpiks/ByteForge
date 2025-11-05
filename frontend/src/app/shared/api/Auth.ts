import apiClient from './Config'
import { AuthResponse, LoginData, RegisterData } from './types'

export const authApi = {
  async login(data: LoginData): Promise<AuthResponse> {
    const response = await apiClient.post<AuthResponse>('/auth/signIn', data)
    return response.data
  },

  async register(data: RegisterData): Promise<AuthResponse> {
    const response = await apiClient.post<AuthResponse>('/auth/signUp', data)
    return response.data
  },

  async refresh(): Promise<void> {
    const response = await apiClient.post('/auth/refresh')
    return response?.data
  },

  async logout(): Promise<void> {
    await apiClient.post('/auth/signOut')
  },
}
