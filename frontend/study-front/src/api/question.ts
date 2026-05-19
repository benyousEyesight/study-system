import request from './request'

export function getQuestionPage(params: any) {
  return request.get('/questions/page', { params })
}

export function createQuestion(data: any) {
  return request.post('/questions', data)
}

export function updateQuestion(id: number, data: any) {
  return request.put(`/questions/${id}`, data)
}

export function deleteQuestion(id: number) {
  return request.delete(`/questions/${id}`)
}

export function getQuestionById(id: number) {
  return request.get(`/questions/${id}`)
}

export function getSubjectList(tenantId: number) {
  return request.get('/subjects/list', { params: { tenantId } })
}

export function getKnowledgePointTree(subjectId: number, tenantId: number) {
  return request.get('/knowledge-points/tree', { params: { subjectId, tenantId } })
}
