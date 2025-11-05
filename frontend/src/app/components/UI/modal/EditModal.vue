<template>
  <SimpleModal :modal-value="modalValue" @update:model-value="onUpdateModelValue">
    <div class="w-full max-w-lg gap-4 border bg-background p-6 shadow-lg sm:rounded-lg">
      <slot />
    </div>
  </SimpleModal>
</template>

<script setup lang="ts">
import SimpleModal from './SimpleModal.vue'

defineProps<{
  modalValue: boolean
}>()

const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>()

const onUpdateModelValue = (value: boolean) => {
  emit('update:modelValue', value)
}
</script>
