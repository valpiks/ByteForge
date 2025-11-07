import axios from 'axios';
import { useToast } from 'vue-toastification';
import { authApi } from './Auth';

const toast = useToast()

const API_BASE_URL = '/api/v1';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
})

let refreshing: null | Promise<void> = null

apiClient.interceptors.response.use(
  (r) => r,
  async (error) => {
    const { response, config } = error
    if (!response) return Promise.reject(error)

    if (response.status === 401 && !config.url?.includes('/auth/refresh')) {
      if (refreshing) {
        await refreshing
        return apiClient(config)
      }

      try {
        refreshing = authApi.refresh()
        await refreshing
        return apiClient(config)
      } catch (e) {
        toast.error('Сессия истекла. Войдите снова.')
        window.location.href = '/login'
        return Promise.reject(e)
      } finally {
        refreshing = null
      }
    }
    return Promise.reject(error)
  },
)

export default apiClient
