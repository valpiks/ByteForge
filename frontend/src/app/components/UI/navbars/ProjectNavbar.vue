<template>
  <nav class="border-b bg-card backdrop-blur-sm sticky top-0 z-50">
    <div class="mx-auto px-8 h-16 flex items-center justify-between">
      <div class="flex items-center gap-4">
        <router-link to="/" class="flex items-center gap-2 font-bold text-lg">
          <Icon icon="material-symbols:frame-source" />
          <span>ByteForge</span>
        </router-link>
        <div class="h-6 w-px bg-border" />
        <span class="w-52 overflow-hidden font-semibold">{{
          projectStore.currentProject.title
        }}</span>
      </div>
      <div class="flex items-center gap-1">
        <div class="flex items-center -space-x-2 mr-2">
          <div
            :key="cont.id"
            v-for="(cont, i) in projectStore.contributors"
            @click="toggleUserModal(cont)"
          >
            <img
              v-if="!cont.imageError"
              :src="getUserImageUrl(cont)"
              :alt="cont.username"
              @error="handleImageError(cont)"
              class="relative border-2 cursor-pointer hover:z-20 rounded-full transition-transform w-10 h-10 hover:scale-110 duration-200"
              :class="[cont?.online ? 'border-green-400' : 'border-rose-600', `z-[${i}]`]"
            />
            <div
              v-else
              class="relative cursor-pointer hover:z-20 rounded-full transition-transform hover:scale-110 duration-200 text-xl font-semibold bg-gradient-primary border-2 border-border leading-tight h-10 w-10 flex items-center justify-center bg-card"
              :class="[cont?.online ? 'border-green-400' : 'border-rose-600', `z-[${i}]`]"
            >
              {{ cont.username?.charAt(0) }}
            </div>
          </div>
        </div>
        <SimpleButton @click="toggleExportModal" variant="ghost" size="icon">
          <Icon icon="lucide:download" class="w-5 h-5" />
        </SimpleButton>
        <SimpleButton @click="saveFile" variant="ghost" v-if="currentUserRole !== 'VIEWER'">
          <Icon icon="lucide-lab:save" class="w-5 h-5" />
          Save
        </SimpleButton>
        <SimpleButton
          @click="toggleLinkModal"
          variant="ghost"
          size="icon"
          v-if="currentUserRole === 'OWNER'"
        >
          <Icon icon="tabler:share" class="w-5 h-5" />
        </SimpleButton>
        <SimpleButton variant="ghost" size="icon" @click="openSettings">
          <Icon icon="tabler:settings" class="w-5 h-5" />
        </SimpleButton>
        <SimpleButton variant="ghost" size="icon" @click="themeStore.toggleTheme">
          <Icon v-if="themeStore.theme === 'dark'" icon="material-symbols:sunny-outline-rounded" />
          <Icon v-else icon="tabler:moon" />
        </SimpleButton>
        <SimpleButton @click="goToProjects" variant="ghost" size="icon">
          <Icon icon="tabler:home" class="w-5 h-5" />
        </SimpleButton>
      </div>
    </div>

    <LinkInviteModal
      :current-project-id="currentProjectId"
      :modal-value="isLinkOpen"
      @update:model-value="toggleLinkModal"
    />

    <ExportProjectModal
      :current-project-id="currentProjectId"
      :modal-value="isExportOpen"
      @update:model-value="toggleExportModal"
    />

    <ContributorInfoModal
      :modal-value="!!selectedContributor"
      @update:model-value="closeUserModal"
      :contributor="selectedContributor"
      :is-owner="isOwner"
      :current-project-id="currentProjectId"
    />

    <ProjectSettingsModal
      :modal-value="isSettingsOpen"
      @update:model-value="toggleSettingsModal"
      :is-owner="isOwner"
    />
  </nav>
</template>

<script setup lang="ts">
import { useProjectStore } from '@/stores/project'
import { useThemeStore } from '@/stores/theme'
import { useUserStore } from '@/stores/user'
import { Icon } from '@iconify/vue'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import ContributorInfoModal from '../../Editor/ContributorInfoModal.vue'
import ExportProjectModal from '../../Editor/ExportProjectModal.vue'
import LinkInviteModal from '../../Editor/LinkInviteModal.vue'
import ProjectSettingsModal from '../../Editor/ProjectSettingsModal.vue'
import SimpleButton from '../buttons/SimpleButton.vue'

const isLinkOpen = ref(false)
const isSettingsOpen = ref(false)
const isExportOpen = ref(false)
const selectedContributor = ref(null)

const themeStore = useThemeStore()
const projectStore = useProjectStore()
const userStore = useUserStore()

const router = useRouter()

const isOwner = computed(() =>
  projectStore.contributors.some((cont) => cont.id === userStore.user?.id && cont.role === 'OWNER'),
)

const toggleUserModal = (contributor: any) => {
  selectedContributor.value = contributor
}

const toggleSettingsModal = (value: boolean) => {
  isSettingsOpen.value = value
}

const closeUserModal = () => {
  selectedContributor.value = null
}

const toggleLinkModal = (value: boolean) => {
  isLinkOpen.value = value
}

const toggleExportModal = (value: boolean) => {
  isExportOpen.value = value
}

const handleImageError = (cont) => {
  cont.imageError = true
}

const getUserImageUrl = (user) => {
  return `http://localhost:8000/api/v1/profile-image/${user.id}`
}

defineProps<{
  currentProjectId: number
  currentUserRole: string
}>()

const emit = defineEmits<{
  saveFile: []
}>()

const saveFile = () => {
  emit('saveFile')
}

const openSettings = () => {
  isSettingsOpen.value = true
}

const goToProjects = () => {
  router.push('/projects')
}
</script>
