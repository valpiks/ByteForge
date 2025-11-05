<template>
  <SimpleModal :modal-value="modalValue" @update:model-value="onUpdateModelValue">
    <div class="flex flex-col gap-4">
      <div>
        <h1 class="semi-bold text-xl">Export project</h1>
        <p class="text-muted-foreground">Export your project files as an archive</p>
      </div>

      <div class="flex flex-col gap-3">
        <div class="flex flex-col gap-2">
          <p>Format</p>
          <div class="grid grid-cols-2 gap-2">
            <label class="flex cursor-pointer">
              <input type="radio" v-model="format" value="ZIP" class="hidden" />
              <div
                class="flex flex-col items-center p-3 border-2 rounded-lg w-full transition-all"
                :class="format === 'ZIP' ? 'border-primary bg-primary/10' : 'border-border'"
              >
                <Icon icon="ph:file-zip" class="w-6 h-6 mb-1" />
                <span class="font-medium">ZIP</span>
                <span class="text-xs text-muted-foreground">Universal</span>
              </div>
            </label>
            <label class="flex cursor-pointer">
              <input type="radio" v-model="format" value="RAR" class="hidden" />
              <div
                class="flex flex-col items-center p-3 border-2 rounded-lg w-full transition-all"
                :class="format === 'RAR' ? 'border-primary bg-primary/10' : 'border-border'"
              >
                <Icon icon="mdi:archive" class="w-6 h-6 mb-1" />
                <span class="font-medium">RAR</span>
                <span class="text-xs text-muted-foreground">Compressed</span>
              </div>
            </label>
          </div>
        </div>
      </div>

      <div v-if="isExporting" class="p-3 bg-muted rounded-lg">
        <div class="flex justify-between mb-2">
          <span class="text-sm">Exporting project...</span>
          <span class="text-sm font-medium">{{ exportStatus?.progress }}%</span>
        </div>
        <div class="w-full bg-background rounded-full h-2">
          <div
            class="bg-primary h-2 rounded-full transition-all duration-300"
            :style="{ width: exportStatus?.progress + '%' }"
          ></div>
        </div>
        <p class="text-xs text-muted-foreground mt-2">{{ exportStatus?.message }}</p>
      </div>

      <div v-if="error" class="p-3 bg-destructive/10 border border-destructive rounded-lg">
        <div class="flex items-center gap-2 text-destructive">
          <Icon icon="mdi:alert-circle" class="w-4 h-4" />
          <span class="text-sm">{{ error }}</span>
        </div>
      </div>

      <div class="flex gap-2 justify-end">
        <button
          @click="onUpdateModelValue(false)"
          class="px-4 py-2 border border-border rounded-lg hover:bg-muted transition-colors"
          :disabled="isExporting"
        >
          Cancel
        </button>
        <button
          @click="handleExport"
          class="px-4 py-2 bg-primary text-primary-foreground rounded-lg hover:bg-primary/90 transition-colors"
          :disabled="isExporting"
        >
          {{ isExporting ? 'Exporting...' : 'Export Project' }}
        </button>
      </div>
    </div>
  </SimpleModal>
</template>

<script setup lang="ts">
import { useProjectExport } from '@/app/shared/composables/useProjectExport'
import { Icon } from '@iconify/vue'
import { ref, watch } from 'vue'
import SimpleModal from '../UI/modal/SimpleModal.vue'

const props = defineProps<{
  modalValue: boolean
  currentProjectId: number
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const format = ref<'ZIP' | 'RAR'>('ZIP')
const includeGit = ref(false)

const { isExporting, exportStatus, error, startExport, cancelExport } = useProjectExport()

const handleExport = async () => {
  await startExport(props.currentProjectId, {
    format: format.value,
    includeGit: includeGit.value,
  })
}

const onUpdateModelValue = (value: boolean) => {
  emit('update:modelValue', value)
}

watch(exportStatus, (status) => {
  if (status?.status === 'COMPLETED') {
    setTimeout(() => onUpdateModelValue(false), 1000)
  }
})

watch(
  () => props.modalValue,
  (show) => {
    if (!show) {
      cancelExport()
    }
  },
)
</script>
