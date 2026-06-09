import request from './request'

export function getRolePage(params: { page: number; size: number; tenantId: number }) {
  return request.get('/roles/page', { params })
}

export function getRoleList(tenantId: number) {
  return request.get('/roles/list', { params: { tenantId } })
}

export function createRole(data: any) {
  return request.post('/roles', data)
}

export function updateRole(data: any) {
  return request.put('/roles', data)
}

export function deleteRole(id: number) {
  return request.delete(`/roles/${id}`)
}

export function getRolePermissions(id: number) {
  return request.get(`/roles/${id}/permissions`)
}

export function assignRolePermissions(id: number, permissionIds: number[]) {
  return request.put(`/roles/${id}/permissions`, { permissionIds })
}

export function getUserRoles(id: number) {
  return request.get(`/users/${id}/roles`)
}

export function assignUserRoles(id: number, roleIds: number[]) {
  return request.put(`/users/${id}/roles`, { roleIds })
}
