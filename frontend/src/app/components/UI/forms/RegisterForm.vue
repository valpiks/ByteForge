<script setup lang="ts">
import { useProjectStore } from '@/stores/project'
import { useUserStore } from '@/stores/user'
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import CustomInput from '../CustomInput.vue'
import CustomLabel from '../CustomLabel.vue'
import SimpleButton from '../buttons/SimpleButton.vue'

const router = useRouter()
const userStore = useUserStore()
const projectStore = useProjectStore()

const registerFormData = ref({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
})

const handleSubmit = async () => {
  const result = await userStore.register(registerFormData.value)
  if (result) {
    const joinToken = localStorage.getItem('joinToken')
    const splited = joinToken?.split('&')
    const token = splited?.[0].split('token=')[1]
    const role = splited?.[1].split('role=')[1]
    if (joinToken) {
      const joinProjectId = await projectStore.sendToken(token, role)
      if (joinProjectId) {
        router.push(`/projects/${joinProjectId}`)
      } else {
        router.push('/not-found')
      }
    } else {
      router.push('/profile')
    }
  }
}
</script>

<template>
  <form @submit.prevent="handleSubmit" class="flex flex-col gap-6">
    <div class="flex flex-col gap-3">
      <CustomLabel htmlFor="name"> Name </CustomLabel>
      <CustomInput
        v-model="registerFormData.username"
        id="name"
        name="name"
        placeholder="Ivan"
        type="text"
        required
      />
    </div>
    <div class="flex flex-col gap-3">
      <CustomLabel htmlFor="email"> Email </CustomLabel>
      <CustomInput
        v-model="registerFormData.email"
        id="email"
        name="email"
        placeholder="example@gmail.com"
        type="email"
        required
      />
    </div>
    <div class="flex flex-col gap-3">
      <CustomLabel htmlFor="password"> Password </CustomLabel>
      <CustomInput
        v-model="registerFormData.password"
        id="password"
        name="password"
        placeholder="••••••••"
        type="password"
        required
      />
    </div>
    <div class="flex flex-col gap-3">
      <CustomLabel htmlFor="confirmPassword"> Confirm Password </CustomLabel>
      <CustomInput
        v-model="registerFormData.confirmPassword"
        id="confirmPassword"
        name="confirmPassword"
        placeholder="••••••••"
        type="password"
        required
      />
    </div>
    <SimpleButton> Sign Up </SimpleButton>
  </form>
</template>
