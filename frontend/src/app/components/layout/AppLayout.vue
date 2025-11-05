<script setup lang="ts">
import NavBar from '../UI/navbars/NavBar.vue'
</script>

<template>
  <div class="min-h-screen bg-background">
    <NavBar />
    <main>
      <slot />
    </main>
  </div>
</template>
