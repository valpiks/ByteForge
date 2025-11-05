<template>
  <nav class="border-b bg-card/50 backdrop-blur-sm sticky top-0 z-50">
    <div class="container mx-auto px-4 h-16 flex items-center justify-between">
      <router-link to="/" class="flex items-center gap-2 font-bold text-xl">
        <div class="h-8 w-8 rounded-lg bg-gradient-primary flex items-center justify-center">
          <Icon icon="material-symbols:frame-source" />
        </div>
      </router-link>
      <div class="flex items-center gap-6">
        <router-link
          v-if="userStore.isAuthenticated"
          to="/projects"
          class="text-sm font-medium transition-smooth hover:text-primary text-muted-foreground"
          active-class="text-primary"
        >
          Projects</router-link
        >
        <router-link
          v-if="userStore.isAuthenticated"
          to="/profile"
          class="text-sm font-medium transition-smooth hover:text-primary text-muted-foreground"
          active-class="text-primary"
          >Profile</router-link
        >
        <SimpleButton variant="ghost" size="icon" @click="themeStore.toggleTheme">
          <Icon v-if="themeStore.theme === 'dark'" icon="tabler:sun-high" />
          <Icon v-else icon="tabler:moon" />
        </SimpleButton>
        <SimpleButton v-if="userStore.isAuthenticated" @click="handleClick">Logout</SimpleButton>
        <router-link v-else to="/login">
          <SimpleButton variant="ghost" size="sm">
            <Icon icon="tabler:user" />
            Sign In
          </SimpleButton>
        </router-link>
      </div>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { useThemeStore } from '@/stores/theme'
import { useUserStore } from '@/stores/user'
import { Icon } from '@iconify/vue'
import { useRouter } from 'vue-router'
import SimpleButton from '../buttons/SimpleButton.vue'

const router = useRouter()

const userStore = useUserStore()
const themeStore = useThemeStore()

const handleClick = async () => {
  userStore.logout()
  router.push('/login')
}
</script>
