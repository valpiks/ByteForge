<template>
  <teleport to="body">
    <div
      v-if="modalValue"
      @click.self="$emit('update:modelValue', false)"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/70"
    >
      <div class="w-full relative max-w-lg gap-4 border bg-background p-6 shadow-lg sm:rounded-lg">
        <slot />
        <button v-if="needX" @click="$emit('update:modelValue', false)">
          <Icon
            icon="tabler:x"
            class="absolute text-muted-foreground top-3 right-3 w-5 h-5 hover:text-red-500 cursor-pointer"
          />
        </button>
      </div>
    </div>
  </teleport>
</template>

<script setup lang="ts">
import { Icon } from '@iconify/vue'

defineProps<{
  modalValue: boolean
  needX?: boolean
}>()

defineEmits<{ 'update:modelValue': [value: boolean] }>()
</script>
