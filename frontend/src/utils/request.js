import axios from 'axios'
import { ElLoading, ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api/v1/mcp',
  timeout: 15000,
})

let loadingInstance = null
let loadingRequestCount = 0

/** 显示全局加载遮罩。并发请求结束后再关闭。 */
function showLoading() {
  loadingRequestCount += 1
  if (!loadingInstance) {
    loadingInstance = ElLoading.service({
      lock: true,
      text: '正在加载...',
      background: 'rgba(255, 255, 255, 0.72)',
    })
  }
}

/** 隐藏全局加载遮罩。 */
function hideLoading() {
  loadingRequestCount = Math.max(0, loadingRequestCount - 1)
  if (loadingRequestCount === 0 && loadingInstance) {
    loadingInstance.close()
    loadingInstance = null
  }
}

request.interceptors.request.use(
  (config) => {
    showLoading()
    return config
  },
  (error) => {
    hideLoading()
    return Promise.reject(error)
  },
)

request.interceptors.response.use(
  (response) => {
    hideLoading()
    const result = response.data
    if (result?.code !== 'SUCCESS') {
      ElMessage.error(result?.message || '请求失败')
      return Promise.reject(new Error(result?.message || '请求失败'))
    }
    return result.data
  },
  (error) => {
    hideLoading()
    const message = error.response?.data?.message
      || error.message
      || '网络请求失败'
    ElMessage.error(message)
    return Promise.reject(error)
  },
)

export default request
