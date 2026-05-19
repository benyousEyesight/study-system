import request from './request'

export function getUserPage(params: { page: number; size: number; tenantId: number }) {
  return request.get('/users/page', { params })
}

export function createUser(data: any) {
  return request.post('/users', data)
}

export function updateUser(data: any) {
  return request.put('/users', data)
}

export function deleteUser(id: number) {
  return request.delete(`/users/${id}`)
}

export function getUserById(id: number) {
  return request.get(`/users/${id}`)
}
