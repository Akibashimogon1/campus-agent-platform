<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { mcpApi } from '../api/mcp'

const loading = ref(false)
const submitting = ref(false)
const servers = ref([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const requestError = ref('')

const dialogVisible = ref(false)
const dialogMode = ref('register')
const formRef = ref(null)
const form = reactive({
  id: null,
  serverCode: '',
  serverName: '',
  transportType: 'HTTP_SSE',
  endpoint: '',
  credentialRef: '',
  status: 'REGISTERED',
})

const connectivityVisible = ref(false)
const connectivityResult = ref(null)

const rules = {
  serverCode: [{ required: true, message: '请输入 Server 编码', trigger: 'blur' }],
  serverName: [{ required: true, message: '请输入服务名称', trigger: 'blur' }],
  transportType: [{ required: true, message: '请选择传输类型', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

/** 根据状态返回 Tag 类型。 */
function statusType(status) {
  if (status === 'ENABLED' || status === 'REGISTERED') return 'success'
  if (status === 'DISABLED' || status === 'UNHEALTHY') return 'danger'
  if (status === 'UNKNOWN') return 'info'
  return 'warning'
}

/** 加载 Server 分页列表。 */
async function loadServers() {
  loading.value = true
  requestError.value = ''
  try {
    const result = await mcpApi.listServers({
      page: page.value,
      size: size.value,
    })
    servers.value = result.records || []
    page.value = result.page || page.value
    size.value = result.size || size.value
    total.value = result.total || 0
  } catch (error) {
    requestError.value = error?.message || '请求失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.id = null
  form.serverCode = ''
  form.serverName = ''
  form.transportType = 'HTTP_SSE'
  form.endpoint = ''
  form.credentialRef = ''
  form.status = 'REGISTERED'
}

/** 打开注册弹窗。 */
function openRegister() {
  resetForm()
  dialogMode.value = 'register'
  dialogVisible.value = true
}

/** 打开编辑弹窗，并填充已有配置。 */
function openEdit(row) {
  resetForm()
  dialogMode.value = 'edit'
  form.id = row.id
  form.serverName = row.serverName || ''
  form.transportType = row.transportType || 'HTTP_SSE'
  form.endpoint = row.endpoint || ''
  form.status = row.status || 'REGISTERED'
  dialogVisible.value = true
}

/** 提交注册或更新。 */
async function submitForm() {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (dialogMode.value === 'register') {
      await mcpApi.registerServer({
        serverCode: form.serverCode,
        serverName: form.serverName,
        transportType: form.transportType,
        endpoint: form.transportType === 'STDIO' ? undefined : form.endpoint,
        credentialRef: form.credentialRef || undefined,
        status: form.status,
      })
      ElMessage.success('Server 注册成功')
    } else {
      await mcpApi.updateServer(form.id, {
        serverName: form.serverName,
        transportType: form.transportType,
        endpoint: form.transportType === 'STDIO' ? undefined : form.endpoint,
        credentialRef: form.credentialRef || undefined,
        status: form.status,
      })
      ElMessage.success('Server 更新成功')
    }
    dialogVisible.value = false
    await loadServers()
  } finally {
    submitting.value = false
  }
}

/** 删除前二次确认。 */
async function deleteServer(row) {
  await ElMessageBox.confirm(`确定要删除 Server「${row.serverName}」吗？`, '确认删除', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning',
  })
  await mcpApi.deleteServer(row.id)
  ElMessage.success('Server 已删除')
  await loadServers()
}

/** 测试连通性并展示后端结果。 */
async function testConnectivity(row) {
  connectivityResult.value = await mcpApi.testConnectivity(row.id)
  connectivityVisible.value = true
}

function handlePageChange(nextPage) {
  page.value = nextPage
  loadServers()
}

function handleSizeChange(nextSize) {
  size.value = nextSize
  page.value = 1
  loadServers()
}

onMounted(loadServers)
</script>

<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-button type="primary" @click="openRegister">注册 Server</el-button>
    </div>

    <el-empty
      v-if="requestError"
      :description="requestError"
      class="table-state"
    >
      <el-button type="primary" :loading="loading" @click="loadServers">重新加载</el-button>
    </el-empty>

    <div v-else class="table-scroll">
    <el-table v-loading="loading" :data="servers" border stripe empty-text="暂无 MCP Server 数据">
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="serverName" label="服务名称" min-width="160" />
      <el-table-column prop="serverCode" label="编码" min-width="160" />
      <el-table-column prop="transportType" label="传输类型" width="130" />
      <el-table-column prop="endpoint" label="地址" min-width="220">
        <template #default="{ row }">
          {{ row.endpoint || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="健康状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusType(row.healthStatus)">{{ row.healthStatus }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
          <el-button type="danger" link @click="deleteServer(row)">删除</el-button>
          <el-button type="success" link @click="testConnectivity(row)">连通性测试</el-button>
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

  <el-dialog
    v-model="dialogVisible"
    :title="dialogMode === 'register' ? '注册 MCP Server' : '编辑 MCP Server'"
    width="560px"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
      <el-form-item v-if="dialogMode === 'register'" label="Server 编码" prop="serverCode">
        <el-input v-model="form.serverCode" maxlength="128" />
      </el-form-item>
      <el-form-item label="服务名称" prop="serverName">
        <el-input v-model="form.serverName" maxlength="128" />
      </el-form-item>
      <el-form-item label="传输类型" prop="transportType">
        <el-select v-model="form.transportType">
          <el-option label="HTTP_SSE" value="HTTP_SSE" />
          <el-option label="STDIO" value="STDIO" />
        </el-select>
      </el-form-item>
      <el-form-item label="地址">
        <el-input
          v-model="form.endpoint"
          maxlength="1024"
          :disabled="form.transportType === 'STDIO'"
          placeholder="STDIO 模式可留空"
        />
      </el-form-item>
      <el-form-item label="凭据引用">
        <el-input v-model="form.credentialRef" maxlength="128" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="form.status">
          <el-option label="REGISTERED" value="REGISTERED" />
          <el-option label="ENABLED" value="ENABLED" />
          <el-option label="DISABLED" value="DISABLED" />
          <el-option label="UNHEALTHY" value="UNHEALTHY" />
        </el-select>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="connectivityVisible" title="连通性测试结果" width="520px">
    <el-descriptions v-if="connectivityResult" :column="1" border>
      <el-descriptions-item label="状态">
        <el-tag :type="connectivityResult.connected ? 'success' : 'danger'">
          {{ connectivityResult.status }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="是否连通">{{ connectivityResult.connected ? '是' : '否' }}</el-descriptions-item>
      <el-descriptions-item label="耗时">{{ connectivityResult.latencyMs }} ms</el-descriptions-item>
      <el-descriptions-item label="测试时间">{{ connectivityResult.testedAt || '-' }}</el-descriptions-item>
      <el-descriptions-item label="说明">{{ connectivityResult.message || '-' }}</el-descriptions-item>
    </el-descriptions>
  </el-dialog>
</template>

<style scoped>
.table-state {
  min-height: 280px;
}

.table-scroll {
  width: 100%;
  overflow-x: auto;
}

.toolbar {
  margin-bottom: 16px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
