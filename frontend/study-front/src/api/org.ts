import request from './request'

// --- 年级 ---
export function getGradeList(tenantId = 0) {
  return request.get('/grades/list', { params: { tenantId } })
}
export function createGrade(data: any) {
  return request.post('/grades', data)
}
export function updateGrade(data: any) {
  return request.put('/grades', data)
}
export function deleteGrade(id: number) {
  return request.delete(`/grades/${id}`)
}

// --- 班级 ---
export function getClazzPage(params: { page: number; size: number; tenantId: number; gradeId?: number }) {
  return request.get('/clazzes/page', { params })
}
export function getClazzList(tenantId = 0, gradeId?: number) {
  return request.get('/clazzes/list', { params: { tenantId, gradeId } })
}
export function createClazz(data: any) {
  return request.post('/clazzes', data)
}
export function updateClazz(data: any) {
  return request.put('/clazzes', data)
}
export function deleteClazz(id: number) {
  return request.delete(`/clazzes/${id}`)
}

// --- 班级学生 ---
export function getClazzStudents(clazzId: number) {
  return request.get(`/clazzes/${clazzId}/students`)
}
export function addClazzStudents(clazzId: number, studentIds: number[]) {
  return request.post(`/clazzes/${clazzId}/students`, { studentIds })
}
export function removeClazzStudent(clazzId: number, id: number) {
  return request.delete(`/clazzes/${clazzId}/students/${id}`)
}

// --- 任课教师 ---
export function getClazzTeachers(clazzId: number) {
  return request.get(`/clazzes/${clazzId}/teachers`)
}
export function assignClazzTeacher(clazzId: number, data: any) {
  return request.post(`/clazzes/${clazzId}/teachers`, data)
}
export function removeClazzTeacher(clazzId: number, id: number) {
  return request.delete(`/clazzes/${clazzId}/teachers/${id}`)
}
