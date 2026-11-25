import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    meta: { public: true },
  },
  {
    path: '/',
    redirect: '/servers',
  },
  {
    path: '/servers',
    name: 'servers',
    component: () => import('../views/ServerManagementView.vue'),
  },
  {
    path: '/tools',
    name: 'tools',
    component: () => import('../views/ToolManagementView.vue'),
  },
  {
    path: '/invocations',
    name: 'invocations',
    component: () => import('../views/InvocationView.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const authStore = useAuthStore()
  if (!to.meta.public && !authStore.isAuthenticated) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path === '/login' && authStore.isAuthenticated) {
    return '/'
  }
})

export default router
