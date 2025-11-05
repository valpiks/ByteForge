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

const loginFormData = ref({
  email: '',
  password: '',
})

const handleSubmit = async () => {
  const result = await userStore.login(loginFormData.value)
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
      <CustomLabel htmlFor="email"> Email </CustomLabel>
      <CustomInput
        id="email"
        name="email"
        placeholder="example@gmail.com"
        type="email"
        v-model="loginFormData.email"
        required
      />
    </div>
    <div class="flex flex-col gap-3">
      <CustomLabel htmlFor="password"> Password </CustomLabel>
      <CustomInput
        id="password"
        name="password"
        placeholder="••••••••"
        type="password"
        v-model="loginFormData.password"
        required
      />
    </div>
    <SimpleButton> Sign In </SimpleButton>
  </form>
</template>
