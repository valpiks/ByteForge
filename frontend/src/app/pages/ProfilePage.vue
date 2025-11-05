<template>
  <main class="container mx-auto px-4 py-8">
    <div class="max-w-4xl mx-auto">
      <h1 class="text-3xl font-semibold mb-8">Profile Settings</h1>
      <div class="py-6 px-5 bg-card rounded-lg border border-border flex flex-col gap-8">
        <div>
          <h1 class="text-2xl font-semibold">Profile information</h1>
          <p class="text-muted-foreground text-sm">Update your profile details and preferences</p>
        </div>
        <div class="flex gap-8 items-center">
          <div
            v-if="!hasProfileImage"
            class="rounded-full text-3xl font-semibold bg-gradient-primary border border-border leading-tight h-24 w-24 flex items-center justify-center"
          >
            {{ userStore.user?.username?.charAt(0) }}
          </div>
          <img
            v-else
            :src="profileImageUrl"
            alt="User avatar"
            class="rounded-full bg-gradient-primary border border-border leading-tight h-24 w-24 object-cover"
          />
          <div>
            <label
              for="fileUpload"
              class="button-base px-4 py-2 border border-input bg-background hover:bg-accent hover:text-accent-foreground"
            >
              <span>Change Avatar</span>
              <input
                @change="handleImageChange"
                name="fileUpload"
                id="fileUpload"
                type="file"
                accept="image/*"
                hidden
              />
            </label>
            <p class="text-muted-foreground text-sm mt-2">JPG, PNG or GIF. Max size 2MB</p>
          </div>
        </div>
        <div class="flex flex-col gap-4">
          <div class="flex flex-col gap-2">
            <CustomLabel htmlFor="username"> Username </CustomLabel>
            <div class="relative">
              <Icon
                icon="tabler:user"
                class="absolute left-3 top-3 h-4.5 w-4.5 text-muted-foreground"
              />
              <CustomInput
                v-model="userData.username"
                id="username"
                name="username"
                placeholder="Username"
                type="text"
                required
                class="pl-10"
              />
            </div>
          </div>
          <div class="flex flex-col gap-2">
            <CustomLabel htmlFor="email"> Email </CustomLabel>
            <div class="relative">
              <Icon
                icon="ic:outline-email"
                class="absolute left-3 top-3 h-4.5 w-4.5 text-muted-foreground"
              />
              <CustomInput
                v-model="userData.email"
                id="email"
                name="email"
                placeholder="example@gmail.com"
                type="text"
                required
                class="pl-10"
              />
            </div>
          </div>
          <SimpleButton @click="handleUpdateUser" class="w-32">Save Changes</SimpleButton>
        </div>
      </div>
    </div>
  </main>
</template>

<script setup lang="ts">
import { useUserStore } from '@/stores/user'
import { Icon } from '@iconify/vue'
import { computed, ref } from 'vue'
import { useToast } from 'vue-toastification'
import CustomInput from '../components/UI/CustomInput.vue'
import CustomLabel from '../components/UI/CustomLabel.vue'
import SimpleButton from '../components/UI/buttons/SimpleButton.vue'
import { userApi } from '../shared/api/User'

const userStore = useUserStore()
const toast = useToast()
const imageVersion = ref(0)

const hasProfileImage = computed(() => {
  return userStore.user?.profileImageUrl
})

const profileImageUrl = computed(() => {
  if (!hasProfileImage.value) return null
  return `http://localhost:8000/api/v1/profile-image/${userStore.user?.id}?v=${imageVersion.value}`
})

const handleImageChange = async (e) => {
  const files = e.target.files
  if (!files || files.length === 0) return

  try {
    const response = await userApi.uploadProfileImage(files[0])
    userStore.user = response
    imageVersion.value++
  } catch (error) {
    toast.error('Failed to upload image')
  }
}

const handleUpdateUser = async () => {
  if (!userData.value.username || !userData.value.email) {
    return toast.error('Все поля должны быть заполненны корректно')
  }
  try {
    const response = await userApi.updateCurrentUser(userData.value)
    userData.value = response
    userStore.user = response
    toast.success('Profile updated successfully')
  } catch (error) {
    toast.error('Failed to update profile')
  }
}

const userData = ref({
  username: userStore.user?.username,
  email: userStore.user?.email,
})
</script>

<style scoped>
.button-base {
  @apply inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-md text-sm font-medium ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 [&_svg]:pointer-events-none [&_svg]:size-4 [&_svg]:shrink-0;
}
</style>
