export interface CardContainerProps {
  projects: Project[]
}

export interface Project {
  id: number
  title: string
  description?: string
  updatedAt: string
  ownerId: number
  createdAt: string
  isPublic: boolean
  fileCount: number
}
