<template>
  <SimpleModal :modal-value="modalValue" @update:model-value="onUpdateModelValue">
    <div class="relative">
      <!-- user info -->
      <div v-if="currentMode === 'info'">
        <button v-if="isOwner" @click="toggleMode('settings')" class="absolute top-0 right-0">
          <Icon icon="tabler:settings" class="w-5 h-5" />
        </button>
        <h1 class="text-xl font-semibold mb-4">User Information</h1>
        <div>
          <div class="flex gap-4 items-center">
            <div>
              <img
                v-if="!contributor.imageError"
                :src="getUserImageUrl(contributor)"
                :alt="contributor.username"
                class="relative border-2 cursor-pointer rounded-full transition-transform w-24 h-24"
              />
              <div
                v-else
                class="relative cursor-pointer rounded-full text-3xl font-semibold bg-gradient-primary border-2 border-border leading-tight h-24 w-24 flex items-center justify-center bg-card"
              >
                {{ contributor.username?.charAt(0) }}
              </div>
            </div>
            <div>
              <h1 class="text-xl font-semibold">{{ contributor.username }}</h1>
              <p class="text-muted-foreground">{{ contributor.email }}</p>
              <div class="flex gap-2 items-center mt-1">
                <div
                  class="w-2 h-2 rounded-full"
                  :class="[contributor.online ? 'bg-green-500' : 'bg-rose-500']"
                ></div>
                <p class="text-muted-foreground">
                  {{ contributor.online ? 'Online' : 'Offline' }}
                </p>
              </div>
            </div>
          </div>
          <hr class="my-5" />
          <div class="flex justify-between items-center">
            <h2 class="text-lg font-semibold">Role</h2>
            <div class="lowercase first-letter:uppercase rounded-2xl bg-secondary py-1 px-3">
              {{ contributor.role }}
            </div>
          </div>
          <div class="mt-2 flex flex-col gap-2">
            <div v-for="group in permissions" :key="group.title">
              <h3>{{ group.title }}:</h3>
              <ul>
                <li
                  class="text-muted-foreground"
                  v-for="permission in group.permissions"
                  :key="permission"
                >
                  • {{ permission }}
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>
      <!-- cont settings -->
      <div v-else>
        <button @click="toggleMode('info')" class="flex items-center gap-2 mb-4 text-primary">
          <Icon icon="tabler:arrow-left" class="w-4 h-4" />
          Back
        </button>
        <h1 class="text-xl font-semibold mb-4">User Settings</h1>

        <div class="space-y-6">
          <div>
            <h3 class="text-lg font-medium mb-3">Change Role</h3>
            <CustomSelect
              :options="availableRoles"
              :model-value="props.contributor?.role"
              @update:model-value="handleContributorRoleChange"
            />
          </div>

          <div v-if="contributor.role === 'Owner'" class="border-t pt-6">
            <SimpleButton @click="kickContributor" variant="destructive" class="w-full">
              <Icon icon="tabler:user-off" class="w-4 h-4" />
              Remove from Project
            </SimpleButton>
          </div>
        </div>
      </div>
    </div>
  </SimpleModal>
</template>

<script setup lang="ts">
import websocketService from '@/app/shared/api/websocket-service'
import { getRolePermissions } from '@/app/shared/utils/projectUtil'
import { useProjectStore } from '@/stores/project'
import { Icon } from '@iconify/vue'
import { computed, ref } from 'vue'
import SimpleButton from '../UI/buttons/SimpleButton.vue'
import CustomSelect from '../UI/CustomSelect.vue'
import SimpleModal from '../UI/modal/SimpleModal.vue'

const props = defineProps<{
  modalValue: boolean
  contributor: any
  isOwner: boolean
  currentProjectId: number
}>()

const projectStore = useProjectStore()
const currentMode = ref('info')

const permissions = computed(() => getRolePermissions(props.contributor?.role))
const availableRoles = [
  { name: 'Owner', value: 'OWNER' },
  { name: 'Developer', value: 'DEVELOPER' },
  { name: 'Viewer', value: 'VIEWER' },
]

const getUserImageUrl = (user) => {
  return `http://localhost:8000/api/v1/profile-image/${user.id}`
}

const handleContributorRoleChange = (value: string) => {
  changeRole(value)
}

const toggleMode = (value: string) => {
  currentMode.value = value
}

const changeRole = async (value: string) => {
  await projectStore.changeContributorRole(props.currentProjectId, props.contributor.id, value)
}

const kickContributor = async () => {
  await projectStore.removeContributor(props.currentProjectId, props.contributor.id)
  websocketService.kickUser(props.contributor.id)
  onUpdateModelValue(false)
}

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const onUpdateModelValue = (value: boolean) => {
  emit('update:modelValue', value)
  currentMode.value = 'info'
}
</script>
