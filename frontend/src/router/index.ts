import { useUserStore } from '@/stores/user'
import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/app/pages/HomePage.vue'),
      meta: { title: 'Home', layout: 'AppLayout' },
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/app/pages/LoginPage.vue'),
      meta: { title: 'Login', layout: 'AuthLayout' },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/app/pages/RegisterPage.vue'),
      meta: { title: 'Registration', layout: 'AuthLayout' },
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('@/app/pages/ProfilePage.vue'),
      meta: { title: 'Profile', layout: 'AppLayout', requiresAuth: true },
    },
    {
      path: '/projects',
      name: 'projects',
      component: () => import('@/app/pages/ProjectsPage.vue'),
      meta: { title: 'Projects', layout: 'AppLayout', requiresAuth: true },
    },
    {
      path: '/projects/:id',
      name: 'editor',
      component: () => import('@/app/pages/EditorPage.vue'),
      meta: { title: 'Editor', requiresAuth: true },
    },
    {
      path: '/join-link',
      name: 'join',
      component: () => import('@/app/pages/JoinPage.vue'),
      meta: { title: 'JoinPage', requiresAuth: false },
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/app/pages/NotFound.vue'),
      meta: {
        title: 'Page Not Found - ByteForge',
        requiresAuth: false,
      },
    },
  ],
})

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()

  if (!userStore.isAuthenticated) {
    try {
      await userStore.refreshTokens()
    } catch (error) {
      userStore.clearUser()
    }
  }

  if (to.meta.requiresAuth) {
    if (userStore.isAuthenticated) {
      next()
    } else {
      next({
        path: '/login',
        query: { redirect: to.fullPath },
      })
    }
    return
  }

  next()
})

let refreshInterval: number

router.afterEach((to) => {
  const userStore = useUserStore()

  if (refreshInterval) {
    clearInterval(refreshInterval)
  }

  if (userStore.isAuthenticated) {
    refreshInterval = window.setInterval(
      async () => {
        try {
          await userStore.refreshTokens()
        } catch (error) {
          userStore.clearUser()
          clearInterval(refreshInterval)
        }
      },
      5 * 60 * 1000,
    )
  }
})

export default router
