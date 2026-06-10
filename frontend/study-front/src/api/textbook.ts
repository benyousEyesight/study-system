import request from './request'

export function parseTextbook(file: File) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/textbook/parse', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000,
  })
}

export function saveTextbookKnowledgePoints(subjectId: number, chapters: any[]) {
  return request.post(`/textbook/save?subjectId=${subjectId}&tenantId=0`, chapters)
}
