import { useProjectStore } from '@/stores/project'
import { type Ref } from 'vue'

export function useWebSocketHandlers(
  activeFile: Ref<any>,
  activeFileContent: Ref<string>,
  openFiles: Ref<Set<any>>,
  openFolders: Ref<Set<number>>,
  outputContent: Ref<string>,
  isExecuting: Ref<boolean>,
  isAwaitingInput: Ref<boolean>,
) {
  const projectStore = useProjectStore()

  const handleFileSaved = (message: any) => {
    const fileId = Number(message.fileId)

    updateFileInStore(fileId, {
      content: message.content,
      originalContent: message.content,
      hasUnsavedChanges: false,
    })

    if (activeFile.value && activeFile.value.id === fileId) {
      activeFile.value.content = message.content
      activeFile.value.originalContent = message.content
      activeFileContent.value = message.content
      activeFile.value.hasUnsavedChanges = false
    }

    for (const file of openFiles.value) {
      if (file.id === fileId) {
        file.content = message.content
        file.originalContent = message.content
        file.hasUnsavedChanges = false
      }
    }
  }

  const handleFileCreated = (message: any) => {
    const newFile = message.file

    if (!projectStore.currentProjectFiles.some((file) => file?.id === newFile?.id)) {
      projectStore.currentProjectFiles.push({
        ...newFile,
        hasUnsavedChanges: false,
        originalContent: newFile?.content || '',
      })
    }

    if (newFile?.parentId) {
      projectStore.openFolders.add(newFile?.parentId)
      openFolders.value.add(newFile?.parentId)
    }
  }

  const handleFileDeleted = (message: any) => {
    const fileId = message.fileId

    projectStore.currentProjectFiles = projectStore.currentProjectFiles.filter(
      (file) => file.id !== fileId,
    )

    if (openFiles.value.has(fileId)) {
      closeFile(fileId)
    }

    if (activeFile.value?.id === fileId) {
      const filesArray = Array.from(openFiles.value)
      activeFile.value = filesArray[filesArray.length - 1] || null
      activeFileContent.value = activeFile.value?.content || ''
    }
  }

  const handleFileRenamed = (message: any) => {
    const { fileId, name, newPath } = message

    updateFileInStore(fileId, {
      name: name,
      path: newPath,
    })

    for (const file of openFiles.value) {
      if (file.id === fileId) {
        file.name = name
        file.path = newPath
      }
    }

    if (activeFile.value?.id === fileId) {
      activeFile.value.name = name
      activeFile.value.path = newPath
    }
  }

  const updateFileInStore = (fileId: number, updates: any) => {
    const fileIndex = projectStore.currentProjectFiles.findIndex((file) => file.id === fileId)
    if (fileIndex !== -1) {
      projectStore.currentProjectFiles[fileIndex] = {
        ...projectStore.currentProjectFiles[fileIndex],
        ...updates,
      }
    }
  }

  const closeFile = (fileId: number) => {
    openFiles.value = new Set(Array.from(openFiles.value).filter((file) => file.id !== fileId))
  }

  const handleExecutionMessage = (message: any) => {
    switch (message.type) {
      case 'EXECUTION_STARTED':
        outputContent.value = 'Execution started...\n'
        isExecuting.value = true
        break
      case 'OUTPUT':
        outputContent.value += message.message + '\n'
        break
      case 'INPUT_REQUIRED':
        outputContent.value += 'Program is waiting for input...\n'
        isAwaitingInput.value = true
        break
      case 'EXECUTION_COMPLETED':
      case 'EXECUTION_RESULT':
        if (message.error) {
          outputContent.value += `\nError: ${message.error}\n`
        }
        outputContent.value += `\nExecution completed with exit code: ${message.exitCode || message.exit_code}\n`
        isExecuting.value = false
        isAwaitingInput.value = false
        break
      case 'ERROR':
        outputContent.value += `\nError: ${message.message}\n`
        outputContent.value += `\nExecution completed with exit code: ${message.exitCode}\n`
        isExecuting.value = false
        isAwaitingInput.value = false
        break
    }
  }

  return {
    handleFileSaved,
    handleFileCreated,
    handleFileDeleted,
    handleFileRenamed,
    handleExecutionMessage,
  }
}
