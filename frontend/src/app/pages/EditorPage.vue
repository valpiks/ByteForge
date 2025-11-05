<template>
  <ProjectNavbar
    :current-project-id="currentProjectId"
    :save-file="saveFile"
    :current-user-role="currentUserRole"
  />

  <div class="wrapper h-[calc(100dvh-4.1rem)] font-mono overflow-hidden">
    <div class="sidebar-box bg-editor-sidebar border-r border-editor-border">
      <div
        class="flex items-center justify-between p-4 text-muted-foreground border-b border-editor-border"
      >
        <h2 class="uppercase text-xs font-semibold">Explorer</h2>
        <div class="flex gap-2">
          <button @click="handleRootCreate('FILE')" v-if="currentUserRole !== 'VIEWER'">
            <Icon
              icon="ri:file-add-line"
              class="w-7 h-7 hover:bg-primary hover:text-white rounded-sm p-1 transition-colors"
            />
          </button>
          <button @click="handleRootCreate('FOLDER')" v-if="currentUserRole !== 'VIEWER'">
            <Icon
              icon="ri:folder-add-line"
              class="w-7 h-7 hover:bg-primary rounded-sm p-1 hover:text-white transition-colors"
            />
          </button>
        </div>
      </div>
      <div class="py-2 flex flex-col overflow-auto h-[calc(100%-65px)]" @click="handleSidebarClick">
        <FileTree
          v-for="node in fileTree"
          :key="node.id"
          :ref="(el) => (fileTreeRefs[node.id] = el)"
          :node="node"
          :open-folders="openFolders"
          :is-creating="isCreating"
          :creating-type="creatingType"
          :creating-location="creatingLocation"
          :active-context-menu="activeContextMenu"
          @delete-file="handleDeleteFile"
          @toggle-folder="toggleFolder"
          @select-file="selectFile"
          @create-item="handleCreateItem"
          @create-file="handleCreateFile"
          @cancel-create="cancelCreating"
          @show-context-menu="setActiveContextMenu"
        />

        <div v-if="isCreating && creatingLocation === null" class="px-4 py-1" ref="inputContainer">
          <input
            ref="nameInput"
            v-model="newItemName"
            @keydown.enter="handleRootInputEnter"
            @keydown.escape="handleRootInputEscape"
            @keydown.stop
            @click.stop
            type="text"
            class="w-full px-2 py-1 text-sm bg-editor-bg border border-primary rounded focus:outline-none focus:ring-1 focus:ring-primary"
            :placeholder="
              creatingType === 'FILE' ? 'File name with extension...' : 'Folder name...'
            "
          />
        </div>
      </div>
    </div>

    <div @mousedown="startSidebarResize" class="resize-handler horizontal"></div>

    <div class="flex flex-col w-full font-mono h-full" @click="closeContextMenu">
      <div class="w-full h-10 px-2 py-0.5 bg-editor-sidebar border-b border-editor-border flex">
        <div v-if="openFiles.size > 0" class="flex gap-2">
          <div
            v-for="file in Array.from(openFiles)"
            :key="file.id"
            class="relative px-3 py-1.5 rounded-t text-sm font-mono flex items-center gap-2 cursor-pointer transition-smooth group"
            :class="[activeFile?.id === file.id ? 'bg-editor-bg' : '']"
            @click="setActiveFile(file)"
          >
            <Icon icon="tabler:file-code" />
            <span>{{ file.name }}</span>
            <button @click.stop="closeFile(file)">
              <Icon
                icon="tabler:x"
                class="opacity-0 hover:text-red-400 group-hover:opacity-100 transition-opacity duration-400"
              />
            </button>
            <div
              v-if="file.hasUnsavedChanges"
              class="rounded-full bg-muted-foreground p-1 absolute right-4 group-hover:opacity-0"
            ></div>
          </div>
        </div>
        <div v-else class="px-3 py-1.5">
          <span class="text-muted-foreground text-sm">No files open</span>
        </div>
      </div>

      <div class="content-box bg-editor-bg overflow-hidden flex-1">
        <div class="flex w-full h-full overflow-hidden" v-if="activeFile">
          <div class="editor-container flex w-full h-full">
            <div
              class="line-numbers border-r border-border py-4 px-2 select-none sticky left-0 bg-editor-bg"
            >
              <pre class="font-mono text-xs text-muted-foreground leading-6">{{ rowsCount }}</pre>
            </div>
            <div class="editor-content flex-1">
              <textarea
                ref="editorTextarea"
                v-model="activeFileContent"
                @input="handleFileContentChange"
                @scroll="syncLineNumbersScroll"
                class="w-full h-full bg-transparent p-4 font-mono text-sm text-foreground resize-none focus:outline-none leading-6"
                placeholder="// Write your C++ code here..."
                :readonly="currentUserRole === 'VIEWER'"
              />
            </div>
          </div>
        </div>
        <div v-else class="w-full h-full flex gap-2 flex-col justify-center items-center text-sm">
          <Icon icon="tabler:file-code" class="w-16 h-16 opacity-40" />
          <span class="text-muted-foreground">No file selected </span>
          <span class="text-muted-foreground">Open a file from the explorer</span>
        </div>
      </div>

      <div @mousedown="startTerminalResize" class="resize-handler vertical"></div>

      <div class="terminal-container" :style="{ height: terminalHeight + 'px' }">
        <div
          class="bg-editor-sidebar border-y border-editor-border py-2 px-4 flex items-center justify-between"
        >
          <span>Output</span>
          <div class="flex gap-2 items-center">
            <Icon
              icon="lucide:play"
              class="w-4 h-4 cursor-pointer hover:text-primary transition-colors"
              @click="handleExecuteFile"
              :class="{ 'text-primary': isExecuting }"
              :disabled="!activeFile || currentUserRole === 'VIEWER'"
            />
            <Icon
              icon="lucide:square"
              class="w-4 h-4 cursor-pointer hover:text-destructive transition-colors"
              @click="stopExecution"
              :class="{ 'opacity-50 cursor-not-allowed': !isExecuting }"
              :disabled="currentUserRole === 'VIEWER'"
            />
            <div class="connection-status" :class="isConnected ? 'connected' : 'disconnected'">
              {{ isConnected ? '● Connected' : '○ Disconnected' }}
            </div>
          </div>
        </div>
        <div class="terminal-content bg-editor-bg h-full overflow-auto flex flex-col">
          <div class="flex-1 p-4 overflow-auto">
            <pre class="font-mono text-sm text-foreground whitespace-pre-wrap break-words">{{
              outputContent
            }}</pre>
          </div>

          <div v-if="isAwaitingInput" class="border-t border-editor-border p-4 bg-editor-sidebar">
            <div class="flex items-center gap-2">
              <span class="text-green-400 font-mono">>></span>
              <input
                v-model="currentInput"
                @keydown.enter="sendInputToProgram"
                @keydown.escape="cancelInput"
                ref="consoleInputRef"
                class="flex-1 bg-transparent border-none outline-none font-mono text-foreground"
                placeholder="Type your input here and press Enter..."
                :readonly="currentUserRole === 'VIEWER'"
              />
            </div>
          </div>
        </div>
      </div>
    </div>

    <div
      v-if="activeContextMenu && currentUserRole !== 'VIEWER'"
      class="context-menu fixed z-50 p-1 px-1 bg-editor-bg border border-editor-border rounded-md shadow-lg text-sm"
      :style="contextMenuStyle"
    >
      <div class="flex flex-col gap-1">
        <button
          @click="handleContextAction('delete')"
          class="hover:bg-muted py-1 px-4 rounded-sm w-full text-left"
        >
          Delete
        </button>
        <button
          @click="handleContextAction('rename')"
          class="hover:bg-muted py-1 px-4 rounded-sm w-full text-left"
        >
          Rename
        </button>
        <button
          v-if="activeContextMenuNode?.type === 'FOLDER'"
          @click="handleContextAction('create_file')"
          class="hover:bg-muted py-1 px-4 rounded-sm w-full text-left"
        >
          New File
        </button>
        <button
          v-if="activeContextMenuNode?.type === 'FOLDER'"
          @click="handleContextAction('create_folder')"
          class="hover:bg-muted py-1 px-4 rounded-sm w-full text-left"
        >
          New Folder
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useProjectStore } from '@/stores/project'
import { Icon } from '@iconify/vue'
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import FileTree from '../components/Editor/FileTree.vue'
import ProjectNavbar from '../components/UI/navbars/ProjectNavbar.vue'

