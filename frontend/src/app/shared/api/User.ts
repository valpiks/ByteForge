import apiClient from './Config'
import { AuthResponse, UpdateUserData } from './types'

export const userApi = {
  async getCurrentUser(): Promise<AuthResponse> {
    const response = await apiClient.get<AuthResponse>('/user')
    return response.data
  },

  async updateCurrentUser(userData: UpdateUserData): Promise<AuthResponse> {
    const response = await apiClient.put<AuthResponse>('/user', userData)
    return response.data
  },

  async uploadProfileImage(file: File): Promise<AuthResponse> {
    const formData = new FormData()
    formData.append('file', file)
    const response = await apiClient.post<AuthResponse>('/profile-image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })

    return response.data
  },
}
