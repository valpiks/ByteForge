import websocketService from '@/app/shared/api/websocket-service'
import { nextTick, ref, type Ref } from 'vue'

export function useFileOperations() {
  const isCreating = ref(false)
  const creatingType = ref<'FILE' | 'FOLDER'>('FILE')
  const creatingLocation = ref<number | null>(null)
  const newItemName = ref('')

  const startCreating = (
    type: 'FILE' | 'FOLDER',
    folderId: number | null,
    inputRef?: Ref<HTMLInputElement | null>,
  ) => {
    isCreating.value = true
    creatingType.value = type
    creatingLocation.value = folderId
    newItemName.value = ''

    nextTick(() => {
      if (folderId === null && inputRef?.value) {
        inputRef.value.focus()
      }
    })
  }

  const cancelCreating = () => {
    isCreating.value = false
    creatingType.value = 'FILE'
    creatingLocation.value = null
    newItemName.value = ''
  }

  const createItem = (generatePathFn: (name: string, parentId: number | null) => string) => {
    if (typeof generatePathFn !== 'function') {
      cancelCreating()
      return
    }

    const name = newItemName.value.trim()
    if (!name) {
      cancelCreating()
      return
    }

    try {
      let finalName = name
      if (creatingType.value === 'FILE' && !name.includes('.')) {
        finalName += '.cpp'
      }

      const path = generatePathFn(finalName, creatingLocation.value)

      if (!path || typeof path !== 'string') {
        console.error('Invalid path generated:', path)
        throw new Error('Generated path is invalid')
      }

      websocketService.createFile(finalName, path, creatingType.value, creatingLocation.value)
    } catch (error) {
      console.error('Error creating item:', error)
    } finally {
      cancelCreating()
    }
  }

  return {
    isCreating,
    creatingType,
    creatingLocation,
    newItemName,
    startCreating,
    cancelCreating,
    createItem,
  }
}
