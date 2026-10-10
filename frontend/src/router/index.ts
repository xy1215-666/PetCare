import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/register',
      name: 'register',
      component: () => import('../views/RegisterView.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/',
      component: () => import('../layouts/MainLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'dashboard',
          component: () => import('../views/DashboardView.vue'),
        },
        {
          path: 'pets',
          name: 'pets',
          component: () => import('../views/PetListView.vue'),
        },
        {
          path: 'pets/new',
          name: 'pet-create',
          component: () => import('../views/PetCreateView.vue'),
        },
        {
          path: 'pets/:petId',
          name: 'pet-detail',
          component: () => import('../views/PetDetailView.vue'),
        },
        {
          path: 'expenses',
          name: 'expenses',
          component: () => import('../views/ExpensesView.vue'),
        },
        {
          path: 'ai',
          name: 'ai-chat',
          component: () => import('../views/AiChatView.vue'),
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: { name: 'dashboard' },
    },
  ],
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth) {
    if (!authStore.accessToken) {
      return { name: 'login', query: { redirect: to.fullPath } }
    }

    if (!authStore.currentUser) {
      try {
        await authStore.refreshCurrentUser()
      } catch {
        authStore.clearSession()
        return { name: 'login', query: { redirect: to.fullPath } }
      }
    }
  }

  if (to.meta.guestOnly && authStore.accessToken) {
    try {
      if (!authStore.currentUser) {
        await authStore.refreshCurrentUser()
      }
      return { name: 'dashboard' }
    } catch {
      authStore.clearSession()
    }
  }

  return true
})

export default router
