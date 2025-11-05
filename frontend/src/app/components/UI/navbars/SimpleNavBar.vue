<script setup lang="ts">
import { useThemeStore } from '@/stores/theme'
import { Icon } from '@iconify/vue'
import SimpleButton from '../buttons/SimpleButton.vue'

const themeStore = useThemeStore()
</script>

<template>
  <nav class="border-b bg-card/50 backdrop-blur-sm sticky top-0 z-50">
    <div class="container mx-auto px-4 h-16 flex items-center justify-between">
      <router-link to="/" class="flex items-center gap-2 font-bold text-xl">
        <div class="h-8 w-8 rounded-lg bg-gradient-primary flex items-center justify-center">
          <Icon icon="material-symbols:frame-source" />
        </div>
      </router-link>
      <SimpleButton variant="ghost" size="icon" @click="themeStore.toggleTheme">
        <Icon v-if="themeStore.theme === 'dark'" icon="material-symbols:sunny-outline-rounded" />
        <Icon v-else icon="tabler:moon" />
      </SimpleButton>
    </div>
  </nav>
</template>
