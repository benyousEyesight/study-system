<template>
  <div v-loading="loading">
    <el-row justify="space-between" align="middle">
      <h3>考试详情</h3>
      <div>
        <el-button type="primary" @click="router.push(`/exams/${route.params.id}/report`)" v-if="exam?.gradedSessions > 0">查看报告</el-button>
        <el-button @click="router.back()">返回</el-button>
      </div>
    </el-row>

    <el-card style="margin-top: 16px" v-if="exam">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="考试名称" :span="2">{{ exam.title }}</el-descriptions-item>
        <el-descriptions-item label="关联试卷">{{ exam.paperTitle }}</el-descriptions-item>
        <el-descriptions-item label="时间模式">{{ {FIXED_WINDOW:'固定时段',FLEXIBLE:'灵活时长',BOTH:'两者'}[exam.timeMode as string] }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ exam.startTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ exam.endTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="作答时长">{{ exam.durationMinutes || '-' }}分钟</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="exam.status==='PUBLISHED'?'success':'info'" size="small">{{ exam.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="批改进度">{{ exam.gradedSessions }}/{{ exam.totalSessions }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ exam.description || '无' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card style="margin-top: 16px">
      <template #header><span>分配列表</span></template>
      <el-table :data="assignments" border stripe>
        <el-table-column prop="assignType" label="分配方式" width="120">
          <template #default="{row}">{{ {USER:'用户',ROLE:'角色',CLASS:'班级',EXAM_CODE:'考试码'}[row.assignType as string] || row.assignType }}</template>
        </el-table-column>
        <el-table-column prop="assigneeId" label="分配对象ID" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getExamById, getAssignments } from '@/api/exam'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const exam = ref<any>(null)
const assignments = ref<any[]>([])

onMounted(async () => {
  loading.value = true
  try {
    const id = Number(route.params.id)
    const [examRes, assignRes]: any = await Promise.all([getExamById(id), getAssignments(id)])
    exam.value = examRes.data
    assignments.value = assignRes.data || []
  } catch { ElMessage.error('加载失败')
  } finally { loading.value = false }
})
</script>
