<script setup>
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

function logout() {
  authStore.clearSession()
  router.replace('/login')
}

const menuItems = [
  { path: '/servers', label: 'MCP Server' },
  { path: '/tools', label: 'MCP 工具' },
  { path: '/invocations', label: '调用记录' },
]
</script>

<template>
  <RouterView v-if="route.meta.public" />
  <el-container v-else class="app-shell">
    <el-aside width="220px" class="app-sidebar">
      <div class="brand">MCP 管理后台</div>
      <el-menu
        :default-active="route.path"
        router
        class="app-menu"
      >
        <el-menu-item
          v-for="item in menuItems"
          :key="item.path"
          :index="item.path"
        >
          {{ item.label }}
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="app-header">
        <span class="page-title">{{ route.name === 'servers' ? 'MCP Server 管理' : route.name === 'tools' ? 'MCP 工具管理' : '调用记录' }}</span>
        <div class="user-actions">
          <span>{{ authStore.username }} · {{ authStore.role }}</span>
          <el-button link @click="logout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="app-content">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.user-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #606266;
}

.app-shell {
  min-height: 100vh;
  background: #f5f7fa;
}

.app-sidebar {
  border-right: 1px solid #e4e7ed;
  background: #fff;
}

.brand {
  height: 64px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  border-bottom: 1px solid #e4e7ed;
}

.app-menu {
  border-right: 0;
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
}

.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.app-content {
  min-width: 0;
  padding: 20px;
}
</style>
