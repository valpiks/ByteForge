<script setup lang="ts">
import { computed, defineAsyncComponent, onMounted, type Component } from 'vue'
import { useRoute } from 'vue-router'
import { useThemeStore } from './stores/theme'

const route = useRoute()

const themeStore = useThemeStore()

const layoutName = computed(() => {
  return (route.meta.layout as string) || null
})

const layoutComponent = computed(() => {
  const layoutMap: Record<string, Component> = {
    AppLayout: defineAsyncComponent(() => import('@/app/components/layout/AppLayout.vue')),
    AuthLayout: defineAsyncComponent(() => import('@/app/components/layout/AuthLayout.vue')),
  }

  return layoutMap[layoutName.value] || null
})

onMounted(() => {
  themeStore.initializeTheme()
})
</script>

<template>
  <component :is="layoutComponent" v-if="layoutComponent">
    <router-view />
  </component>
  <router-view v-else />
</template>

<style>
#root {
  max-width: 1280px;
  margin: 0 auto;
  padding: 2rem;
  text-align: center;
}

.logo {
  height: 6em;
  padding: 1.5em;
  will-change: filter;
  transition: filter 300ms;
}
.logo:hover {
  filter: drop-shadow(0 0 2em #646cffaa);
}
.logo.react:hover {
  filter: drop-shadow(0 0 2em #61dafbaa);
}

@keyframes logo-spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@media (prefers-reduced-motion: no-preference) {
  a:nth-of-type(2) .logo {
    animation: logo-spin infinite 20s linear;
  }
}

.card {
  padding: 2em;
}

.read-the-docs {
  color: #888;
}
</style>
