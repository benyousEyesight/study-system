<template>
  <div v-loading="loading">
    <el-row justify="space-between" align="middle">
      <h3 v-if="!currentSession">选择学生答卷</h3>
      <h3 v-else>批改 - 学生答卷</h3>
      <el-button @click="currentSession ? (currentSession=null,fetchSessions()) : router.back()">
        {{ currentSession ? '返回列表' : '返回' }}
      </el-button>
    </el-row>

    <!-- 学生列表 -->
    <el-table v-if="!currentSession" :data="sessions" border stripe style="margin-top: 16px">
      <el-table-column prop="userId" label="学生ID" width="100" />
      <el-table-column prop="submittedAt" label="提交时间" width="170" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{row}">{{ {SUBMITTED:'待批改',GRADING:'批改中',GRADED:'已批改'}[row.status as string] }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="loadSession(row.id)">批改</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 批改界面 -->
    <div v-else style="margin-top: 16px">
      <el-card v-for="ans in currentSession.answers" :key="ans.questionId" style="margin-bottom: 12px">
        <div class="answer-header">
          <span><b>{{ ans.questionContent }}</b></span>
          <el-tag :type="ans.gradingStatus==='AUTO_GRADED'?'success':'warning'" size="small">
            {{ ans.gradingStatus==='AUTO_GRADED'?'自动批改':'待批改' }}
          </el-tag>
        </div>
        <div class="answer-detail">
          <div><b>学生答案：</b>{{ ans.studentAnswer || '未作答' }}</div>
          <div v-if="ans.correctAnswer"><b>参考答案：</b>{{ ans.correctAnswer }}</div>
          <div v-if="ans.gradingStatus === 'AUTO_GRADED'">
            <b>得分：</b>{{ ans.score }} / {{ ans.totalScore }}
          </div>
          <div v-else>
            <el-input-number v-model="gradeScores[ans.questionId]" :min="0" :max="ans.totalScore || 100" size="small" />
            <el-input v-model="gradeComments[ans.questionId]" placeholder="评语（可选）" size="small" style="width: 300px; margin-left: 8px" />
          </div>
        </div>
      </el-card>

      <div style="text-align: center; margin-top: 16px">
        <el-button type="primary" @click="submitGrading" :loading="saving">保存批改</el-button>
        <el-button type="success" @click="releaseGrade" :loading="releasing">发布成绩</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getGradingSessions, getSessionForGrading, gradeSession, releaseGrades } from '@/api/exam'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const saving = ref(false)
const releasing = ref(false)
const sessions = ref<any[]>([])
const currentSession = ref<any>(null)
const gradeScores = ref<Record<number, number>>({})
const gradeComments = ref<Record<number, string>>({})

onMounted(() => fetchSessions())

async function fetchSessions() {
  loading.value = true
  try { const res: any = await getGradingSessions(Number(route.params.examId)); sessions.value = res.data || [] }
  finally { loading.value = false }
}

async function loadSession(sessionId: number) {
  loading.value = true
  try {
    const res: any = await getSessionForGrading(sessionId)
    currentSession.value = res.data
    gradeScores.value = {}
    gradeComments.value = {}
  } finally { loading.value = false }
}

async function submitGrading() {
  saving.value = true
  try {
    const grades = Object.entries(gradeScores.value).filter(([, s]) => s !== undefined).map(([qId, score]) => ({
      questionId: Number(qId), score, comment: gradeComments.value[Number(qId)] || '',
    }))
    if (grades.length === 0) { ElMessage.warning('没有需要批改的题目'); return }
    await gradeSession(currentSession.value.id, { grades })
    ElMessage.success('批改保存成功')
  } catch {} finally { saving.value = false }
}

async function releaseGrade() {
  releasing.value = true
  try {
    await releaseGrades(currentSession.value.id)
    ElMessage.success('成绩发布成功')
    currentSession.value = null
    fetchSessions()
  } catch {} finally { releasing.value = false }
}
</script>

<style scoped>
.answer-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.answer-detail { font-size: 14px; line-height: 1.8; color: #606266; }
</style>
