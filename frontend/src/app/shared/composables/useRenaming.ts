import websocketService from '@/app/shared/api/websocket-service'
import { nextTick, ref, type Ref } from 'vue'

export function useRenaming() {
  const isRenaming = ref(false)
  const renameValue = ref('')

  const startRenaming = (currentName: string, inputRef?: Ref<HTMLInputElement | null>) => {
    isRenaming.value = true
    renameValue.value = currentName

    nextTick(() => {
      inputRef?.value?.focus()
    })
  }

  const cancelRenaming = () => {
    isRenaming.value = false
    renameValue.value = ''
  }

  const handleRename = (fileId: number, currentName: string) => {
    if (currentName === renameValue.value || !renameValue.value.trim()) {
      cancelRenaming()
      return
    }

    websocketService.renameFile(renameValue.value, fileId)
    cancelRenaming()
  }

  return {
    isRenaming,
    renameValue,
    startRenaming,
    cancelRenaming,
    handleRename,
  }
}
