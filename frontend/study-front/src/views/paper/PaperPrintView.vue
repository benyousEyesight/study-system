<template>
  <div class="print-wrapper" v-loading="loading">
    <div class="no-print" style="text-align:center;margin-bottom:20px">
      <el-button type="primary" @click="printWindow()">打印 / 导出PDF</el-button>
      <el-button @click="router.back()">返回</el-button>
    </div>

    <div v-if="paper" class="paper-content">
      <div class="paper-header">
        <h1 class="school-name">在线教育考试系统</h1>
        <h2 class="paper-title">{{ paper.title }}</h2>
        <div class="paper-meta">
          <span>总分：{{ paper.totalScore }} 分</span>
          <span>时长：{{ paper.durationMinutes }} 分钟</span>
        </div>
        <div class="seal-line">装&emsp;订&emsp;线</div>
        <div class="student-info">
          <span>姓名：________</span>
          <span>班级：________</span>
          <span>学号：________</span>
        </div>
        <div class="seal-line">装&emsp;订&emsp;线</div>
      </div>

      <div class="paper-body">
        <div v-for="(section, si) in paper.sections" :key="section.id" class="section">
          <h3 class="section-title">{{ section.title }}（共{{ section.questions?.length || 0 }}题，{{ section.totalScore }}分）</h3>
          <div v-for="(q, qi) in section.questions" :key="q.questionId" class="question">
            <div class="question-header">
              <span class="q-number">{{ si + 1 }}.{{ qi + 1 }}</span>
              <span class="q-type">[{{ typeLabel(q.questionInfo?.type) }}]</span>
              <span class="q-score">（{{ q.score }}分）</span>
            </div>
            <div class="q-content" v-html="renderContent(q.questionInfo?.contentJson)"></div>

            <div v-if="hasOptions(q.questionInfo?.type)" class="options">
              <div v-for="opt in parseOptions(q.questionInfo?.contentJson)" :key="opt.key" class="option-item">
                {{ opt.key }}. {{ opt.value }}
              </div>
            </div>

            <div v-if="needsAnswerArea(q.questionInfo?.type)" class="answer-area">
              <div class="answer-line" v-for="n in answerLines(q.questionInfo?.type)" :key="n"></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getPaperById } from '@/api/paper'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const paper = ref<any>(null)

function printWindow() { window.print() }

onMounted(async () => {
  loading.value = true
  try {
    const res: any = await getPaperById(Number(route.params.id))
    paper.value = res.data
    setTimeout(() => printWindow(), 500)
  } catch { ElMessage.error('加载失败')
  } finally { loading.value = false }
})

function typeLabel(type: string) {
  const map: Record<string, string> = { SINGLE_CHOICE: '单选', MULTIPLE_CHOICE: '多选', TRUE_FALSE: '判断', FILL_BLANK: '填空', SHORT_ANSWER: '简答', ESSAY: '论述', COMPOSITE: '组合' }
  return map[type] || type
}

function hasOptions(type: string) {
  return ['SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE'].includes(type)
}

function needsAnswerArea(type: string) {
  return ['SHORT_ANSWER', 'ESSAY'].includes(type)
}

function answerLines(type: string) {
  return type === 'ESSAY' ? 8 : 4
}

function renderContent(jsonStr: string) {
  if (!jsonStr) return ''
  try {
    const obj = JSON.parse(jsonStr)
    const text = obj.passage || obj.text || jsonStr
    return text.replace(/\n/g, '<br>')
  } catch { return jsonStr }
}

function parseOptions(contentJson: string) {
  try {
    const obj = JSON.parse(contentJson)
    if (!obj.options) return []
    return Object.entries(obj.options).map(([key, value]) => ({ key, value: value as string }))
  } catch { return [] }
}
</script>

<style>
@media print {
  .no-print { display: none !important; }
  .print-wrapper { padding: 0 !important; background: white !important; }
  @page { margin: 20mm 15mm; }
  body { -webkit-print-color-adjust: exact; print-color-adjust: exact; }
}

.print-wrapper {
  max-width: 210mm;
  margin: 0 auto;
  padding: 20px;
  background: #fff;
  font-family: 'SimSun', 'STSong', serif;
  font-size: 14px;
  line-height: 1.8;
  color: #000;
}

.paper-header {
  text-align: center;
  margin-bottom: 20px;
}
.school-name {
  font-size: 20px;
  margin-bottom: 4px;
  font-weight: bold;
}
.paper-title {
  font-size: 18px;
  margin-bottom: 8px;
}
.paper-meta {
  display: flex;
  justify-content: center;
  gap: 32px;
  font-size: 14px;
  margin-bottom: 12px;
}
.seal-line {
  text-align: center;
  letter-spacing: 4px;
  font-size: 14px;
  border-top: 1px dashed #333;
  border-bottom: 1px dashed #333;
  padding: 4px 0;
  margin: 8px 0;
}
.student-info {
  display: flex;
  justify-content: center;
  gap: 40px;
  font-size: 14px;
  padding: 8px 0;
}
.section {
  margin-bottom: 24px;
}
.section-title {
  font-size: 16px;
  font-weight: bold;
  border-bottom: 1px solid #333;
  padding-bottom: 4px;
  margin-bottom: 12px;
}
.question {
  margin-bottom: 16px;
  page-break-inside: avoid;
}
.question-header {
  margin-bottom: 4px;
}
.q-number {
  font-weight: bold;
  margin-right: 4px;
}
.q-type {
  color: #666;
  margin-right: 4px;
}
.q-score {
  color: #999;
  font-size: 13px;
}
.q-content {
  margin-bottom: 8px;
  padding-left: 20px;
}
.options {
  padding-left: 32px;
  margin-bottom: 8px;
}
.option-item {
  margin-bottom: 2px;
}
.answer-area {
  padding-left: 20px;
  margin-top: 8px;
}
.answer-line {
  border-bottom: 1px solid #ccc;
  height: 28px;
  margin-bottom: 4px;
}
</style>
