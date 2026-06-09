<template>
  <div>
    <h3>批改管理</h3>
    <el-table :data="exams" border stripe style="margin-top: 16px" v-loading="loading">
      <el-table-column prop="title" label="考试名称" min-width="200" />
      <el-table-column label="批改进度" width="150">
        <template #default="{ row }">{{ row.gradedSessions }}/{{ row.totalSessions }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button size="small" type="primary"
            @click="router.push(`/grading/exams/${row.id}/sessions`)">批改</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getGradingExams } from '@/api/exam'

const router = useRouter()
const loading = ref(false)
const exams = ref<any[]>([])

onMounted(async () => {
  loading.value = true
  try { const res: any = await getGradingExams(); exams.value = res.data || [] }
  finally { loading.value = false }
})
</script>
