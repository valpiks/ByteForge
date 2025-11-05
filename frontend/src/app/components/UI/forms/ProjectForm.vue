<template>
  <div>
    <div v-if="needTitle">
      <h1 class="font-semibold text-xl">{{ props.title }}</h1>
      <p class="text-muted-foreground text-sm mb-6">{{ props.description }}</p>
    </div>
    <form @submit.prevent="handleSubmit" class="flex flex-col gap-6">
      <div class="flex flex-col gap-3">
        <CustomLabel> Project Name </CustomLabel>
        <CustomInput
          placeholder="My C++ Project"
          :model-value="props.project.title"
          @update:model-value="(value) => handleChange('title', value)"
        />
      </div>
      <div class="flex flex-col gap-3">
        <CustomLabel> Description </CustomLabel>
        <CustomTextArea
          placeholder="A brief description of your project..."
          :model-value="props.project.description"
          @update:model-value="(value) => handleChange('description', value)"
        />
      </div>
      <div class="mt-4 flex gap-2 justify-end" :class="adittionalButtonClasses">
        <SimpleButton @click="handleCancel" type="button" variant="outline"> Cancel </SimpleButton>
        <SimpleButton type="submit">{{ props.submitBtn }}</SimpleButton>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import CustomInput from '../CustomInput.vue'
import CustomLabel from '../CustomLabel.vue'
import CustomTextArea from '../CustomTextarea.vue'
import SimpleButton from '../buttons/SimpleButton.vue'

interface Project {
  title: string
  description: string
}

interface Props {
  title?: string
  description?: string
  submitBtn?: string
  project: Project
  needTitle?: boolean
  adittionalButtonClasses?: string[]
}

const props = withDefaults(defineProps<Props>(), {
  title: 'Create New Project',
  description: 'Enter the details for your new C++ project.',
  submitBtn: 'Create Project',
  needTitle: true,
})

const emit = defineEmits<{
  (e: 'update', field: string, value: string): void
  (e: 'submit', project: Project): void
  (e: 'cancel'): void
}>()

const handleChange = (field: keyof Project, value: string) => {
  emit('update', field, value)
}

const handleSubmit = () => {
  emit('submit', props.project)
}

const handleCancel = () => {
  emit('cancel')
}
</script>
