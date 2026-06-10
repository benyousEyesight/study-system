import request from './request'

export function getExamReport(examId: number) {
  return request.get(`/stats/exam/${examId}/report`)
}

export function getTeacherDashboard() {
  return request.get('/stats/dashboard/teacher')
}

export function getStudentWeakness() {
  return request.get('/stats/student/weakness')
}

export function getStudentWeaknessBySubject(subjectId: number) {
  return request.get(`/stats/student/weakness/subject/${subjectId}`)
}

export function computeWeakness() {
  return request.post('/stats/student/weakness/compute')
}
