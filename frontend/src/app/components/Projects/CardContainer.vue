<template>
  <div class="grid grid-cols-3 gap-6">
    <div
      @click="$router.push(`/projects/${project.id}`)"
      v-for="project in projectStore.projectList"
      :key="project.id"
      class="py-6 px-6 bg-card rounded-lg border border-border hover:shadow-elegant transition-smooth"
    >
      <div class="mb-4 flex items-center justify-between">
        <div class="h-10 w-10 rounded-lg bg-primary/10 flex items-center justify-center">
          <Icon icon="tabler:folder-code" class="h-5 w-5 text-primary" />
        </div>
        <div class="flex gap-1">
          <button
            @click.stop="openEditModal(project)"
            type="button"
            class="py-2 px-2 hover:bg-primary rounded-md transition-smooth"
          >
            <Icon icon="tabler:pencil" class="h-5 w-5 text-white" />
          </button>
          <button
            @click.stop="openConfirmModal(project)"
            type="button"
            class="py-2 px-2 hover:bg-primary text-red-500 hover:text-white rounded-md transition-smooth"
          >
            <Icon icon="tabler:trash" class="h-5 w-5" />
          </button>
        </div>
      </div>
      <div class="mb-4 break-words">
        <h1 class="text-2xl font-semibold hover:text-primary transition-smooth cursor-pointer">
          {{ project.title }}
        </h1>
        <p class="text-muted-foreground">{{ project.description }}</p>
      </div>
      <div class="flex items-center gap-4 text-sm text-muted-foreground">
        <div class="flex items-center gap-1">
          <Icon icon="tabler:file-code" class="h-5 w-5" />
          <span>{{ project.fileCount }} files</span>
        </div>
        <div class="flex items-center gap-1">
          <Icon icon="tabler:calendar-code" class="h-5 w-5" />
          <span>{{ getUpdatedTimeAgo(project.updatedAt) }} </span>
        </div>
      </div>
      <SimpleButton variant="outline" class="w-full mt-4">Open Project</SimpleButton>
    </div>

    <SimpleModal :need-x="true" :modal-value="isEditOpen" @update:model-value="setIsEditOpen">
      <ProjectForm
        :project="newProjectData"
        @update="handleUpdate"
        @submit="handleSubmitEditProject"
        @cancel="setIsEditOpen(false)"
        title="Edit Project"
        description="Update your project details."
        submit-btn="Save Changes"
      />
    </SimpleModal>
    <SimpleModal :modal-value="isConfirmOpen" @update:model-value="setIsConfirmOpen">
      <div>
        <h1 class="font-semibold text-xl mb-2">Are you sure?</h1>
        <p class="text-muted-foreground text-sm mb-6">
          This action cannot be undone. This will permanently delete the project "{{
            selectedProject?.title
          }}" and all of its files.
        </p>
        <form @submit.prevent="handleSubmitDeleteProject" class="mt-4 flex gap-2 justify-end">
          <SimpleButton @click="setIsConfirmOpen(false)" type="button" variant="outline"
            >Cancel</SimpleButton
          >
          <SimpleButton variant="destructive" type="submit">Delete</SimpleButton>
        </form>
      </div>
    </SimpleModal>
  </div>
</template>

<script setup lang="ts">
import { getUpdatedTimeAgo } from '@/app/shared/utils/Formatters'
import { useProjectStore } from '@/stores/project'
import { Icon } from '@iconify/vue'
import { ref } from 'vue'
import type { Project } from '../types/Profile'
import SimpleButton from '../UI/buttons/SimpleButton.vue'
import ProjectForm from '../UI/forms/ProjectForm.vue'
import SimpleModal from '../UI/modal/SimpleModal.vue'

const projectStore = useProjectStore()

const isEditOpen = ref<boolean>(false)
const isConfirmOpen = ref<boolean>(false)
const selectedProject = ref<Project | null>(null)
const newProjectData = ref({
  title: '',
  description: '',
})

const handleUpdate = (field: string, value: string) => {
  newProjectData.value[field] = value
}

const handleSubmitEditProject = async () => {
  await projectStore.editProject(newProjectData.value, selectedProject.value?.id as number)
  setIsEditOpen(false)
}

const handleSubmitDeleteProject = async () => {
  await projectStore.deleteProject(selectedProject.value?.id as number)
  setIsConfirmOpen(false)
}

const openEditModal = (project: Project) => {
  setIsEditOpen(true)
  newProjectData.value.title = project.title
  newProjectData.value.description = project?.description || ''
  selectedProject.value = project
}

const openConfirmModal = (project: Project) => {
  setIsConfirmOpen(true)
  selectedProject.value = project
}

const setIsEditOpen = (value: boolean) => {
  isEditOpen.value = value
}

const setIsConfirmOpen = (value: boolean) => {
  isConfirmOpen.value = value
}
</script>
