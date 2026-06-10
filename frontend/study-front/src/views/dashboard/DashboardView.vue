<template>
  <div v-loading="loading">
    <h2>仪表盘</h2>

    <el-row :gutter="16" style="margin-top: 20px">
      <el-col :span="6" v-for="card in statCards" :key="card.label">
        <el-card shadow="never">
          <div class="stat-item">
            <div class="stat-value" :style="{ color: card.color }">{{ card.value }}</div>
            <div class="stat-label">{{ card.label }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="16">
        <el-card shadow="never">
          <template #header>
            <span>最近考试</span>
            <el-button size="small" style="float:right" @click="router.push('/exams')">查看全部</el-button>
          </template>
          <el-table :data="dashboard?.recentExams || []" stripe v-if="(dashboard?.recentExams || []).length > 0">
            <el-table-column prop="title" label="考试名称" min-width="160" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="statusTag(row.status)" size="small">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="参与人数" width="100" prop="totalSessions" />
            <el-table-column label="批改进度" width="160">
              <template #default="{ row }">
                <span>{{ row.gradedSessions }}/{{ row.totalSessions }}</span>
                <el-progress :percentage="row.totalSessions > 0 ? Math.round(row.gradedSessions / row.totalSessions * 100) : 0" :stroke-width="6" style="width:80px;display:inline-block;margin-left:8px" />
              </template>
            </el-table-column>
            <el-table-column label="平均分" width="80" prop="avgScore" />
          </el-table>
          <el-empty v-else description="暂无考试" />
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="never">
          <template #header><span>待批改</span></template>
          <div v-if="(dashboard?.gradingAlerts || []).length > 0">
            <div v-for="alert in dashboard?.gradingAlerts" :key="alert.examId" class="alert-item" @click="router.push(`/grading/exams`)">
              <span class="alert-title">{{ alert.examTitle }}</span>
              <el-tag size="small" type="warning">{{ alert.ungradedCount }}份待批</el-tag>
            </div>
          </div>
          <el-empty v-else description="暂无待批改" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getTeacherDashboard } from '@/api/stats'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const dashboard = ref<any>(null)

const statCards = computed(() => {
  if (!dashboard.value) return [
    { label: '考试总数', value: '--', color: '#409EFF' },
    { label: '已发布', value: '--', color: '#67C23A' },
    { label: '待批改', value: '--', color: '#E6A23C' },
    { label: '参与人次', value: '--', color: '#909399' },
  ]
  const d = dashboard.value
  return [
    { label: '考试总数', value: d.totalExams, color: '#409EFF' },
    { label: '已发布', value: d.publishedExams, color: '#67C23A' },
    { label: '待批改', value: d.pendingGrading, color: d.pendingGrading > 0 ? '#F56C6C' : '#67C23A' },
    { label: '参与人次', value: d.totalSessions, color: '#909399' },
  ]
})

function statusTag(status: string) {
  const map: Record<string, string> = { DRAFT: 'info', PUBLISHED: 'success', IN_PROGRESS: 'warning', FINISHED: 'info' }
  return map[status] || 'info'
}

async function fetchDashboard() {
  loading.value = true
  try {
    const res: any = await getTeacherDashboard()
    dashboard.value = res.data
  } catch { ElMessage.error('加载仪表盘失败')
  } finally { loading.value = false }
}

onMounted(() => fetchDashboard())
</script>

<style scoped>
.stat-item {
  text-align: center;
  padding: 10px 0;
}
.stat-value {
  font-size: 32px;
  font-weight: bold;
  transition: color 0.3s;
}
.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 8px;
}
.alert-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
}
.alert-item:hover {
  color: #409EFF;
}
.alert-item:last-child {
  border-bottom: none;
}
.alert-title {
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  margin-right: 8px;
}
</style>
