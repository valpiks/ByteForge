<template>
  <SimpleModal :modal-value="modalValue" @update:model-value="onUpdateModelValue">
    <div class="flex flex-col gap-2">
      <div>
        <h1 class="semi-bold text-xl">Project invite link</h1>
        <p class="text-muted-foreground">Share this link to invite someone to your project</p>
      </div>
      <div class="flex flex-col gap-1">
        <p>Select role</p>
        <CustomSelect
          :options="selectOptions"
          :model-value="currentInviteMode"
          @update:model-value="handleSelectChange"
        />
      </div>
      <div>
        <div class="flex flex-col gap-1">
          <p>Invite link</p>
          <div class="flex gap-2">
            <CustomInput id="link" name="link" type="text" v-model="currentInviteLink" readonly />
            <button
              @click="handleCopyLink"
              class="py-1 px-2 border rounded-sm border-border hover:text-primary transition-all duration-300"
            >
              <Icon icon="tabler:copy" class="w-5 h-5" />
            </button>
          </div>
        </div>
        <p class="text-muted-foreground mt-2 text-sm">Copy this link and send to new contributor</p>
      </div>
    </div>
  </SimpleModal>
</template>

<script setup lang="ts">
import { useProjectStore } from '@/stores/project'
import { Icon } from '@iconify/vue'
import { onMounted, ref } from 'vue'
import { useToast } from 'vue-toastification'
import CustomInput from '../UI/CustomInput.vue'
import CustomSelect from '../UI/CustomSelect.vue'
import SimpleModal from '../UI/modal/SimpleModal.vue'

const selectOptions = [
  { name: 'Developer', value: 'DEVELOPER' },
  { name: 'Viewer', value: 'VIEWER' },
]

const projectStore = useProjectStore()
const toast = useToast()

const linkToken = ref('')

const props = defineProps<{
  modalValue: boolean
  currentProjectId: number
}>()

const currentInviteLink = ref('')
const currentInviteMode = ref('DEVELOPER')

const handleCopyLink = async () => {
  await navigator.clipboard.writeText(currentInviteLink.value)
  toast.success('Ссылка успешно скопирована')
}

const handleSelectChange = (value: string) => {
  currentInviteMode.value = value
  currentInviteLink.value =
    window.location.origin + `/join-link?token=${linkToken.value}&role=${currentInviteMode.value}`
}

const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>()

const onUpdateModelValue = (value: boolean) => {
  emit('update:modelValue', value)
}

onMounted(async () => {
  linkToken.value = await projectStore.getJoinLink(props.currentProjectId)
  currentInviteLink.value =
    window.location.origin + `/join-link?token=${linkToken.value}&role=${currentInviteMode.value}`
})
</script>
