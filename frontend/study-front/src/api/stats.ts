import request from './request'

export function getExamReport(examId: number) {
  return request.get(`/stats/exam/${examId}/report`)
}
