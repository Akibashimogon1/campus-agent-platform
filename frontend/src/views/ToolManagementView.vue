<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { mcpApi } from '../api/mcp'

const loading = ref(false)
const tools = ref([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const requestError = ref('')
const filters = reactive({
  category: '',
  scope: '',
  status: '',
  keyword: '',
})

const detailVisible = ref(false)
const detail = ref(null)

/** 根据状态返回 Element Plus Tag 类型。 */
function statusType(status) {
  if (status === 'ENABLED' || status === 'REGISTERED') return 'success'
  if (status === 'DISABLED' || status === 'UNHEALTHY') return 'danger'
  if (status === 'UNKNOWN') return 'info'
  return 'warning'
}

/** 加载工具分页列表。 */
async function loadTools() {
  loading.value = true
  requestError.value = ''
  try {
    const result = await mcpApi.queryTools({
      category: filters.category || undefined,
      scope: filters.scope || undefined,
      status: filters.status || undefined,
      keyword: filters.keyword.trim() || undefined,
      page: page.value,
      size: size.value,
    })
    tools.value = result.records || []
    page.value = result.page || page.value
    size.value = result.size || size.value
    total.value = result.total || 0
  } catch (error) {
    requestError.value = error?.message || '请求失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

/** 重置筛选条件并回到第一页。 */
function resetFilters() {
  filters.category = ''
  filters.scope = ''
  filters.status = ''
  filters.keyword = ''
  page.value = 1
  loadTools()
}

/** 分页切换时重新加载。 */
function handlePageChange(nextPage) {
  page.value = nextPage
  loadTools()
}

/** 分页条数切换时重新加载。 */
function handleSizeChange(nextSize) {
  size.value = nextSize
  page.value = 1
  loadTools()
}

/** 打开工具详情抽屉。 */
async function showDetail(toolId) {
  detail.value = await mcpApi.getTool(toolId)
  detailVisible.value = true
}

/** 确认后切换工具上线状态。 */
async function toggleStatus(row) {
  const targetStatus = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  const action = targetStatus === 'ENABLED' ? '上线' : '下线'

  await ElMessageBox.confirm(`确定要${action}工具「${row.toolName}」吗？`, `确认${action}`, {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })

  await mcpApi.updateToolStatus(row.id, targetStatus)
  ElMessage.success(`工具已${action}`)
  await loadTools()
}

onMounted(loadTools)
</script>

<template>
  <el-card shadow="never">
    <el-form :inline="true" class="filter-form" @submit.prevent="loadTools">
      <el-form-item label="分类">
        <el-select v-model="filters.category" placeholder="全部分类" clearable>
          <el-option label="query" value="query" />
          <el-option label="write" value="write" />
          <el-option label="approve" value="approve" />
          <el-option label="notify" value="notify" />
        </el-select>
      </el-form-item>
      <el-form-item label="作用域">
        <el-select v-model="filters.scope" placeholder="全部作用域" clearable>
          <el-option label="TENANT" value="TENANT" />
          <el-option label="WORKFLOW" value="WORKFLOW" />
          <el-option label="NODE" value="NODE" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="filters.status" placeholder="全部状态" clearable>
          <el-option label="ENABLED" value="ENABLED" />
          <el-option label="DISABLED" value="DISABLED" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键字">
        <el-input
          v-model="filters.keyword"
          placeholder="工具名或描述"
          clearable
          @keyup.enter="loadTools"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadTools">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </el-form-item>
    </el-form>

    <el-empty
      v-if="requestError"
      :description="requestError"
      class="table-state"
    >
      <el-button type="primary" :loading="loading" @click="loadTools">重新加载</el-button>
    </el-empty>

    <div v-else class="table-scroll">
    <el-table v-loading="loading" :data="tools" border stripe empty-text="暂无符合条件的 MCP 工具">
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="toolName" label="工具名" min-width="160" />
      <el-table-column prop="displayName" label="展示名" min-width="160" />
      <el-table-column prop="category" label="分类" width="120" />
      <el-table-column prop="scope" label="作用域" width="120" />
      <el-table-column prop="permissionLevel" label="权限级别" width="120" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="version" label="版本" width="90" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="showDetail(row.id)">详情</el-button>
          <el-button
            :type="row.status === 'ENABLED' ? 'danger' : 'success'"
            link
            @click="toggleStatus(row)"
          >
            {{ row.status === 'ENABLED' ? '下线' : '上线' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    </div>

    <el-pagination
      v-if="!requestError"
      class="pagination"
      background
      layout="total, sizes, prev, pager, next, jumper"
      :current-page="page"
      :page-size="size"
      :page-sizes="[10, 20, 50, 100]"
      :total="total"
      @current-change="handlePageChange"
      @size-change="handleSizeChange"
    />
  </el-card>

  <el-drawer v-model="detailVisible" title="工具详情" size="420px">
    <el-descriptions v-if="detail" :column="1" border>
      <el-descriptions-item label="工具名">{{ detail.toolName }}</el-descriptions-item>
      <el-descriptions-item label="描述">{{ detail.description || '-' }}</el-descriptions-item>
      <el-descriptions-item label="分类">{{ detail.category }}</el-descriptions-item>
      <el-descriptions-item label="作用域">{{ detail.scope }}</el-descriptions-item>
      <el-descriptions-item label="权限级别">{{ detail.permissionLevel }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag :type="statusType(detail.status)">{{ detail.status }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="版本">{{ detail.version }}</el-descriptions-item>
    </el-descriptions>
  </el-drawer>
</template>

<style scoped>
.table-state {
  min-height: 280px;
}

.table-scroll {
  width: 100%;
  overflow-x: auto;
}

.filter-form {
  margin-bottom: 12px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
