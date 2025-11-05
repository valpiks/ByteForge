<template>
  <main class="container mx-auto px-4 py-8">
    <div class="max-w-8xl mx-auto">
      <div class="flex justify-between items-center mb-8">
        <div>
          <h1 class="text-3xl font-semibold mb-2">My Projects</h1>
          <p class="text-muted-foreground">Manage and organize your C++ projects</p>
        </div>
        <SimpleButton @click="setIsCreateOpen(true)" class="shadow-elegant">
          <Icon icon="tabler:plus" class="w-4 h-4 font-semibold" />
          New Project
        </SimpleButton>
      </div>

      <!-- Projects container -->
      <CardContainer v-if="projectStore.projectList.length > 0" />

      <div v-else class="flex flex-col items-center justify-center py-16 px-4">
        <div className="relative mb-8">
          <div class="mx-auto flex flex-col items-center gap-6">
            <div class="absolute inset-0 gradient-primary opacity-20 blur-2xl rounded-full"></div>
            <div
              class="relative h-24 w-24 rounded-full bg-primary/10 flex items-center justify-center"
            >
              <Icon icon="mingcute:code-line" class="h-12 w-12 text-primary" />
            </div>
          </div>
        </div>
        <h1 class="text-2xl font-bold mb-2">No projects yet</h1>
        <p class="text-muted-foreground text-center max-w-md mb-6">
          Start your C++ journey by creating your first project. Build, run, and manage your code
          all in one place.
        </p>
      </div>
    </div>

    <SimpleModal :need-x="true" :modal-value="isCreateOpen" @update:model-value="setIsCreateOpen">
      <ProjectForm
        :project="newProjectData"
        @update="handleUpdate"
        @submit="handleSubmit"
        @cancel="setIsCreateOpen(false)"
      />
    </SimpleModal>
  </main>
</template>

<script setup lang="ts">
import { ProjectData, useProjectStore } from '@/stores/project'
import { Icon } from '@iconify/vue'
import { onMounted, ref } from 'vue'
import CardContainer from '../components/Projects/CardContainer.vue'
import SimpleButton from '../components/UI/buttons/SimpleButton.vue'
import ProjectForm from '../components/UI/forms/ProjectForm.vue'
import SimpleModal from '../components/UI/modal/SimpleModal.vue'

const projectStore = useProjectStore()

const isCreateOpen = ref(false)
const newProjectData = ref({
  title: '',
  description: '',
})

const handleUpdate = (field: string, value: string) => {
  newProjectData.value[field] = value
}

const handleSubmit = async (project: ProjectData) => {
  await projectStore.createProject(project)
  setIsCreateOpen(false)
}

const setIsCreateOpen = (value: boolean) => {
  isCreateOpen.value = value
}

onMounted(async () => {
  await projectStore.getListOfProjects()
})
</script>
