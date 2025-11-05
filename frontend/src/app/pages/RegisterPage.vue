<script setup lang="ts">
import { Icon } from '@iconify/vue'
import RegisterForm from '../components/UI/forms/RegisterForm.vue'
</script>

<template>
  <div class="min-h-screen gradient-hero flex items-center justify-center px-4">
    <div class="w-full max-w-md border rounded-lg py-8 px-6 shadow-glow bg-card">
      <div class="text-center flex flex-col gap-4 mb-8 items-center">
        <Icon icon="material-symbols:frame-source" class="w-8 h-8" />
        <h2 class="text-2xl font-semibold">Create Your Account</h2>
        <p class="text-sm text-muted-foreground">Start building C++ projects today</p>
      </div>
      <div class="flex flex-col gap-6">
        <RegisterForm />
        <p class="text-sm text-muted-foreground text-center">
          Already have an account?
          <router-link to="/login" class="text-primary hover:underline">Sign in</router-link>
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.min-h-screen {
  min-height: 10vh;
  height: 95vh;
  overflow: hidden;
}
</style>
