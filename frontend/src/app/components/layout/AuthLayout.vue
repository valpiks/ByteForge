<script setup lang="ts">
import SimpleNavBar from '../UI/navbars/SimpleNavBar.vue'
</script>

<template>
  <div class="min-h-screen bg-background">
    <SimpleNavBar />
    <main>
      <slot />
    </main>
  </div>
</template>