import { useUserStore } from '@/stores/user'
import { useToast } from 'vue-toastification'
import websocketService from '../shared/api/websocket-service'
import { useFileOperations } from '../shared/composables/useFileOperations'
import { useResize } from '../shared/composables/useResize'
import { useWebSocketHandlers } from '../shared/composables/useWebsocketHandlers'

const route = useRoute()
const projectStore = useProjectStore()
const userStore = useUserStore()

const projectId = route.params.id as string
const currentProjectId = Number(projectId)

const currentUserRole = ref<string>('')

const {
  isCreating,
  creatingType,
  creatingLocation,
  newItemName,
  startCreating,
  cancelCreating,
  createItem,
} = useFileOperations()

const {
  isSidebarResizing,
  isTerminalResizing,
  terminalHeight,
  startSidebarResize,
  startTerminalResize,
} = useResize()

const isExecuting = ref(false)
const isConnected = ref(false)
const isAwaitingInput = ref(false)
const currentInput = ref('')
const outputContent = ref('Click "Run" to compile and execute your code...')
const activeContextMenu = ref<{ nodeId: number; x: number; y: number } | null>(null)
const activeContextMenuNode = ref<any>(null)
const contextMenuStyle = ref({ top: '0px', left: '0px' })
const openFolders = ref(new Set<number>())
const openFiles = ref(new Set<any>())
const activeFile = ref<any>(null)
const activeFileContent = ref('')

