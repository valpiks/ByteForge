<template>
  <div class="relative" ref="dropdownRef">
    <div
      class="flex items-center justify-between p-3 border rounded-md cursor-pointer bg-background transition-all duration-200 ease-out"
      :class="[
        isOpen
          ? 'border-primary ring-2 ring-primary/20 shadow-sm'
          : 'border-border hover:border-primary/40',
      ]"
      @click="isOpen = !isOpen"
      tabindex="0"
      @keydown.space.enter="handleKeydown"
      @blur="handleBlur"
    >
      <span class="text-sm font-medium text-foreground">
        {{ selectedOption?.name || placeholder }}
      </span>
      <span
        class="transform transition-transform duration-300 ease-out text-muted-foreground"
        :class="{ 'rotate-180': isOpen }"
      >
        <Icon icon="tabler:chevron-down" class="w-4 h-4" />
      </span>
    </div>

    <transition
      enter-active-class="transition-all duration-200 ease-out"
      leave-active-class="transition-all duration-150 ease-in"
      enter-from-class="opacity-0 transform -translate-y-1 scale-95"
      enter-to-class="opacity-100 transform translate-y-0 scale-100"
      leave-from-class="opacity-100 transform translate-y-0 scale-100"
      leave-to-class="opacity-0 transform -translate-y-1 scale-95"
    >
      <div
        v-if="isOpen"
        class="absolute top-full left-0 right-0 mt-2 bg-card border border-border rounded-md shadow-xl z-10 max-h-60 overflow-auto"
      >
        <div
          v-for="option in options"
          :key="option.value"
          class="p-2 m-1 rounded-md text-sm hover:bg-accent cursor-pointer transition-colors duration-200"
          :class="{
            'bg-primary text-primary-foreground': option.value === selectedValue,
            'text-foreground': option.value !== selectedValue,
          }"
          @click="selectOption(option)"
        >
          {{ option.name }}
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { computed, onMounted, onUnmounted, ref } from 'vue'

export interface Option {
  name: string
  value: any
}

interface Props {
  options: Option[]
  placeholder?: string
  modelValue?: any
}

const props = withDefaults(defineProps<Props>(), {
  placeholder: 'Select an option',
  modelValue: '',
})

const emit = defineEmits<{
  'update:modelValue': [value: any]
}>()

const isOpen = ref(false)
const dropdownRef = ref<HTMLElement>()
const internalValue = ref(props.modelValue)

const selectedOption = computed(() => {
  return props.options.find((opt) => opt.value === internalValue.value)
})

const selectedValue = computed({
  get: () => internalValue.value,
  set: (value: any) => {
    internalValue.value = value
    emit('update:modelValue', value)
  },
})

const selectOption = (option: Option) => {
  selectedValue.value = option.value
  isOpen.value = false
}

const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === ' ' || event.key === 'Enter') {
    event.preventDefault()
    isOpen.value = !isOpen.value
  }

  if (event.key === 'Escape') {
    isOpen.value = false
  }
}

const handleBlur = (event: FocusEvent) => {
  const relatedTarget = event.relatedTarget as HTMLElement
  if (!dropdownRef.value?.contains(relatedTarget)) {
    setTimeout(() => {
      isOpen.value = false
    }, 150)
  }
}

const handleClickOutside = (event: MouseEvent) => {
  if (dropdownRef.value && !dropdownRef.value.contains(event.target as Node)) {
    isOpen.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>
