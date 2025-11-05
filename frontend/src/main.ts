import { createPinia } from 'pinia'
import { createApp } from 'vue'
import Toast, { type PluginOptions, POSITION } from 'vue-toastification'
import 'vue-toastification/dist/index.css'
import App from './App.vue'
import './main.css'
import router from './router'

const app = createApp(App)

const options: PluginOptions = {
  closeButton: false,
  maxToasts: 3,
  draggable: true,
  timeout: 5000,
  closeOnClick: true,
  draggablePercent: 0.6,
  icon: true,
  rtl: false,
  position: POSITION.BOTTOM_RIGHT,
  hideProgressBar: true,
}

if (typeof window.global === 'undefined') {
  window.global = window
}

app.use(createPinia())
app.use(router)
app.use(Toast, options)

app.mount('#app')
