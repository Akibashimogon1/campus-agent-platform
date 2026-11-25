import { createRouter, createWebHistory } from 'vue-router'

const routes = [
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

export default router
