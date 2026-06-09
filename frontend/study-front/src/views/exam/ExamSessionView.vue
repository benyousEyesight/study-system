<template>
  <div class="exam-session" v-loading="loading">
    <!-- 顶部栏 -->
    <div class="exam-header">
      <h3>{{ sessionData?.examInfo?.title || '在线答题' }}</h3>
      <div class="header-right">
        <ExamTimer v-if="sessionData?.examInfo?.remainingSeconds > 0"
          :remaining-seconds="sessionData.examInfo.remainingSeconds" @expired="handleSubmit" />
        <el-button type="danger" @click="handleSubmit" :loading="submitting">交卷</el-button>
      </div>
    </div>

    <div class="exam-body" v-if="sessionData">
      <!-- 左侧导航 -->
      <QuestionNavigator
        :sections="sessionData.sections"
        :current-id="currentQuestionId"
        :answered-ids="answeredIds"
        @select="navigateTo" />

      <!-- 右侧答题区 -->
      <div class="question-area">
        <div v-for="section in sessionData.sections" :key="section.id" v-show="currentSection?.id === section.id">
          <h4>{{ section.title }}</h4>
          <div v-for="q in section.questions" :key="q.questionId" v-show="q.questionId === currentQuestionId">
            <div class="question-meta">本题 {{ q.score }} 分</div>
            <QuestionRenderer :type="q.type" :content="q.content" :answer="getAnswer(q.questionId)"
              @answer="(val: string) => saveAnswer(q, val)" />
          </div>
        </div>

        <div class="question-nav-buttons">
          <el-button @click="prevQuestion" :disabled="isFirst">上一题</el-button>
          <el-button type="primary" @click="nextQuestion" :disabled="isLast">下一题</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getSessionData, saveAnswer, submitExam, heartbeat } from '@/api/exam'
import { ElMessage, ElMessageBox } from 'element-plus'
import ExamTimer from './components/ExamTimer.vue'
import QuestionNavigator from './components/QuestionNavigator.vue'
import QuestionRenderer from './components/QuestionRenderer.vue'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const submitting = ref(false)
const sessionData = ref<any>(null)
const currentQuestionId = ref(0)
const answers = ref<Record<number, string>>({})

const allQuestions = computed(() => {
  const qs: any[] = []
  for (const s of sessionData.value?.sections || []) {
    for (const q of s.questions || []) qs.push(q)
  }
  return qs
})

const currentSection = computed(() => {
  for (const s of sessionData.value?.sections || []) {
    if (s.questions?.find((q: any) => q.questionId === currentQuestionId.value)) return s
  }
  return null
})

const answeredIds = computed(() => new Set(
  Object.entries(answers.value).filter(([, v]) => v).map(([k]) => Number(k))
))

const currentIdx = computed(() => allQuestions.value.findIndex(q => q.questionId === currentQuestionId.value))
const isFirst = computed(() => currentIdx.value <= 0)
const isLast = computed(() => currentIdx.value >= allQuestions.value.length - 1)

let heartbeatTimer: number | null = null
let tabSwitchCount = 0

onMounted(async () => {
  loading.value = true
  try {
    const sessionId = Number(route.params.sessionId)
    const res: any = await getSessionData(sessionId)
    sessionData.value = res.data
    if (sessionData.value?.sections?.[0]?.questions?.[0]) {
      currentQuestionId.value = sessionData.value.sections[0].questions[0].questionId
    }
    document.addEventListener('visibilitychange', onVisibilityChange)
    heartbeatTimer = window.setInterval(sendHeartbeat, 30000)
  } catch {
    ElMessage.error('加载考试数据失败')
  } finally { loading.value = false }
})

onUnmounted(() => {
  document.removeEventListener('visibilitychange', onVisibilityChange)
  if (heartbeatTimer) clearInterval(heartbeatTimer)
})

function onVisibilityChange() {
  if (document.hidden) { tabSwitchCount++ }
}

async function sendHeartbeat() {
  if (!sessionData.value) return
  try {
    await heartbeat(sessionData.value.id, tabSwitchCount)
  } catch {}
}

function getAnswer(questionId: number): string | undefined {
  return answers.value[questionId]
}

async function saveAnswer(q: any, val: string) {
  answers.value[q.questionId] = val
  try {
    await saveAnswer(sessionData.value.id, {
      questionId: q.questionId,
      sectionId: currentSection.value?.id,
      answerJson: val,
    })
  } catch {}
}

function navigateTo(questionId: number) { currentQuestionId.value = questionId }
function nextQuestion() {
  if (!isLast.value) currentQuestionId.value = allQuestions.value[currentIdx.value + 1].questionId
}
function prevQuestion() {
  if (!isFirst.value) currentQuestionId.value = allQuestions.value[currentIdx.value - 1].questionId
}

async function handleSubmit() {
  try {
    await ElMessageBox.confirm('确定交卷吗？交卷后不可再修改答案。', '提示', { type: 'warning' })
    submitting.value = true
    await submitExam(sessionData.value.id)
    ElMessage.success('交卷成功')
    router.push('/my-exams')
  } catch {
    // cancelled or error
  } finally { submitting.value = false }
}
</script>

<style scoped>
.exam-session { height: calc(100vh - 100px); display: flex; flex-direction: column; }
.exam-header { display: flex; justify-content: space-between; align-items: center;
  padding: 12px 20px; background: #fff; border-bottom: 1px solid #e6e6e6; }
.header-right { display: flex; align-items: center; gap: 20px; }
.exam-body { display: flex; flex: 1; overflow: hidden; background: #fff; }
.question-area { flex: 1; padding: 20px; overflow-y: auto; }
.question-meta { color: #909399; font-size: 13px; margin-bottom: 12px; }
.question-nav-buttons { text-align: center; margin-top: 24px; padding-top: 16px; border-top: 1px solid #eee; }
</style>
