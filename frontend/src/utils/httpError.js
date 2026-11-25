export const HTTP_ERROR_CATEGORY = {
  NETWORK: 'NETWORK',
  SERVER: 'SERVER',
  UNAUTHORIZED: 'UNAUTHORIZED',
  FORBIDDEN: 'FORBIDDEN',
  BUSINESS: 'BUSINESS',
  UNKNOWN: 'UNKNOWN',
}

export function normalizeHttpError(error) {
  if (!error.response) {
    if (error.code === 'ECONNABORTED') {
      return {
        category: HTTP_ERROR_CATEGORY.NETWORK,
        message: '请求超时，请稍后重试',
        status: null,
      }
    }
    return {
      category: HTTP_ERROR_CATEGORY.NETWORK,
      message: '无法连接后端服务，请确认后端已启动后重试',
      status: null,
    }
  }

  const status = error.response.status
  if (status === 401) {
    return {
      category: HTTP_ERROR_CATEGORY.UNAUTHORIZED,
      message: '用户名或密码错误',
      status,
    }
  }
  if (status === 403) {
    return {
      category: HTTP_ERROR_CATEGORY.FORBIDDEN,
      message: '无权限执行该操作',
      status,
    }
  }
  if (status >= 500 && status <= 599) {
    return {
      category: HTTP_ERROR_CATEGORY.SERVER,
      message: '后端服务异常，请稍后重试',
      status,
    }
  }
  return {
    category: HTTP_ERROR_CATEGORY.UNKNOWN,
    message: '请求失败，请稍后重试',
    status,
  }
}

export function normalizeBusinessResponse(result) {
  return {
    category: HTTP_ERROR_CATEGORY.BUSINESS,
    message: result?.message || '操作失败，请稍后重试',
    status: null,
  }
}
