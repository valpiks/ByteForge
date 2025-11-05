<template>
  <SimpleModal :modal-value="modalValue" @update:model-value="onUpdateModelValue">
    <div>
      <div>
        <h1 class="semi-bold text-xl">Project Settings</h1>
        <p class="text-muted-foreground">Manage your project configuration and preferences</p>
      </div>

      <div class="flex gap-1 p-1 bg-secondary rounded-md mt-4">
        <div
          :key="btn"
          v-for="btn in menuButtons"
          class="cursor-pointer py-1 flex-1 text-center rounded-md transition-all duration-200"
          :class="[
            currentActiveMode === btn ? 'bg-background text-white' : 'text-muted-foreground',
          ]"
          @click="toggleMode(btn)"
        >
          {{ btn }}
        </div>
      </div>

      <div class="mt-6">
        <!-- project -->
        <div v-if="currentActiveMode === 'General'">
          <div class="flex flex-col gap-6">
            <ProjectForm
              :project="newProjectData"
              @update="handleUpdate"
              @submit="handleSubmit"
              @cancel="cancelEdit"
              submit-btn="Edit"
              :need-title="false"
              :adittional-button-classes="['justify-between mt-0']"
            />
          </div>
        </div>

        <!-- editor -->
        <div v-if="currentActiveMode === 'Editor'" class="flex flex-col gap-4">
          <div class="flex flex-col gap-2">
            <h2 class="font-semibold">Font Size</h2>
            <CustomSelect
              :options="fontOptions"
              :model-value="settings.fontSize"
              @update:model-value="handleFontSizeChange"
            />
          </div>
          <div class="flex flex-col gap-2">
            <h2 class="font-semibold">Tab Size</h2>
            <CustomSelect
              :options="tabsOptions"
              :model-value="settings.tabSize"
              @update:model-value="handleTabSizeChange"
            />
          </div>
          <div class="flex justify-between items-center">
            <div>
              <h2>Line numbers</h2>
              <p class="text-muted-foreground">Show line numbers in editor</p>
            </div>
            <SliderButton
              :toggle-value="settings.lineNumbers"
              @update:toggle-value="handleLineNumbersClick"
            />
          </div>
          <div class="flex justify-between items-center">
            <div>
              <h2>Auto save</h2>
              <p class="text-muted-foreground">Automatically save files on change</p>
            </div>
            <SliderButton
              :toggle-value="settings.autoSave"
              @update:toggle-value="handleAutoSaveClick"
            />
          </div>
        </div>
      </div>
    </div>
  </SimpleModal>
</template>

<script setup lang="ts">
import { useProjectStore } from '@/stores/project'
import { onMounted, ref, watch } from 'vue'
import SliderButton from '../UI/buttons/SliderButton.vue'
import CustomSelect from '../UI/CustomSelect.vue'
import ProjectForm from '../UI/forms/ProjectForm.vue'
import SimpleModal from '../UI/modal/SimpleModal.vue'

const menuButtons = ['General', 'Editor'] //build contr

const settings = ref(
  JSON.parse(localStorage.getItem('projectSettings')) || {
    autoSave: true,
    fontSize: '14px',
    tabSize: 2,
    lineNumbers: true,
  },
)

const newProjectData = ref({
  title: '',
  description: '',
})

const handleUpdate = (field: string, value: string) => {
  newProjectData.value[field] = value
}

const cancelEdit = () => {
  newProjectData.value = {
    title: projectStore.currentProject?.title || '',
    description: projectStore.currentProject?.description || '',
  }
}

const fontOptions = [
  { name: '12px', value: '12px' },
  { name: '14px', value: '14px' },
  { name: '16px', value: '16px' },
  { name: '18px', value: '18px' },
]

const tabsOptions = [
  { name: '2 spaces', value: 2 },
  { name: '4 spaces', value: 4 },
  { name: '8 spaces', value: 8 },
]

const handleFontSizeChange = (value: string) => {
  settings.value.fontSize = value
}

const handleTabSizeChange = (value: number) => {
  settings.value.tabSize = value
}

const currentActiveMode = ref('General')
const projectStore = useProjectStore()

const toggleMode = (value: string) => {
  currentActiveMode.value = value
}

const handleAutoSaveClick = () => {
  settings.value.autoSave = !settings.value.autoSave
}

const handleLineNumbersClick = () => {
  settings.value.lineNumbers = !settings.value.lineNumbers
}

const handleSubmit = async () => {
  if (projectStore.currentProject?.id) {
    await projectStore.editProject(newProjectData.value, projectStore.currentProject.id)
  }
}

defineProps<{
  modalValue: boolean
  isOwner: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const onUpdateModelValue = (value: boolean) => {
  emit('update:modelValue', value)
}

watch(
  () => projectStore.currentProject,
  (newVal) => {
    if (newVal) {
      newProjectData.value = {
        title: newVal.title,
        description: newVal.description || '',
      }
    }
  },
  { immediate: true },
)

watch(
  () => settings.value.fontSize,
  (newSize) => {
    const editor = document.querySelector('.editor-content textarea')
    if (editor && newSize) {
      editor.style.fontSize = newSize
    }
  },
)

watch(
  () => settings.value.lineNumbers,
  (showLineNumbers) => {
    const lineNumbers = document.querySelector('.line-numbers')
    if (lineNumbers) {
      lineNumbers.classList.toggle('hidden', !showLineNumbers)
    }
  },
)

watch(
  settings,
  (newSettings) => {
    localStorage.setItem('projectSettings', JSON.stringify(newSettings))
  },
  { deep: true },
)

onMounted(() => {
  if (projectStore.currentProject) {
    newProjectData.value = {
      title: projectStore.currentProject.title,
      description: projectStore.currentProject.description || '',
    }
  }
})
</script>
