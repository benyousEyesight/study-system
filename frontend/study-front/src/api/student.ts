import request from './request'

export function getStudentOverview() {
  return request.get('/stats/student/overview')
}

export function getStudentSubjects() {
  return request.get('/stats/student/subjects')
}

export function getStudentRecentExams(limit = 10) {
  return request.get('/stats/student/recent', { params: { limit } })
}
