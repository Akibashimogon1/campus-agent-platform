<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()

const menuItems = [
  { path: '/servers', label: 'MCP Server' },
  { path: '/tools', label: 'MCP 工具' },
  { path: '/invocations', label: '调用记录' },
]
</script>

<template>
  <el-container class="app-shell">
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
      </el-header>
      <el-main class="app-content">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
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
