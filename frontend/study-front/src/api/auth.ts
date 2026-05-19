import request from './request'

export function login(data: { username: string; password: string; tenantId?: number }) {
  return request.post('/auth/login', data)
}

export function logout() {
  return request.post('/auth/logout')
}
