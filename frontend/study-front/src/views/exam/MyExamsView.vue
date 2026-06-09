<template>
  <div>
    <h3>我的考试</h3>

    <el-card style="margin-top: 16px">
      <template #header><span>待考</span></template>
      <el-empty v-if="pendingExams.length === 0" description="暂无待考考试" />
      <el-table v-else :data="pendingExams" border stripe>
        <el-table-column prop="title" label="考试名称" min-width="180" />
        <el-table-column prop="durationMinutes" label="时长(分钟)" width="100" />
        <el-table-column label="时间范围" min-width="200">
          <template #default="{row}">{{ row.startTime || '-' }} ~ {{ row.endTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{row}">
            <el-tag size="small">待参加</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{row}">
            <el-button type="primary" size="small" @click="handleStart(row)">开始考试</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card style="margin-top: 16px">
      <template #header><span>已考</span></template>
      <el-empty v-if="completedExams.length === 0" description="暂无已完成考试" />
      <el-table v-else :data="completedExams" border stripe>
        <el-table-column prop="title" label="考试名称" min-width="180" />
        <el-table-column prop="submittedAt" label="提交时间" width="170" />
        <el-table-column label="状态" width="100">
          <template #default="{row}">
            <el-tag :type="row.sessionStatus==='GRADED'?'success':''" size="small">
              {{ row.sessionStatus==='GRADED'?'已出分':row.sessionStatus==='SUBMITTED'?'待批改':'批改中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{row}">
            <el-button size="small" :disabled="row.sessionStatus!=='GRADED'" @click="router.push(`/exam/result/${row.sessionId}`)">查看成绩</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyExams, startExam } from '@/api/exam'
import { ElMessage } from 'element-plus'

const router = useRouter()
const exams = ref<any[]>([])

const pendingExams = computed(() => exams.value.filter((e: any) => !e.totalSessions))
const completedExams = computed(() => exams.value.filter((e: any) => e.totalSessions))

onMounted(() => fetchMyExams())

async function fetchMyExams() {
  const res: any = await getMyExams()
  exams.value = res.data || []
}

async function handleStart(row: any) {
  try {
    const res: any = await startExam(row.id)
    router.push(`/exam/session/${res.data.id}`)
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '开始考试失败')
  }
}
</script>
