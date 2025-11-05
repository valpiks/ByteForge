import { ExportRequest } from '../composables/types'
import apiClient from './Config'
import { ProjectData } from './types'

export const projectApi = {
  async getProject(projectId: number) {
    const response = await apiClient.get(`/project/${projectId}`)
    return response.data
  },

  async getProjects() {
    const response = await apiClient.get('/project')
    return response.data
  },

  async createProject(projectData: ProjectData) {
    const response = await apiClient.post('/project', projectData)
    return response.data
  },

  async editProject(projectData: ProjectData, projectId: number) {
    const response = await apiClient.put(`/project/${projectId}`, projectData)
    return response.data
  },

  async deleteProject(projectId: number) {
    const response = await apiClient.delete(`/project/${projectId}`)
    return response.data
  },

  async getJoinLink(projectId: number) {
    const response = await apiClient.get(`/project/${projectId}/join-link`)
    return response.data
  },

  async sendJoinLink(token: string, role: string) {
    const response = await apiClient.post(`/project/join-link`, { token, role })
    return response.data
  },

  async getContributors(projectId: number) {
    const response = await apiClient.get(`/project/${projectId}/contributors`)
    return response.data
  },

  async changecontributorRole(projectId: number, contributorId: number, role: string) {
    const response = await apiClient.put(`/project/${projectId}/contributors/${contributorId}`, {
      role,
    })
    return response.data
  },

  async removecontributorFromProject(projectId: number, contributorId: number) {
    const response = await apiClient.delete(`/project/${projectId}/contributors/${contributorId}`)
    return response.data
  },

  async exportProject(projectId: number, request: ExportRequest) {
    const response = await apiClient.post(`/project/${projectId}/export`, request)
    return response
  },

  async getExportStatus(projectId: number, exportId: number) {
    const response = await apiClient.get(`/project/${projectId}/export/${exportId}/status`)
    return response
  },

  async getDownloadUrl(projectId: number, exportId: number) {
    const response = await apiClient.get(`/project/${projectId}/export/${exportId}/download`, {
      responseType: 'blob',
    })
    return response
  },
}
