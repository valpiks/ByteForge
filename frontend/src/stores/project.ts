import { fileApi } from '@/app/shared/api/Files'
import { projectApi } from '@/app/shared/api/Project'
import { defineStore } from 'pinia'
import { useToast } from 'vue-toastification'

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

export interface ProjectData {
  title: string
  description?: string
}

export interface FileNode {
  id: number
  name: string
  path: string
  type: 'FILE' | 'FOLDER'
  hasUnsavedChanges: boolean
  content?: string
  parentId?: number
  children?: FileNode[]
}

export interface FileData {
  name: string
  type: 'FILE' | 'FOLDER'
  path: string
  parentId?: number
  content?: string
}

export interface ExecutionResult {
  error: string
  executionTimeMs: number
  exitCode: number
  memoryExceeded: boolean
  memoryUsedKb: number
  output: string
  status: string
  timeOut: boolean
}

export interface Contributor {
  id: number
  username: string
  email: string
  role: string
  online: boolean
  projectId: number
}

const toast = useToast()

export const useProjectStore = defineStore('project', {
  state: () => ({
    projectList: [] as Project[],
    currentProjectFiles: [] as FileNode[],
    openFolders: new Set<number>(),
    contributors: [] as Contributor[],
    currentProject: {} as Project,
  }),

  getters: {
    fileTree: (state) => {
      const fileMap = new Map<number, FileNode>()
      const roots: FileNode[] = []

      state.currentProjectFiles?.forEach((file) => {
        fileMap.set(file?.id, { ...file, hasUnsavedChanges: false, children: [] })
      })

      state.currentProjectFiles?.forEach((file) => {
        const node = fileMap.get(file?.id)!
        if (file?.parentId) {
          const parent = fileMap.get(file?.parentId)
          if (parent) {
            parent.children?.push(node)
          }
        } else {
          roots.push(node)
        }
      })

      const sortTree = (nodes: FileNode[]): FileNode[] => {
        return nodes
          .sort((a, b) => {
            if (a.type !== b.type) {
              return a.type === 'FOLDER' ? -1 : 1
            }
            return a.name.localeCompare(b.name)
          })
          .map((node) => ({
            ...node,
            children: node.children ? sortTree(node.children) : undefined,
          }))
      }

      return sortTree(roots)
    },

    findFileById: (state) => (id: number) => {
      return state.currentProjectFiles.find((file) => file.id === id) || null
    },
  },

  actions: {
    async getListOfProjects() {
      const result = await projectApi.getProjects()

      if (result.reponse?.data?.message) {
        return toast.error(result.reponse.data.message)
      }

      this.projectList = result
    },

    async getProjectById(projectId: number) {
      const result = await projectApi.getProject(projectId)

      if (result.reponse?.data?.message) {
        return toast.error(result.reponse.data.message)
      }

      return result
    },

    async getJoinLink(projectId: number) {
      const result = await projectApi.getJoinLink(projectId)

      if (result.reponse?.data?.message) {
        return toast.error(result.reponse.data.message)
      }

      return result.token
    },

    async sendToken(token: string, role: string) {
      const result = await projectApi.sendJoinLink(token, role)

      if (result.reponse?.data?.message) {
        return toast.error(result.reponse.data.message)
      }

      return result.projectId
    },

    async createProject(data: ProjectData) {
      const result = await projectApi.createProject(data)

      if (result.reponse?.data?.message) {
        return toast.error(result.reponse.data.message)
      }
      toast.success('Project created successfully')
      const newProjectList = await projectApi.getProjects()
      this.projectList = newProjectList
      return result
    },

    async editProject(data: ProjectData, projectId: number) {
      const result = await projectApi.editProject(data, projectId)

      if (result.response?.data?.message) {
        return toast.error(result.response.data.message)
      }

      this.currentProject = result

      const newProjectList = await projectApi.getProjects()
      this.projectList = newProjectList
    },

    async deleteProject(projectId: number) {
      const result = await projectApi.deleteProject(projectId)

      if (result.reponse?.data?.message) {
        return toast.error(result.reponse.data.message)
      }

      const newProjectList = await projectApi.getProjects()
      this.projectList = newProjectList
      toast.success('Project deleted successfully')
    },

    async loadProjectFiles(projectId: number) {
      const files = await fileApi.getFiles(projectId)
      this.currentProjectFiles = files
      return this.fileTree
    },

    async loadContributors(projectId: number) {
      const contributors = await projectApi.getContributors(projectId)
      this.contributors = contributors
    },

    async createFile(fileData: FileData, projectId: number) {
      const newFile = await fileApi.createFile(fileData, projectId)
      this.currentProjectFiles.push(newFile)

      if (newFile.parentId) {
        this.openFolders.add(newFile.parentId)
      }

      toast.success(`${newFile.type === 'FILE' ? 'File' : 'Folder'} created successfully`)
      return newFile
    },

    async executeCode(code: string): Promise<ExecutionResult | null> {
      const result = await fileApi.executeFile(code)

      if (result.reponse?.data?.message) {
        toast.error(result.reponse.data.message)
        return null
      }

      return result
    },

    async deleteFile(fileId: number, projectId: number) {
      await fileApi.deleteFile(projectId, fileId)
      this.currentProjectFiles = this.currentProjectFiles.filter((file) => file.id !== fileId)
      toast.success('File deleted successfully')
    },

    async updateFileContent(fileId: number, content: string, projectId: number) {
      const updatedFile = await fileApi.updateFile(content, projectId, fileId)
      const fileIndex = this.currentProjectFiles.findIndex((file) => file.id === fileId)
      if (fileIndex !== -1) {
        this.currentProjectFiles[fileIndex].content = content
      }
      return updatedFile
    },

    async changeContributorRole(projectId: number, contributorId: number, newRole: string) {
      await projectApi.changecontributorRole(projectId, contributorId, newRole)

      const contributor = this.contributors.find((cont) => cont.id === contributorId)
      if (contributor) {
        contributor.role = newRole
      }
    },

    async removeContributor(projectId: number, contributorId: number) {
      await projectApi.removecontributorFromProject(projectId, contributorId)

      this.contributors = this.contributors.filter((cont) => cont.id !== contributorId)
    },

    toggleFolder(folderId: number) {
      if (this.openFolders.has(folderId)) {
        this.openFolders.delete(folderId)
      } else {
        this.openFolders.add(folderId)
      }
    },

    clearCurrentProject() {
      this.currentProjectFiles = []
      this.openFolders.clear()
    },
  },
})
