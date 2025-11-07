import { ExecutionMessage } from '../types/websocket';

class WebSocketService {
  private socketURL: string = '/ws';
  private socket: WebSocket | null = null
  private isConnected: boolean = false
  private messageCallbacks: ((message: ExecutionMessage) => void)[] = []
  private projectId: string | null = null
  private currentSessionId: string = 'unknown'
  private connectionId: string = ''
  private reconnectAttempts: number = 0
  private maxReconnectAttempts: number = 5
  private userId: number | null = null
  private username: string = ''
  private email: string = ''

  private generateConnectionId(): string {
    return `conn_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
  }

  setUserInfo(userId: number, username: string, email: string): void {
    this.userId = userId
    this.username = username
    this.email = email
  }

  connect(projectId: string): Promise<boolean> {
    return new Promise((resolve) => {
      this.projectId = projectId
      this.connectionId = this.generateConnectionId()

      console.log(`🔄 [${this.connectionId}] Starting WebSocket connection...`)

      try {
        this.socket = new WebSocket(`${this.socketURL}/project/${this.projectId}`)

        this.socket.onopen = (event) => {
          console.log(`✅ [${this.connectionId}] WebSocket CONNECTED!`)
          this.isConnected = true
          this.reconnectAttempts = 0

          this.sendAuthentication()
          this.getOnlineUsers()
          resolve(true)
        }

        this.socket.onmessage = (event) => {
          this.handleMessage(event.data)
        }

        this.socket.onclose = (event) => {
          console.log(`🔌 [${this.connectionId}] WebSocket disconnected:`, event.code, event.reason)
          this.isConnected = false
          this.handleReconnection()
        }

        this.socket.onerror = (event) => {
          console.error(`❌ [${this.connectionId}] WebSocket error:`, event)
          this.isConnected = false
        }
      } catch (error) {
        console.error(`💥 [${this.connectionId}] Connection failed:`, error)
        resolve(false)
      }
    })
  }

  private sendAuthentication(): void {
    if (!this.socket) return

    const authMessage = {
      type: 'AUTH',
      userId: this.userId,
      username: this.username,
      email: this.email,
      projectId: this.projectId,
      connectionId: this.connectionId,
      timestamp: Date.now(),
    }

    this.socket.send(JSON.stringify(authMessage))
  }

  private handleMessage(data: string): void {
    try {
      const message = JSON.parse(data)

      if (message.type === 'SESSION_INFO' && message.sessionId) {
        this.currentSessionId = message.sessionId
      }

      this.messageCallbacks.forEach((callback) => {
        try {
          callback(message)
        } catch (e) {
          console.error(`❌ [${this.connectionId}] Callback error:`, e)
        }
      })
    } catch (error) {
      console.error(`❌ [${this.connectionId}] Message parse error:`, error, data)
    }
  }

  private handleReconnection(): void {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++
      console.log(`🔄 [${this.connectionId}] Reconnecting... (attempt ${this.reconnectAttempts})`)

      setTimeout(() => {
        this.connect(this.projectId!)
      }, 3000)
    }
  }

  private sendMessage(type: string, payload: any): void {
    if (!this.socket || !this.isConnected) {
      console.error(`❌ [${this.connectionId}] Cannot send - not connected`)
      return
    }

    const message = {
      type,
      ...payload,
      sessionId: this.currentSessionId,
      connectionId: this.connectionId,
      timestamp: Date.now(),
    }

    try {
      this.socket.send(JSON.stringify(message))
      console.log(`📤 [${this.connectionId}] Sent ${type}:`, message)
    } catch (error) {
      console.error(`❌ [${this.connectionId}] Send error:`, error)
    }
  }

  getOnlineUsers(): void {
    this.sendMessage('GET_ONLINE_USERS', {})
  }

  executeCode(code: string, filePath: string): void {
    this.sendMessage('EXECUTE_CODE', {
      code,
      filePath,
    })
  }

  sendInput(input: string): void {
    this.sendMessage('SEND_INPUT', {
      input,
    })
  }

  stopExecution(): void {
    this.sendMessage('STOP_EXECUTION', {})
  }

  saveFile(fileId: number, content: string): void {
    this.sendMessage('FILE_SAVE', {
      fileId,
      content,
    })
  }

  createFile(fileName: string, path: string, fileType: string, parentId: number): void {
    this.sendMessage('FILE_CREATE', {
      fileName,
      path,
      fileType,
      parentId,
    })
  }

  executeMultiFile(files: Record<string, string>, entryPoint: string): void {
    this.sendMessage('EXECUTE_CODE', {
      files,
      entryPoint,
    })
  }

  deleteFile(fileId: number): void {
    this.sendMessage('FILE_DELETE', {
      fileId,
    })
  }

  renameFile(newFileName: string, fileId: number): void {
    this.sendMessage('FILE_RENAME', {
      newFileName,
      fileId,
    })
  }

  kickUser(userId: number): void {
    this.sendMessage('KICK_USER', {
      userId,
    })
  }

  onExecutionMessage(callback: (message: ExecutionMessage) => void): void {
    this.messageCallbacks.push(callback)
  }

  offExecutionMessage(callback: (message: ExecutionMessage) => void): void {
    this.messageCallbacks = this.messageCallbacks.filter((cb) => cb !== callback)
  }

  disconnect(): void {
    if (this.socket) {
      this.socket.close(1000, 'Manual disconnect')
      this.socket = null
    }
    this.isConnected = false
    this.messageCallbacks = []
  }

  get connectionStatus(): boolean {
    return this.isConnected
  }

  get sessionId(): string {
    return this.currentSessionId
  }
}

export default new WebSocketService()