const projectSettings = ref(JSON.parse(localStorage.getItem('projectSettings')))

const fileTreeRefs = ref<Record<number, any>>({})
const nameInput = ref<HTMLInputElement>()
const inputContainer = ref<HTMLDivElement>()
const consoleInputRef = ref<HTMLInputElement>()
const editorTextarea = ref<HTMLTextAreaElement>()

let autoSaveTimeout: NodeJS.Timeout

const {
  handleFileSaved,
  handleFileCreated,
  handleFileDeleted,
  handleFileRenamed,
  handleExecutionMessage,
} = useWebSocketHandlers(
  activeFile,
  activeFileContent,
  openFiles,
  openFolders,
  outputContent,
  isExecuting,
  isAwaitingInput,
)

const fileTree = computed(() => projectStore.fileTree)
const rowsCount = computed(() => {
  const lines = activeFileContent.value.split('\n')
  return lines.map((_, index) => index + 1).join('\n')
})

const findCurrentUserRole = () => {
  if (!userStore.user?.id || !projectStore.contributors.length) return ''

  const contributor = projectStore.contributors.find((cont) => cont.id === userStore.user.id)
  return contributor?.role || ''
}

const handleWebSocketMessage = (message: any) => {
  console.log('=== VUE COMPONENT RECEIVED MESSAGE ===', message)

  switch (message.type) {
    case 'FILE_SAVED':
      handleFileSaved(message)
      break
    case 'FILE_CREATED':
      handleFileCreated(message)
      break
    case 'FILE_DELETED':
      handleFileDeleted(message)
      break
    case 'FILE_RENAMED':
      handleFileRenamed(message)
      break
    case 'ONLINE_USERS':
      handleOnlineUsers(message)
      break
    case 'USER_KICKED':
      handleUserKicked(message)
      break
    default:
      handleExecutionMessage(message)
      break
  }

  nextTick(() => {
    const terminalContent = document.querySelector('.terminal-content > div') as HTMLElement
    if (terminalContent) {
      terminalContent.scrollTop = terminalContent.scrollHeight
    }
  })
}

const handleUserKicked = (message: any) => {
  const toast = useToast()

  toast.error('You have been removed from the project')

  setTimeout(() => {
    window.location.href = '/projects'
  }, 2000)
}

