<template>
  <div
    @click="onUpdateToggleValue"
    class="rounded-xl h-7 w-[50px] relative cursor-pointer transition-all duration-300"
    :class="[toggleValue ? 'bg-primary' : 'bg-secondary']"
  >
    <div
      class="rounded-full w-6 h-6 bg-background absolute top-0.5 transition-all duration-300 ease-in-out"
      :style="{ transform: toggleValue ? 'translateX(24px)' : 'translateX(2px)' }"
    />
  </div>
</template>

<script setup lang="ts">
defineProps<{
  toggleValue: boolean
}>()

const emit = defineEmits<{
  'update:toggleValue': []
}>()

const onUpdateToggleValue = () => {
  emit('update:toggleValue')
}
</script>

<style scoped></style>
