<template>
  <div class="tree-node">
    <div
      @contextmenu.prevent="handleEditorRightClick($event, node)"
      class="node-header group"
      :class="{ 'is-folder': node.type === 'FOLDER' }"
      @click="handleClick"
    >
      <div v-if="!isRenaming" class="flex items-center gap-2 flex-1">
        <Icon
          :icon="
            node.type === 'FOLDER'
              ? isOpen
                ? 'tabler:folder-open'
                : 'tabler:folder'
              : 'tabler:file-code'
          "
          class="w-5 h-5 transition-all duration-300"
          :class="[
            node.type === 'FOLDER'
              ? isOpen
                ? 'text-primary'
                : 'text-muted-foreground group-hover:text-foreground'
              : 'text-blue-400',
          ]"
        />

        <span class="node-name">{{ node.name }}</span>
      </div>
      <input
        v-else-if="isRenaming"
        ref="renameInput"
        v-model="renameValue"
        @keydown.enter="handleRenameConfirm"
        @keydown.escape="cancelRenaming"
        @blur="handleRenameConfirm"
        @keydown.stop
        @click.stop
        type="text"
        class="w-full px-2 py-1 text-sm bg-editor-bg border border-primary rounded focus:outline-none focus:ring-1 focus:ring-primary"
        :placeholder="node.type === 'FILE' ? 'File name with extension...' : 'Folder name...'"
      />
    </div>

    <div v-if="node.type === 'FOLDER' && isOpen" class="node-children">
      <FileTree
        v-for="child in node.children"
        :key="child.id"
        :node="child"
        :open-folders="openFolders"
        :is-creating="isCreating"
        :creating-type="creatingType"
        :creating-location="creatingLocation"
        :active-context-menu="activeContextMenu"
        @delete-file="(id) => $emit('delete-file', id)"
        @toggle-folder="(id) => $emit('toggle-folder', id)"
        @select-file="(file) => $emit('select-file', file)"
        @create-item="(type, folderId) => $emit('create-item', type, folderId)"
        @create-file="(name, type, folderId) => $emit('create-file', name, type, folderId)"
        @cancel-create="$emit('cancel-create')"
        @show-context-menu="(nodeId, x, y) => $emit('show-context-menu', nodeId, x, y)"
        @start-renaming="(fileId) => $emit('start-renaming', fileId)"
      />

      <div v-if="isCreatingInThisFolder" class="px-3 py-1" ref="folderInputContainer">
        <input
          ref="folderInput"
          v-model="newItemName"
          @keydown.enter="createItemInFolder"
          @keydown.escape="cancelCreatingInFolder"
          @keydown.stop
          @click.stop
          type="text"
          class="w-full px-2 py-1 text-sm bg-editor-bg border border-primary rounded focus:outline-none focus:ring-1 focus:ring-primary"
          :placeholder="
            creatingTypeInFolder === 'FILE' ? 'File name with extension...' : 'Folder name...'
          "
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRenaming } from '@/app/shared/composables/useRenaming'
import { Icon } from '@iconify/vue'
import { computed, nextTick, ref, watch } from 'vue'

interface FileNode {
  id: number
  name: string
  path: string
  type: 'FILE' | 'FOLDER'
  hasUnsavedChanges: boolean
  content?: string
  parentId?: number
  children?: FileNode[]
}

interface Props {
  node: FileNode
  openFolders: Set<number>
  isCreating: boolean
  creatingType: 'FILE' | 'FOLDER'
  creatingLocation: number | null
  activeContextMenu: { nodeId: number; x: number; y: number } | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'toggle-folder': [id: number]
  'select-file': [file: FileNode]
  'create-item': [type: 'FILE' | 'FOLDER', folderId: number]
  'create-file': [name: string, type: 'FILE' | 'FOLDER', folderId: number]
  'cancel-create': []
  'show-context-menu': [nodeId: number, x: number, y: number]
  'delete-file': [id: number]
  'start-renaming': [fileId: number]
}>()

const { isRenaming, renameValue, startRenaming, cancelRenaming, handleRename } = useRenaming()

const newItemName = ref('')
const folderInput = ref<HTMLInputElement>()
const folderInputContainer = ref<HTMLDivElement>()
const renameInput = ref<HTMLInputElement>()

const isOpen = computed(() => props.openFolders.has(props.node.id))
const isCreatingInThisFolder = computed(
  () => props.isCreating && props.creatingLocation === props.node.id,
)
const creatingTypeInFolder = computed(() => props.creatingType)

watch(isCreatingInThisFolder, (newVal) => {
  if (newVal) {
    newItemName.value = ''
    nextTick(() => {
      folderInput.value?.focus()
    })
  }
})

const handleEditorRightClick = (event: MouseEvent, node: FileNode) => {
  event.preventDefault()
  event.stopPropagation()
  emit('show-context-menu', node.id, event.clientX, event.clientY)
}

const handleClick = () => {
  if (props.activeContextMenu?.nodeId === props.node.id) {
    emit('show-context-menu', -1, 0, 0)
    return
  }

  if (props.node.type === 'FOLDER') {
    emit('toggle-folder', props.node.id)
  } else {
    emit('select-file', props.node)
  }
}

const handleRenameConfirm = () => {
  handleRename(props.node.id, props.node.name)
}

const createItemInFolder = (event: Event) => {
  event.stopPropagation()
  const name = newItemName.value.trim()
  if (name) {
    emit('create-file', name, creatingTypeInFolder.value, props.node.id)
  } else {
    emit('cancel-create')
  }
}

const cancelCreatingInFolder = (event: Event) => {
  event.stopPropagation()
  emit('cancel-create')
}

const startRenameForThisNode = () => {
  startRenaming(props.node.name, renameInput)
}

defineExpose({
  startRenameForThisNode,
})
</script>

<style scoped>
.tree-node {
  user-select: none;
  position: relative;
}

.node-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.25rem 1rem;
  transition: var(--transition-smooth);
  cursor: pointer;
}

.node-header:hover {
  background-color: hsl(var(--muted) / 0.5);
}

.node-header.is-folder {
  font-weight: 500;
}

.node-name {
  font-size: 0.875rem;
}

.node-children {
  margin-left: 1.5rem;
  border-left: 1px solid hsl(var(--editor-border));
}
</style>
