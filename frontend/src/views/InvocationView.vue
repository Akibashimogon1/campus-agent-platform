<script setup>
import { onMounted, ref } from 'vue'
import { mcpApi } from '../api/mcp'

const loading = ref(false)
const records = ref([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const requestError = ref('')

/** 根据调用状态返回 Tag 类型。 */
function statusType(status) {
  if (status === 'SUCCEEDED') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'RUNNING') return 'warning'
  return 'info'
}

/** 格式化时间；空值显示为 '-'。 */
function formatTime(value) {
  if (!value) return '-'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  }).format(new Date(value))
}

/** 加载调用记录分页列表。 */
async function loadInvocations() {
  loading.value = true
  requestError.value = ''
  try {
    const result = await mcpApi.queryInvocations({
      page: page.value,
      size: size.value,
    })
    records.value = result.records || []
    page.value = result.page || page.value
    size.value = result.size || size.value
    total.value = result.total || 0
  } catch (error) {
    requestError.value = error?.message || '请求失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function handlePageChange(nextPage) {
  page.value = nextPage
  loadInvocations()
}

function handleSizeChange(nextSize) {
  size.value = nextSize
  page.value = 1
  loadInvocations()
}

onMounted(loadInvocations)
</script>

<template>
  <el-card shadow="never">
    <el-empty
      v-if="requestError"
      :description="requestError"
      class="table-state"
    >
      <el-button type="primary" :loading="loading" @click="loadInvocations">重新加载</el-button>
    </el-empty>

    <div v-else class="table-scroll">
    <el-table v-loading="loading" :data="records" border stripe empty-text="暂无调用记录">
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="traceId" label="traceId" min-width="180" />
      <el-table-column prop="toolName" label="工具名" min-width="160" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="errorCode" label="错误码" min-width="150">
        <template #default="{ row }">
          {{ row.errorCode || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="开始时间" min-width="180">
        <template #default="{ row }">
          {{ formatTime(row.startedAt) }}
        </template>
      </el-table-column>
      <el-table-column label="结束时间" min-width="180">
        <template #default="{ row }">
          {{ formatTime(row.finishedAt) }}
        </template>
      </el-table-column>
      <el-table-column label="耗时" width="110">
        <template #default="{ row }">
          {{ row.latencyMs == null ? '-' : `${row.latencyMs} ms` }}
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
</template>

<style scoped>
.table-state {
  min-height: 280px;
}

.table-scroll {
  width: 100%;
  overflow-x: auto;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
