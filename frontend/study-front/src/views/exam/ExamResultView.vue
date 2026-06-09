<template>
  <div v-loading="loading">
    <el-row justify="space-between" align="middle">
      <h3>考试成绩</h3>
      <el-button @click="router.push('/my-exams')">返回</el-button>
    </el-row>

    <el-card style="margin-top: 16px" v-if="result">
      <el-statistic title="总分" :value="result.totalScore || 0" />
      <el-divider />

      <div v-for="ans in result.answers" :key="ans.questionId" class="answer-item">
        <div class="answer-header">
          <span class="question-text">{{ ans.questionContent }}</span>
          <el-tag :type="ans.isCorrect === 1 ? 'success' : 'danger'" size="small">
            {{ ans.gradingStatus === 'AUTO_GRADED' ? (ans.isCorrect === 1 ? '正确' : '错误') : '人工批改' }}
          </el-tag>
        </div>
        <div class="answer-detail">
          <div><b>你的答案：</b>{{ ans.studentAnswer || '未作答' }}</div>
          <div v-if="ans.gradingStatus === 'AUTO_GRADED'"><b>正确答案：</b>{{ ans.correctAnswer }}</div>
          <div v-if="ans.score !== null"><b>得分：</b>{{ ans.score }} / {{ ans.totalScore }}</div>
          <div v-if="ans.graderComment"><b>评语：</b>{{ ans.graderComment }}</div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getExamResult } from '@/api/exam'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const result = ref<any>(null)

onMounted(async () => {
  loading.value = true
  try {
    const res: any = await getExamResult(Number(route.params.sessionId))
    result.value = res.data
  } catch { ElMessage.error('加载失败')
  } finally { loading.value = false }
})
</script>

<style scoped>
.answer-item { padding: 12px; border: 1px solid #e6e6e6; border-radius: 4px; margin-bottom: 12px; }
.answer-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.question-text { font-weight: bold; flex: 1; }
.answer-detail { font-size: 14px; line-height: 1.8; color: #606266; }
</style>
