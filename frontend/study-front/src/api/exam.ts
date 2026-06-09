import request from './request'

export function getExamPage(params: any) {
  return request.get('/exams/page', { params })
}

export function getExamById(id: number) {
  return request.get(`/exams/${id}`)
}

export function createExam(data: any) {
  return request.post('/exams', data)
}

export function updateExam(id: number, data: any) {
  return request.put(`/exams/${id}`, data)
}

export function deleteExam(id: number) {
  return request.delete(`/exams/${id}`)
}

export function updateExamStatus(id: number, status: string) {
  return request.put(`/exams/${id}/status?status=${status}`)
}

export function addAssignments(examId: number, data: any) {
  return request.post(`/exams/${examId}/assignments`, data)
}

export function getAssignments(examId: number) {
  return request.get(`/exams/${examId}/assignments`)
}

export function removeAssignment(examId: number, id: number) {
  return request.delete(`/exams/${examId}/assignments/${id}`)
}

export function getMyExams() {
  return request.get('/my-exams')
}

export function joinExamByCode(examId: number, code: string) {
  return request.post(`/my-exams/${examId}/join?code=${code}`)
}

export function startExam(examId: number) {
  return request.post(`/my-exams/${examId}/start`)
}

export function getSessionData(sessionId: number) {
  return request.get(`/my-exams/sessions/${sessionId}`)
}

export function saveAnswer(sessionId: number, data: any) {
  return request.post(`/my-exams/sessions/${sessionId}/answer`, data)
}

export function submitExam(sessionId: number) {
  return request.post(`/my-exams/sessions/${sessionId}/submit`)
}

export function getExamResult(sessionId: number) {
  return request.get(`/my-exams/sessions/${sessionId}/result`)
}

export function heartbeat(sessionId: number, tabSwitchCount: number) {
  return request.post(`/my-exams/sessions/${sessionId}/heartbeat?tabSwitchCount=${tabSwitchCount}`)
}

export function getGradingExams() {
  return request.get('/grading/exams')
}

export function getGradingSessions(examId: number) {
  return request.get(`/grading/exams/${examId}/sessions`)
}

export function getSessionForGrading(sessionId: number) {
  return request.get(`/grading/sessions/${sessionId}`)
}

export function gradeSession(sessionId: number, data: any) {
  return request.post(`/grading/sessions/${sessionId}/grade`, data)
}

export function releaseGrades(sessionId: number) {
  return request.post(`/grading/sessions/${sessionId}/release`)
}
