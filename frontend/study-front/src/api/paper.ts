import request from './request'

export function getPaperPage(params: any) {
  return request.get('/papers/page', { params })
}

export function getPaperById(id: number) {
  return request.get(`/papers/${id}`)
}

export function createPaper(data: any) {
  return request.post('/papers', data)
}

export function updatePaper(id: number, data: any) {
  return request.put(`/papers/${id}`, data)
}

export function deletePaper(id: number) {
  return request.delete(`/papers/${id}`)
}

export function updatePaperStatus(id: number, status: string) {
  return request.put(`/papers/${id}/status?status=${status}`)
}

export function getTemplatePage(params: any) {
  return request.get('/paper-templates/page', { params })
}

export function getTemplateById(id: number) {
  return request.get(`/paper-templates/${id}`)
}

export function createTemplate(data: any) {
  return request.post('/paper-templates', data)
}

export function updateTemplate(id: number, data: any) {
  return request.put(`/paper-templates/${id}`, data)
}

export function deleteTemplate(id: number) {
  return request.delete(`/paper-templates/${id}`)
}

export function generatePaper(data: any) {
  return request.post('/paper-templates/generate', data)
}
