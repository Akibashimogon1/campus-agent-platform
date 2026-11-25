import axios from 'axios'
import { ElMessage } from 'element-plus'
import { normalizeHttpError, normalizeBusinessResponse } from '../utils/httpError'

const authApi = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
})

authApi.interceptors.response.use(
  (response) => {
    const result = response.data
    if (result?.code !== 'SUCCESS') {
      const normalized = normalizeBusinessResponse(result)
      ElMessage.error(normalized.message)
      return Promise.reject(normalized)
    }
    return result
  },
  (error) => {
    const normalized = normalizeHttpError(error)
    ElMessage.error(normalized.message)
    return Promise.reject(normalized)
  },
)

export function login(username, password) {
  return authApi.post('/auth/login', { username, password })
}

export function getCurrentUser(token) {
  return authApi.get('/auth/me', {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  })
}
