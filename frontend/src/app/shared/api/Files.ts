import apiClient from './Config'

export const fileApi = {
  async getFiles(projectId: number) {
    const response = await apiClient.get(`/project/${projectId}/files`)
    return response.data
  },
}
