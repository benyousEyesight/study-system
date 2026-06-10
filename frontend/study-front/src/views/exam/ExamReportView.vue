<template>
  <div v-loading="loading">
    <el-row justify="space-between" align="middle">
      <h3>考试报告</h3>
      <div>
        <el-button type="primary" @click="exportExcel" :disabled="!report">导出 Excel</el-button>
        <el-button @click="router.back()">返回</el-button>
      </div>
    </el-row>

    <el-card style="margin-top: 16px" v-if="report">
      <template #header>
        <span>{{ report.examTitle }}</span>
        <el-tag style="margin-left: 8px" size="small">{{ report.paperTitle }}</el-tag>
      </template>

      <el-row :gutter="16">
        <el-col :span="4" v-for="card in summaryCards" :key="card.label">
          <div class="stat-card">
            <div class="stat-label">{{ card.label }}</div>
            <div class="stat-value" :style="{ color: card.color }">{{ card.value }}</div>
          </div>
        </el-col>
      </el-row>

      <h4 style="margin-top: 24px">分数分布</h4>
      <div class="bar-chart">
        <div class="bar-item" v-for="bar in bars" :key="bar.label">
          <div class="bar-label">{{ bar.label }}</div>
          <div class="bar-track">
            <div class="bar-fill" :style="{ width: bar.pct, background: bar.color }" />
          </div>
          <div class="bar-count">{{ bar.count }}人</div>
        </div>
      </div>

      <h4 style="margin-top: 24px">学生成绩</h4>
      <el-table :data="report.students" border stripe max-height="500">
        <el-table-column prop="rank" label="排名" width="70" align="center" />
        <el-table-column prop="userId" label="学生ID" width="100" />
        <el-table-column prop="totalScore" label="总分" width="100">
          <template #default="{ row }">
            <span :class="{ 'rank-first': row.rank === 1, 'rank-last': row.rank === report.students.length && row.rank > 1 }">
              {{ row.totalScore ?? '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'GRADED' ? 'success' : 'info'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 试卷质量 -->
    <el-card style="margin-top: 16px" v-if="quality">
      <template #header>试卷质量分析</template>
      <el-row :gutter="16">
        <el-col :span="8" v-for="card in qualityCards" :key="card.label">
          <div class="stat-card">
            <div class="stat-label">{{ card.label }}</div>
            <div class="stat-value" :style="{ color: card.color }">{{ card.value }}</div>
            <div class="stat-desc">{{ card.desc }}</div>
          </div>
        </el-col>
      </el-row>

      <h4 style="margin-top: 20px">各题质量</h4>
      <el-table :data="quality.questions" border stripe size="small" max-height="400">
        <el-table-column label="#" prop="sort" width="50" align="center" />
        <el-table-column label="题型" prop="type" width="100" />
        <el-table-column label="满分" prop="maxScore" width="60" />
        <el-table-column label="平均分" prop="avgScore" width="70" />
        <el-table-column label="难度" width="80">
          <template #default="{ row }">
            <el-tag :type="difficultyTag(row.difficulty)" size="small">{{ row.difficulty }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="区分度" width="80">
          <template #default="{ row }">
            <el-tag :type="discriminationTag(row.discrimination)" size="small">{{ row.discrimination }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="高分组正确" prop="highGroupCorrect" width="80" align="center" />
        <el-table-column label="低分组正确" prop="lowGroupCorrect" width="80" align="center" />
      </el-table>
    </el-card>

    <el-empty v-if="!loading && !report" description="考试不存在" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getExamReport } from '@/api/stats'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const report = ref<any>(null)
const quality = ref<any>(null)
const apiBase = (import.meta as any).env.VITE_API_BASE || ''

function exportExcel() {
  if (!report.value) return
  const examId = route.params.id
  window.open(apiBase + '/api/stats/exam/' + examId + '/export', '_blank')
}

const summaryCards = computed(() => {
  if (!report.value) return []
  const r = report.value
  return [
    { label: '参考人数', value: r.totalStudents, color: '#409EFF' },
    { label: '平均分', value: r.avgScore, color: '#67C23A' },
    { label: '最高分', value: r.maxScore, color: '#E6A23C' },
    { label: '最低分', value: r.minScore, color: '#F56C6C' },
    { label: '及格率', value: r.passRate + '%', color: '#67C23A' },
    { label: '优秀率', value: r.excellentRate + '%', color: '#909399' },
  ]
})

const bars = computed(() => {
  if (!report.value?.distribution) return []
  const d = report.value.distribution
  const maxCount = Math.max(d.below60, d.between60And69, d.between70And79, d.between80And89, d.between90And100, 1)
  const total = report.value.totalStudents || 1
  return [
    { label: '<60', count: d.below60, pct: (d.below60 / maxCount * 100) + '%', color: '#F56C6C' },
    { label: '60-69', count: d.between60And69, pct: (d.between60And69 / maxCount * 100) + '%', color: '#E6A23C' },
    { label: '70-79', count: d.between70And79, pct: (d.between70And79 / maxCount * 100) + '%', color: '#409EFF' },
    { label: '80-89', count: d.between80And89, pct: (d.between80And89 / maxCount * 100) + '%', color: '#67C23A' },
    { label: '90-100', count: d.between90And100, pct: (d.between90And100 / maxCount * 100) + '%', color: '#67C23A' },
  ]
})

const qualityCards = computed(() => {
  if (!quality.value) return []
  const q = quality.value
  return [
    { label: '难度系数', value: q.difficultyIndex, desc: q.difficultyLabel, color: difficultyColor(q.difficultyIndex) },
    { label: '区分度', value: q.discriminationIndex, desc: q.discriminationLabel, color: discriminationColor(q.discriminationIndex) },
    { label: '信度 (Cronbach α)', value: q.reliabilityIndex, desc: q.reliabilityLabel, color: reliabilityColor(q.reliabilityIndex) },
  ]
})

function difficultyColor(v: number) {
  if (v < 0.3) return '#F56C6C'
  if (v <= 0.7) return '#67C23A'
  return '#E6A23C'
}
function discriminationColor(v: number) {
  if (v >= 0.4) return '#67C23A'
  if (v >= 0.2) return '#E6A23C'
  return '#F56C6C'
}
function reliabilityColor(v: number) {
  if (v >= 0.8) return '#67C23A'
  if (v >= 0.7) return '#E6A23C'
  return '#F56C6C'
}
function difficultyTag(v: number) {
  if (v < 0.3) return 'danger'
  if (v <= 0.7) return 'success'
  return 'warning'
}
function discriminationTag(v: number) {
  if (v >= 0.4) return 'success'
  if (v >= 0.2) return 'warning'
  return 'danger'
}

async function fetchQuality() {
  const examId = route.params.id
  try {
    const res: any = await (await fetch(apiBase + '/api/stats/exam/' + examId + '/quality')).json()
    if (res.code === 200) quality.value = res.data
  } catch { /* ignore */ }
}

onMounted(async () => {
  loading.value = true
  try {
    const id = Number(route.params.id)
    const res: any = await getExamReport(id)
    report.value = res.data
    if (res.data) fetchQuality()
  } catch { ElMessage.error('加载报告失败')
  } finally { loading.value = false }
})
</script>

<style scoped>
.stat-card {
  text-align: center;
  padding: 16px 8px;
  background: #fafafa;
  border-radius: 8px;
  border: 1px solid #eee;
}
.stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}
.stat-value {
  font-size: 24px;
  font-weight: bold;
}
.stat-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
.bar-chart {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px 0;
}
.bar-item {
  display: flex;
  align-items: center;
  gap: 12px;
}
.bar-label {
  width: 60px;
  text-align: right;
  font-size: 14px;
  color: #606266;
}
.bar-track {
  flex: 1;
  height: 24px;
  background: #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
}
.bar-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.6s ease;
}
.bar-count {
  width: 50px;
  font-size: 13px;
  color: #909399;
}
.rank-first {
  color: #E6A23C;
  font-weight: bold;
}
.rank-last {
  color: #F56C6C;
}
</style>
