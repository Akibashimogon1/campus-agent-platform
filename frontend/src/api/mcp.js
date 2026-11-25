import request from '../utils/request'

/** MCP 管理接口封装。租户由后端网关注入。 */
export const mcpApi = {
  registerServer(payload) {
    return request.post('/servers', payload)
  },
  listServers(params) {
    return request.get('/servers', { params })
  },
  getServer(serverId) {
    return request.get(`/servers/${serverId}`)
  },
  updateServer(serverId, payload) {
    return request.put(`/servers/${serverId}`, payload)
  },
  deleteServer(serverId) {
    return request.delete(`/servers/${serverId}`)
  },
  testConnectivity(serverId) {
    return request.post(`/servers/${serverId}/connectivity-tests`)
  },
  queryTools(params) {
    return request.get('/tools', { params })
  },
  getTool(toolId) {
    return request.get(`/tools/${toolId}`)
  },
  updateToolStatus(toolId, status) {
    return request.patch(`/tools/${toolId}/status`, { status })
  },
  queryInvocations(params) {
    return request.get('/invocations', { params })
  },
}
