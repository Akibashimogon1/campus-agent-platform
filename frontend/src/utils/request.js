import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import { normalizeHttpError, normalizeBusinessResponse } from './httpError'

const request = axios.create({
  baseURL: '/api/v1/mcp',
  timeout: 15000,
})

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('mcp_access_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    const normalized = normalizeHttpError(error)
    ElMessage.error(normalized.message)
    return Promise.reject(normalized)
  },
)

request.interceptors.response.use(
  (response) => {
    const result = response.data
    if (result?.code !== 'SUCCESS') {
      const normalized = normalizeBusinessResponse(result)
      ElMessage.error(normalized.message)
      return Promise.reject(normalized)
    }
    return result.data
  },
  (error) => {
    const normalized = normalizeHttpError(error)
    if (normalized.status === 401) {
      useAuthStore().clearSession()
      if (window.location.pathname !== '/login') {
        window.location.assign('/login')
      }
    }
    ElMessage.error(normalized.message)
    return Promise.reject(normalized)
  },
)

export default request
