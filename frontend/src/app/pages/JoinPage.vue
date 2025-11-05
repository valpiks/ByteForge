<template>
  <div>link</div>
</template>

<script setup lang="ts">
import { useProjectStore } from '@/stores/project'
import { useUserStore } from '@/stores/user'
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'

const projectStore = useProjectStore()
const userStore = useUserStore()

const router = useRouter()

const token = window.location.href.split('?')?.[1]
const loading = ref(true)

watch(
  () => userStore.isAuthenticated,
  async (isAuthenticated) => {
    if (!token) {
      router.push('/notFound')
      return
    }
    await userStore.initializeAuth()

    const splited = token?.split('&')
    const inviteToken = splited?.[0].split('token=')[1]
    const role = splited?.[1].split('role=')[1]

    try {
      if (isAuthenticated) {
        const joinProjectId = await projectStore.sendToken(inviteToken, role)
        if (joinProjectId) {
          router.push(`/projects/${joinProjectId}`)
        } else {
          router.push('/notFound')
        }
      } else {
        localStorage.setItem('joinToken', token)
        router.push('/login')
      }
    } finally {
      loading.value = false
    }
  },
  { immediate: true },
)
</script>