const handleOnlineUsers = async (message: any) => {
  await projectStore.loadContributors(currentProjectId)

  const onlineUsers = message?.users
  const mapperUsers = []
  projectStore.contributors.forEach((cont) => {
    const isOnline = onlineUsers.some((onlineUser) => onlineUser.id === cont.id)
    mapperUsers.push({
      ...cont,
      online: isOnline,
    })
  })
  projectStore.contributors = mapperUsers
  currentUserRole.value = findCurrentUserRole()
}

const handleExecuteFile = async (): Promise<void> => {
  if (!activeFile.value) {
    outputContent.value = 'Error: No file selected\n'
    return
  }

  const code = activeFileContent.value

  if (!code.trim()) {
    outputContent.value = 'Error: No code to execute\n'
    return
  }

  await saveFile()

  outputContent.value = 'Compiling and running...\n'
  isExecuting.value = true
  isAwaitingInput.value = false
  currentInput.value = ''

  try {
    const shouldUseMultiFile = detectMultiFileMode()

    if (shouldUseMultiFile) {
      const allFiles = collectAllFiles()
      websocketService.executeMultiFile(allFiles, activeFile.value.path)
      console.log('Multi-file execution requested via WebSocket')
    } else {
      websocketService.executeCode(code, activeFile.value.path)
      console.log('Single file execution requested via WebSocket')
    }
  } catch (error) {
    outputContent.value += `\nError: ${error instanceof Error ? error.message : 'Unknown error'}\n`
    isExecuting.value = false
  }
}

