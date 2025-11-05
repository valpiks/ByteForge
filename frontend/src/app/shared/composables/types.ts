export interface ExportRequest {
  includeGit: boolean
  format: 'RAR' | 'ZIP'
}

export interface ExportStatus {
  exportId: string
  status: 'PROCESSING' | 'COMPLETED' | 'FAILED'
  progress: number
  message: string | null
  downloadUrl?: string | null
  createdAt: string
}
