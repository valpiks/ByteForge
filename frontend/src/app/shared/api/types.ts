export interface ProjectData {
  title: string
  description?: string
}

export interface LoginData {
  email: string
  password: string
}

export interface RegisterData extends LoginData {
  username: string
}

export interface AuthResponse {
  id: number
  username: string
  email: string
  created_at: string
  profileImageUrl: string
}

export interface FileData {
  name: string
  type: 'file' | 'folder'
  parentId?: number
  content?: string
}

export interface UpdateUserData {
  username: string
  email: string
}
