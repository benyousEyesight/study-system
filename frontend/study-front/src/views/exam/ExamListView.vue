<template>
  <div>
    <el-row justify="space-between" align="middle">
      <h3>考试安排</h3>
      <el-button type="primary" @click="router.push('/exams/create')">创建考试</el-button>
    </el-row>

    <el-card style="margin-top: 16px">
      <el-form :inline="true" :model="query">
        <el-form-item label="标题">
          <el-input v-model="query.title" placeholder="搜索标题" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="选择状态" clearable style="width: 120px">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="进行中" value="IN_PROGRESS" />
            <el-option label="已结束" value="FINISHED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchExams">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-table :data="examList" border stripe style="margin-top: 16px" v-loading="loading">
      <el-table-column prop="title" label="考试名称" min-width="180" />
      <el-table-column prop="paperTitle" label="关联试卷" min-width="150" />
      <el-table-column label="时间模式" width="100">
        <template #default="{ row }">
          {{ { FIXED_WINDOW: '固定时段', FLEXIBLE: '灵活时长', BOTH: '两者' }[row.timeMode] || row.timeMode }}
        </template>
      </el-table-column>
      <el-table-column label="开始时间" width="160">
        <template #default="{ row }">{{ row.startTime || '-' }}</template>
      </el-table-column>
      <el-table-column label="作答时长" width="80">
        <template #default="{ row }">{{ row.durationMinutes || '-' }}分钟</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="router.push(`/exams/${row.id}`)">详情</el-button>
          <el-button size="small" @click="router.push(`/exams/${row.id}/edit`)" :disabled="row.status!=='DRAFT'">编辑</el-button>
          <el-button size="small" :type="row.status==='PUBLISHED'?'warning':'success'"
            @click="toggleStatus(row)" :disabled="row.status==='FINISHED'">
            {{ row.status==='PUBLISHED'?'结束':'发布' }}
          </el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.id)" :disabled="row.status!=='DRAFT'">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="query.page" v-model:page-size="query.size"
      :total="total" layout="total, prev, pager, next"
      style="margin-top: 16px; justify-content: flex-end" @change="fetchExams" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getExamPage, deleteExam, updateExamStatus } from '@/api/exam'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const examList = ref<any[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, title: undefined, status: undefined })

onMounted(() => fetchExams())

async function fetchExams() {
  loading.value = true
  try {
    const res: any = await getExamPage(query)
    examList.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() { Object.assign(query, { page: 1, title: undefined, status: undefined }); fetchExams() }

async function toggleStatus(row: any) {
  const newStatus = row.status === 'PUBLISHED' ? 'FINISHED' : 'PUBLISHED'
  await updateExamStatus(row.id, newStatus)
  ElMessage.success(newStatus === 'PUBLISHED' ? '已发布' : '已结束')
  fetchExams()
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确定删除该考试安排吗？', '提示')
  await deleteExam(id)
  ElMessage.success('删除成功')
  fetchExams()
}

function statusType(s: string) {
  const map: Record<string, string> = { DRAFT: 'info', PUBLISHED: 'success', IN_PROGRESS: 'warning', FINISHED: '' }
  return map[s] || 'info'
}
function statusLabel(s: string) {
  const map: Record<string, string> = { DRAFT: '草稿', PUBLISHED: '已发布', IN_PROGRESS: '进行中', FINISHED: '已结束' }
  return map[s] || s
}
</script>
