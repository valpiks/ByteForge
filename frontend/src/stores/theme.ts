import { defineStore } from 'pinia'
import { ref, watchEffect } from 'vue'

export const useThemeStore = defineStore('theme', () => {
  const theme = ref<'dark' | 'light'>('dark')

  const setTheme = (value: 'dark' | 'light') => {
    theme.value = value
  }

  const initializeTheme = () => {
    const root = window.document.documentElement
    const initialTheme = localStorage.getItem('theme') as 'light' | 'dark' | null

    if (initialTheme) {
      setTheme(initialTheme)
      root.classList.toggle('dark', initialTheme === 'dark')
    } else {
      root.classList.add('dark')
    }
  }

  watchEffect(() => {
    initializeTheme()
  })

  const toggleTheme = () => {
    const newTheme = theme.value === 'light' ? 'dark' : 'light'
    setTheme(newTheme)
    localStorage.setItem('theme', newTheme)
    window.document.documentElement.classList.toggle('dark', newTheme === 'dark')
  }

  return { theme, setTheme, toggleTheme, initializeTheme }
})
