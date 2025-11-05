import { ref } from 'vue'
import { projectApi } from '../api/Project'
import { ExportRequest, ExportStatus } from './types'

export const useProjectExport = () => {
  const isExporting = ref(false)
  const exportStatus = ref<ExportStatus | null>(null)
  const error = ref<string | null>(null)

  const startExport = async (projectId: number, request: ExportRequest) => {
    try {
      isExporting.value = true
      error.value = null
      exportStatus.value = null

      const response = await projectApi.exportProject(projectId, request)

      exportStatus.value = response.data
      pollExportStatus(projectId, exportStatus.value.exportId)
    } catch (err: any) {
      error.value = err.response?.data?.message || err.message || 'Export failed to start'
      isExporting.value = false
    }
  }

  const pollExportStatus = async (projectId: number, exportId: string) => {
    const interval = setInterval(async () => {
      try {
        const response = await projectApi.getExportStatus(projectId, exportId)
        const status: ExportStatus = response.data

        exportStatus.value = status

        if (status.status === 'COMPLETED') {
          clearInterval(interval)
          isExporting.value = false
          await downloadExportFile(projectId, exportId)
        } else if (status.status === 'FAILED') {
          clearInterval(interval)
          isExporting.value = false
          error.value = status.message || 'Export failed'
        }
      } catch (err: any) {
        clearInterval(interval)
        isExporting.value = false
        error.value = err.response?.data?.message || 'Failed to check export status'
      }
    }, 1000)
  }

  const downloadExportFile = async (projectId: number, exportId: string) => {
    try {
      console.log('📥 Downloading export file...')

      const response = await projectApi.getDownloadUrl(projectId, exportId)

      const url = window.URL.createObjectURL(new Blob([response.data]))
      const link = document.createElement('a')
      link.href = url

      const contentDisposition = response.headers['content-disposition']
      let fileName = `project_${projectId}_export.${exportStatus.value?.format?.toLowerCase() || 'zip'}`

      if (contentDisposition) {
        const fileNameMatch = contentDisposition.match(/filename="(.+)"/)
        if (fileNameMatch && fileNameMatch[1]) {
          fileName = fileNameMatch[1]
        }
      }

      link.setAttribute('download', fileName)
      document.body.appendChild(link)
      link.click()
      link.remove()
      window.URL.revokeObjectURL(url)
    } catch (err: any) {
      error.value = 'Failed to download file: ' + (err.response?.data?.message || err.message)
    }
  }

  const cancelExport = () => {
    isExporting.value = false
    exportStatus.value = null
    error.value = null
  }

  return {
    isExporting,
    exportStatus,
    error,
    startExport,
    cancelExport,
  }
}
