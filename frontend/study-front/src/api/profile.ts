import request from './request'

export function getProfile() {
  return request.get('/users/profile')
}

export function uploadAvatar(file: File) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/users/avatar', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
