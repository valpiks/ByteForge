export interface ExecutionMessage {
  type:
    | 'EXECUTION_STARTED'
    | 'OUTPUT'
    | 'INPUT_REQUEST'
    | 'INPUT_ECHO'
    | 'EXECUTION_COMPLETED'
    | 'EXECUTION_STOPPED'
    | 'ERROR'
  message: string
  exitCode?: number
  timestamp: number
}

export interface WebSocketPayload {
  code?: string
  input?: string
  filePath?: string
  content?: string
  type: string
}