const detectMultiFileMode = (): boolean => {
  if (!activeFile.value?.name.match(/\.(cpp|cxx|cc)$/)) return false

  const traverse = (nodes: any[]): any[] =>
    nodes.flatMap((node) =>
      node.type === 'FILE' ? [node] : node.children ? traverse(node.children) : [],
    )

  const allFiles = traverse(fileTree.value)
  const cppFiles = allFiles.filter((f) => /\.(cpp|cxx|cc)$/.test(f.name))
  const headerFiles = allFiles.filter((f) => /\.(h|hpp|hh)$/.test(f.name))

  const activeCode = activeFileContent.value
  const includes = [...activeCode.matchAll(/#include\s+"([^"]+)"/g)].map((m) => m[1])

  if (includes.length === 0 && cppFiles.length === 1) return false

  const hasLocalIncludes = includes.some((inc) =>
    [...cppFiles, ...headerFiles].some((f) => inc.endsWith(f.name)),
  )

  const hasMain = activeCode.includes('int main')

  return hasLocalIncludes || !hasMain
}

const collectAllFiles = (): Record<string, string> => {
  if (!activeFile.value) return {}

  const traverse = (nodes: any[]): any[] =>
    nodes.flatMap((node) =>
      node.type === 'FILE' ? [node] : node.children ? traverse(node.children) : [],
    )

  const allFiles = traverse(fileTree.value)
  const fileMap = new Map(allFiles.map((f) => [f.name, f]))
  const headerFiles = allFiles.filter((f) => /\.(h|hpp|hh)$/.test(f.name))
  const cppFiles = allFiles.filter((f) => /\.(cpp|cxx|cc)$/.test(f.name))

  const collected: Record<string, string> = {}
  const visited = new Set<number>()

  const collectDependencies = (file: any) => {
    if (!file || visited.has(file.id)) return
    visited.add(file.id)

    collected[file.path] = file.content || ''
    const code = file.content || ''

    const includes = [...code.matchAll(/#include\s+"([^"]+)"/g)].map((m) => m[1])

    for (const inc of includes) {
      const incName = inc.split('/').pop()
      const dep = fileMap.get(incName)
      if (dep) {
        collectDependencies(dep)

        if (/\.(h|hpp|hh)$/.test(dep.name)) {
          const baseName = dep.name.replace(/\.(h|hpp|hh)$/, '')
          const impl = cppFiles.find((f) => f.name.startsWith(baseName))
          if (impl) collectDependencies(impl)
        }
      }
    }
  }

  collectDependencies(activeFile.value)
  return collected
}

const sendInputToProgram = (): void => {
  if (!isAwaitingInput.value || !currentInput.value.trim()) return

  const value = currentInput.value
  console.log('Sending input to program:', value)

  try {
    websocketService.sendInput(value)
    outputContent.value += `>> ${value}\n`
    currentInput.value = ''
    isAwaitingInput.value = false
  } catch (error) {
    outputContent.value += `\nError sending input: ${error}\n`
  }
}

const cancelInput = (): void => {
  if (isAwaitingInput.value) {
    outputContent.value += '\nInput cancelled\n'
    currentInput.value = ''
    isAwaitingInput.value = false
  }
}

const stopExecution = (): void => {
  if (isExecuting.value) {
    websocketService.stopExecution()
    isExecuting.value = false
    isAwaitingInput.value = false
  }
}

const getActualSettings = () => {
  return JSON.parse(localStorage.getItem('projectSettings'))
}

watch([activeFileContent], ([newContent]) => {
  if (activeFile.value) {
    const existsContent = activeFile.value?.originalContent || activeFile.value.content
    activeFile.value.hasUnsavedChanges = newContent !== existsContent

    projectSettings.value = getActualSettings()

    if (isConnected.value && projectSettings.value.autoSave) {
      clearTimeout(autoSaveTimeout)
      autoSaveTimeout = setTimeout(() => {
        if (activeFile.value && activeFile.value.hasUnsavedChanges) {
          websocketService.saveFile(activeFile.value.id, newContent)
          activeFile.value.hasUnsavedChanges = false
        }
      }, 2000)
    }
  }
})

const syncLineNumbersScroll = (): void => {
  if (editorTextarea.value) {
    const lineNumbers = document.querySelector('.line-numbers pre') as HTMLElement
    if (lineNumbers) {
      lineNumbers.style.transform = `translateY(-${editorTextarea.value.scrollTop}px)`
    }
  }
}

const setActiveContextMenu = (nodeId: number, x: number, y: number): void => {
  const findNode = (nodes: any[]): any => {
    for (const node of nodes) {
      if (node.id === nodeId) return node
      if (node.children) {
        const found = findNode(node.children)
        if (found) return found
      }
    }
    return null
  }

  const node = findNode(fileTree.value)
  if (node) {
    activeContextMenuNode.value = node
    activeContextMenu.value = { nodeId, x, y }
    contextMenuStyle.value = { top: `${y}px`, left: `${x}px` }
  }
}

const handleContextAction = (action: string) => {
  if (!activeContextMenuNode.value) return

  switch (action) {
    case 'delete':
      handleDeleteFile(activeContextMenuNode.value.id)
      break
    case 'rename':
      if (fileTreeRefs.value[activeContextMenuNode.value.id]) {
        fileTreeRefs.value[activeContextMenuNode.value.id].startRenameForThisNode()
      }
      break
    case 'create_file':
      if (activeContextMenuNode.value.type === 'FOLDER') {
        openFolders.value.add(activeContextMenuNode.value.id)
        startCreating('FILE', activeContextMenuNode.value.id)
      }
      break
    case 'create_folder':
      if (activeContextMenuNode.value.type === 'FOLDER') {
        openFolders.value.add(activeContextMenuNode.value.id)
        startCreating('FOLDER', activeContextMenuNode.value.id)
      }
      break
  }

  closeContextMenu()
}

const closeContextMenu = (): void => {
  activeContextMenu.value = null
  activeContextMenuNode.value = null
}

const generatePath = (name: string, parentId: number | null): string => {
  if (!parentId) return name

  const parentFile = projectStore.findFileById(parentId)
  if (parentFile) {
    return `${parentFile.path}/${name}`
  }
  return name
}

const saveFile = async () => {
  if (activeFile.value && activeFile.value.hasUnsavedChanges) {
    websocketService.saveFile(activeFile.value.id, activeFileContent.value)
    activeFile.value.hasUnsavedChanges = false
  }
}

const handleRootCreate = (type: 'FILE' | 'FOLDER') => {
  startCreating(type, null, nameInput)
}

const handleRootInputEnter = (event: Event) => {
  event.stopPropagation()
  const name = newItemName.value.trim()
  if (name && typeof generatePath === 'function') {
    createItem(generatePath)
  } else {
    cancelCreating()
  }
}

const handleRootInputEscape = (event: Event) => {
  event.stopPropagation()
  cancelCreating()
}

const handleSidebarClick = (event: MouseEvent): void => {
  const target = event.target as HTMLElement

  closeContextMenu()

  if (isCreating.value && creatingLocation.value === null) {
    if (!inputContainer.value?.contains(target)) {
      const name = newItemName.value.trim()
      if (name && typeof generatePath === 'function') {
        createItem(generatePath)
      } else {
        cancelCreating()
      }
    }
  }
}

const handleCreateItem = (type: 'FILE' | 'FOLDER', folderId: number) => {
  if (type === 'FOLDER') {
    openFolders.value.add(folderId)
  }
  startCreating(type, folderId)
}

const handleCreateFile = (name: string, type: 'FILE' | 'FOLDER', folderId: number): void => {
  if (!name.trim()) {
    cancelCreating()
    return
  }

  newItemName.value = name
  creatingType.value = type
  creatingLocation.value = folderId
  if (typeof generatePath === 'function') {
    createItem(generatePath)
  }
}

const toggleFolder = (folderId: number): void => {
  if (openFolders.value.has(folderId)) {
    openFolders.value.delete(folderId)
  } else {
    openFolders.value.add(folderId)
  }
}

const selectFile = (file: any): void => {
  if (file.type === 'FILE') {
    const isAlreadyOpen = Array.from(openFiles.value).some((openFile) => openFile.id === file.id)

    if (!isAlreadyOpen) {
      openFiles.value.add(file)
    }

    setActiveFile(file)
  }
}

const setActiveFile = (file: any): void => {
  if (!file.originalContent) {
    file.originalContent = file.content
  }

  activeFile.value = file
  activeFileContent.value = file.content || ''
}

const closeFile = (file: any): void => {
  openFiles.value.delete(file)
  if (activeFile.value?.id === file.id) {
    const filesArray = Array.from(openFiles.value)
    activeFile.value = filesArray[filesArray.length - 1] || null
    activeFileContent.value = activeFile.value?.content || ''
  }
}

const handleFileContentChange = (): void => {
  if (activeFile.value) {
    activeFile.value.content = activeFileContent.value
  }
}

const handleDeleteFile = (id: number): void => {
  openFiles.value = new Set(Array.from(openFiles.value).filter((file) => file.id !== id))
  const filesArray = Array.from(openFiles.value)

  if (activeFile.value?.id === id) {
    activeFile.value = filesArray[filesArray.length - 1] || null
    activeFileContent.value = activeFile.value?.content || ''
  }

  websocketService.deleteFile(id)
}

const insertTextAtCursor = (text: string): void => {
  const textarea = editorTextarea.value
  if (!textarea) return

  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const value = textarea.value

  textarea.value = value.substring(0, start) + text + value.substring(end)
  textarea.selectionStart = textarea.selectionEnd = start + text.length
  textarea.focus()

  activeFileContent.value = textarea.value
  handleFileContentChange()
}

const handleGlobalKeyDown = (event: KeyboardEvent): void => {
  if (event.ctrlKey && event.key === 's') {
    event.preventDefault()
    saveFile()
    return
  }

  if (event.key === 'Tab' && editorTextarea.value === document.activeElement) {
    event.preventDefault()
    const tabSize = projectSettings.value?.tabSize || 2
    const spaces = ' '.repeat(tabSize)
    insertTextAtCursor(spaces)
  }

  if (event.ctrlKey && event.key === 'Enter') {
    event.preventDefault()
    handleExecuteFile()
    return
  }

  if (event.key === 'Escape' && isAwaitingInput.value) {
    event.preventDefault()
    cancelInput()
    return
  }
}

const checkWebSocketConnection = (): void => {
  isConnected.value = websocketService.connectionStatus
  if (!isConnected.value) {
    console.log('WebSocket not connected, attempting to reconnect...')
    websocketService.connect(projectId)
  }
}

watch(
  [() => projectSettings.value?.fontSize, () => activeFile.value],
  ([newSize, activeFile]) => {
    if (activeFile) {
      nextTick(() => {
        const editor = document.querySelector('.editor-content textarea') as HTMLTextAreaElement
        if (editor && newSize) {
          editor.style.fontSize = newSize
        }
      })
    }
  },
  { immediate: true },
)

onMounted(async (): Promise<void> => {
  localStorage.removeItem('joinToken')

  if (!localStorage.getItem('projectSettings')) {
    const settings = { autoSave: true, fontSize: '14px', tabSize: 2, lineNumbers: true }
    localStorage.setItem('projectSettings', JSON.stringify(settings))
    projectSettings.value = settings
  }

  projectStore.currentProject = await projectStore.getProjectById(currentProjectId)

  await projectStore.loadProjectFiles(currentProjectId)
  await projectStore.loadContributors(currentProjectId)

  currentUserRole.value = findCurrentUserRole()

  const { user } = useUserStore()

  websocketService.setUserInfo(user?.id, user?.username, user?.email)
  const connected = await websocketService.connect(projectId)

  if (connected) {
    isConnected.value = true
    websocketService.onExecutionMessage(handleWebSocketMessage)
  } else {
    isConnected.value = false
  }

  const connectionCheckInterval = setInterval(() => {
    checkWebSocketConnection()
  }, 3000)

  const projectSettingsInterval = setInterval(() => {
    projectSettings.value = getActualSettings()
  }, 2000)

  document.addEventListener('keydown', handleGlobalKeyDown, true)

  window.connectionCheckInterval = connectionCheckInterval
  window.projectSettingsInterval = projectSettingsInterval
})

onUnmounted((): void => {
  projectStore.clearCurrentProject()
  websocketService.disconnect()
  clearTimeout(autoSaveTimeout)

  if (window.connectionCheckInterval || window.projectSettingsInterval) {
    clearInterval(window.connectionCheckInterval)
    clearInterval(window.projectSettingsInterval)
  }

  document.removeEventListener('keydown', handleGlobalKeyDown, true)
})

declare global {
  interface Window {
    connectionCheckInterval?: NodeJS.Timeout
    projectSettingsInterval?: NodeJs.Timeout
  }
}
</script>

<style scoped>
.editor-container {
  display: flex;
  width: 100%;
  height: 100%;
  position: relative;
}

.line-numbers {
  position: sticky;
  left: 0;
  z-index: 1;
  background: inherit;
}

.line-numbers pre {
  transition: transform 0.1s ease;
  min-height: 100%;
}

.editor-content {
  flex: 1;
  position: relative;
}

.editor-content textarea {
  display: block;
  width: 100%;
  height: 100%;
  border: none;
  outline: none;
  resize: none;
}

.wrapper {
  display: flex;
}

.sidebar-box {
  width: 288px;
  border-radius: 0;
  box-sizing: border-box;
  height: 100%;
  transition: width 0.1s ease;
}

.content-box {
  border-radius: 0;
  box-sizing: border-box;
  height: 100%;
}

.resize-handler {
  background: transparent;
  flex: 0 0 auto;
  position: relative;
  transition: background-color 0.2s ease;
}

.resize-handler.horizontal {
  width: 1px;
  cursor: ew-resize;
  padding: 0;
}

.resize-handler.horizontal::before {
  content: '';
  display: block;
  height: 100%;
  margin: 0 auto;
  transition: background-color 0.2s ease;
}

.resize-handler.vertical {
  height: 1px;
  cursor: ns-resize;
  width: 100%;
  transition: background-color 0.2s ease;
}

.terminal-container {
  transition: height 0.1s ease;
  min-height: 100px;
  max-height: calc(100dvh - 4rem - 200px);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.terminal-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.connection-status {
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 4px;
}

.connection-status.connected {
  color: #10b981;
  background: #10b98120;
}

.connection-status.disconnected {
  color: #ef4444;
  background: #ef444420;
}

.context-menu {
  min-width: 150px;
}
</style>
