<template>
  <div class="profile-page">
    <h3>个人信息</h3>

    <el-row :gutter="20">
      <!-- 基本信息 -->
      <el-col :span="8">
        <el-card shadow="never">
          <div class="user-info-card">
            <div class="avatar-section">
              <el-avatar :size="100" :src="avatarUrl">
                {{ userInfo?.realName?.charAt(0) || userInfo?.username?.charAt(0) }}
              </el-avatar>
              <div class="avatar-actions">
                <el-upload
                  :show-file-list="false"
                  :before-upload="handleAvatarUpload"
                  accept="image/jpeg,image/png"
                >
                  <el-button size="small" :loading="uploading">更换头像</el-button>
                </el-upload>
                <p class="avatar-hint">支持 JPG/PNG，用于考试身份核对</p>
              </div>
            </div>

            <div class="info-items">
              <div class="info-item">
                <span class="label">用户名</span>
                <span class="value">{{ userInfo?.username }}</span>
              </div>
              <div class="info-item">
                <span class="label">姓名</span>
                <span class="value">{{ userInfo?.realName || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="label">类型</span>
                <el-tag :type="typeTag(userInfo?.userType)" size="small">
                  {{ typeLabel(userInfo?.userType) }}
                </el-tag>
              </div>
              <div class="info-item">
                <span class="label">邮箱</span>
                <span class="value">{{ userInfo?.email || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="label">手机号</span>
                <span class="value">{{ userInfo?.phone || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="label">角色</span>
                <span class="value">{{ userInfo?.roles?.join(', ') || '-' }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 学习数据 -->
      <el-col :span="16">
        <!-- 概览统计 -->
        <el-card shadow="never" v-if="isStudent">
          <template #header>学习概览</template>
          <el-row :gutter="20">
            <el-col :span="6" v-for="stat in overviewCards" :key="stat.label">
              <div class="stat-card">
                <div class="stat-value">{{ stat.value }}</div>
                <div class="stat-label">{{ stat.label }}</div>
              </div>
            </el-col>
          </el-row>
        </el-card>

        <!-- 学科分析 -->
        <el-card shadow="never" style="margin-top: 16px" v-if="isStudent && subjects.length > 0">
          <template #header>学科分析</template>

          <div style="display:flex;gap:24px;flex-wrap:wrap">
            <div ref="radarRef" style="width:360px;height:300px;flex-shrink:0"></div>
            <div style="flex:1;min-width:200px">
              <div class="subject-section">
                <div class="subject-group-label">各科得分率</div>
                <div class="subject-tags">
                  <div v-for="s in subjects" :key="s.subjectId" class="subject-bar">
                    <span class="sb-label">{{ s.subjectName }}</span>
                    <div class="sb-track">
                      <div class="sb-fill" :style="{ width: s.accuracy + '%', background: subjectColor(s.accuracy) }" />
                    </div>
                    <span class="sb-value" :style="{ color: subjectColor(s.accuracy) }">{{ s.accuracy }}%</span>
                  </div>
                </div>
              </div>

              <div class="subject-section" v-if="strongSubjects.length > 0" style="margin-top:12px">
                <div class="subject-group-label">擅长学科</div>
                <div class="subject-tags">
                  <el-tag v-for="s in strongSubjects" :key="s.subjectId" type="success" size="large" style="margin:0 8px 8px 0">
                    {{ s.subjectName }} ({{ s.accuracy }}%)
                  </el-tag>
                </div>
              </div>

              <div class="subject-section" v-if="weakSubjects.length > 0" style="margin-top:12px">
                <div class="subject-group-label">薄弱学科</div>
                <div class="subject-tags">
                  <el-tag v-for="s in weakSubjects" :key="s.subjectId" type="danger" size="large" style="margin:0 8px 8px 0">
                    {{ s.subjectName }} ({{ s.accuracy }}%)
                  </el-tag>
                </div>
              </div>
            </div>
          </div>
        </el-card>

        <!-- 知识点薄弱分析 -->
        <el-card shadow="never" style="margin-top: 16px" v-if="isStudent && weaknessData.length > 0">
          <template #header>
            <span>知识点薄弱分析</span>
            <el-button size="small" text :loading="weaknessLoading" @click="fetchWeakness" style="float:right">刷新</el-button>
          </template>
          <div v-for="subject in weaknessData" :key="subject.subjectId" class="weakness-subject">
            <div class="weakness-subject-header">
              <span class="weakness-subject-name">{{ subject.subjectName }}</span>
              <span class="weakness-subject-accuracy" :style="{ color: weaknessColor(subject.subjectAccuracy) }">
                综合得分率 {{ subject.subjectAccuracy }}%
              </span>
            </div>
            <div class="weakness-items">
              <div v-for="item in subject.items" :key="item.knowledgePointId" class="weakness-item">
                <div class="weakness-item-left">
                  <span class="weakness-kp-name" :title="item.knowledgePointName">{{ item.knowledgePointName }}</span>
                  <span class="weakness-kp-attempt">({{ item.attemptCount }}题)</span>
                </div>
                <div class="weakness-item-right">
                  <div class="weakness-bar-track">
                    <div class="weakness-bar-fill" :style="{ width: item.accuracy + '%', background: weaknessColor(item.accuracy) }" />
                  </div>
                  <span class="weakness-bar-value" :style="{ color: weaknessColor(item.accuracy) }">{{ item.accuracy }}%</span>
                  <el-tag :type="weaknessTag(item.level)" size="small" style="width:64px;text-align:center">
                    {{ weaknessLabel(item.level) }}
                  </el-tag>
                </div>
              </div>
            </div>
          </div>
        </el-card>

        <!-- 最近考试 -->
        <el-card shadow="never" style="margin-top: 16px" v-if="isStudent">
          <template #header>最近考试</template>
          <el-table :data="recentExams" stripe v-loading="recentLoading">
            <el-table-column prop="examTitle" label="考试名称" min-width="160" />
            <el-table-column prop="totalScore" label="得分" width="80" />
            <el-table-column label="排名" width="160">
              <template #default="{ row }">
                <span :class="rankClass(row)">{{ row.rank }} / {{ row.totalStudents }}</span>
              </template>
            </el-table-column>
            <el-table-column label="日期" width="100">
              <template #default="{ row }">
                {{ row.submittedAt ? row.submittedAt.substring(0, 10) : '-' }}
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="recentExams.length === 0 && !recentLoading" description="暂无考试成绩" />
        </el-card>

        <p v-if="!isStudent" class="no-student-hint">
          学生账号可查看学习统计和学科分析。
        </p>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { getProfile, uploadAvatar } from '@/api/profile'
import { getStudentOverview, getStudentSubjects, getStudentRecentExams } from '@/api/student'
import { getStudentWeakness } from '@/api/stats'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import * as echarts from 'echarts'

const userStore = useUserStore()
const userInfo = ref<any>(null)
const uploading = ref(false)

const overview = ref<any>(null)
const subjects = ref<any[]>([])
const recentExams = ref<any[]>([])
const recentLoading = ref(false)
const radarRef = ref<HTMLElement | null>(null)
let radarChart: any = null

const isStudent = computed(() => userInfo.value?.userType === 'STUDENT')

const avatarUrl = computed(() => {
  if (userInfo.value?.avatar) {
    if (userInfo.value.avatar.startsWith('http')) return userInfo.value.avatar
    return userInfo.value.avatar
  }
  return ''
})

const overviewCards = computed(() => [
  { label: '考试次数', value: overview.value?.totalExams ?? '-' },
  { label: '平均得分', value: overview.value?.avgScore != null ? overview.value.avgScore : '-' },
  { label: '最高得分', value: overview.value?.bestScore != null ? overview.value.bestScore : '-' },
  { label: '通过率', value: overview.value?.passRate != null ? overview.value.passRate + '%' : '-' },
])

const weaknessData = ref<any[]>([])
const weaknessLoading = ref(false)

const strongSubjects = computed(() =>
  subjects.value.filter(s => s.accuracy >= 70).slice(0, 5)
)
const weakSubjects = computed(() =>
  subjects.value.filter(s => s.accuracy < 70).slice(0, 5)
)

async function fetchWeakness() {
  weaknessLoading.value = true
  try {
    const res: any = await getStudentWeakness()
    weaknessData.value = res.data || []
  } catch {
    weaknessData.value = []
  } finally {
    weaknessLoading.value = false
  }
}

function weaknessColor(accuracy: number) {
  if (accuracy >= 80) return '#67C23A'
  if (accuracy >= 60) return '#E6A23C'
  return '#F56C6C'
}

function weaknessLabel(level: string) {
  const map: Record<string, string> = { STRONG: '掌握良好', MEDIUM: '一般', WEAK: '薄弱' }
  return map[level] || level
}

function weaknessTag(level: string) {
  const map: Record<string, string> = { STRONG: 'success', MEDIUM: 'warning', WEAK: 'danger' }
  return map[level] || ''
}

function rankClass(row: any) {
  if (row.rank === 1) return 'rank-first'
  if (row.rank <= row.totalStudents * 0.3) return 'rank-good'
  return ''
}

async function fetchProfile() {
  try {
    const res: any = await getProfile()
    userInfo.value = res.data
    userStore.setInfo(res.data)
  } catch {}
}

async function fetchOverview() {
  try {
    const res: any = await getStudentOverview()
    overview.value = res.data
  } catch {}
}

async function fetchSubjects() {
  try {
    const res: any = await getStudentSubjects()
    subjects.value = res.data || []
    renderRadar()
  } catch {}
}

async function fetchRecentExams() {
  recentLoading.value = true
  try {
    const res: any = await getStudentRecentExams(5)
    recentExams.value = res.data || []
  } finally {
    recentLoading.value = false
  }
}

async function handleAvatarUpload(file: File) {
  uploading.value = true
  try {
    const res: any = await uploadAvatar(file)
    userInfo.value.avatar = '/api/users/avatar-file/' + res.data
    ElMessage.success('头像已更新')
  } catch {
    ElMessage.error('头像上传失败')
  } finally {
    uploading.value = false
  }
  return false
}

function subjectColor(accuracy: number) {
  if (accuracy >= 80) return '#67C23A'
  if (accuracy >= 60) return '#E6A23C'
  return '#F56C6C'
}

function renderRadar() {
  if (!radarRef.value || subjects.value.length === 0) return
  nextTick(() => {
    if (radarChart) radarChart.dispose()
    radarChart = echarts.init(radarRef.value)
    radarChart.setOption({
      radar: {
        indicator: subjects.value.map(s => ({ name: s.subjectName, max: 100 })),
        shape: 'polygon',
        splitArea: { areaStyle: { color: ['rgba(64,158,255,0.02)', 'rgba(64,158,255,0.05)'] } },
        axisLine: { lineStyle: { color: 'rgba(0,0,0,0.1)' } },
        splitLine: { lineStyle: { color: 'rgba(0,0,0,0.1)' } },
      },
      series: [{
        type: 'radar',
        data: [{ value: subjects.value.map(s => s.accuracy), name: '得分率', areaStyle: { color: 'rgba(64,158,255,0.2)' }, lineStyle: { color: '#409EFF', width: 2 }, itemStyle: { color: '#409EFF' } }],
        symbol: 'circle',
        symbolSize: 6,
      }],
    })
  })
}

function typeLabel(type: string) {
  const map: Record<string, string> = { SUPER_ADMIN: '超级管理员', TENANT_ADMIN: '管理员', TEACHER: '教师', STUDENT: '学生' }
  return map[type] || type
}
function typeTag(type: string) {
  const map: Record<string, string> = { SUPER_ADMIN: 'danger', TENANT_ADMIN: 'warning', TEACHER: 'primary', STUDENT: 'success' }
  return map[type] || ''
}

onMounted(() => {
  fetchProfile().then(() => {
    if (isStudent.value) {
      fetchOverview()
      fetchSubjects()
      fetchRecentExams()
      fetchWeakness()
    }
  })
})
</script>

<style scoped>
.profile-page {
  max-width: 1200px;
}
.user-info-card {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 24px;
}
.avatar-actions {
  margin-top: 12px;
  text-align: center;
}
.avatar-hint {
  font-size: 12px;
  color: #999;
  margin-top: 6px;
}
.info-items {
  width: 100%;
}
.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}
.info-item:last-child {
  border-bottom: none;
}
.info-item .label {
  color: #999;
  font-size: 14px;
}
.info-item .value {
  color: #333;
  font-size: 14px;
}
.stat-card {
  text-align: center;
  padding: 16px 0;
  background: #f9f9f9;
  border-radius: 8px;
}
.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #409EFF;
}
.stat-label {
  font-size: 13px;
  color: #999;
  margin-top: 4px;
}
.subject-section {
  margin-bottom: 8px;
}
.subject-group-label {
  font-size: 14px;
  font-weight: 500;
  color: #666;
  margin-bottom: 8px;
}
.subject-tags {
  display: flex;
  flex-wrap: wrap;
}
.no-student-hint {
  text-align: center;
  color: #999;
  margin-top: 40px;
}
.subject-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.sb-label {
  width: 60px;
  font-size: 13px;
  color: #606266;
  text-align: right;
  flex-shrink: 0;
}
.sb-track {
  flex: 1;
  height: 16px;
  background: #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
}
.sb-fill {
  height: 100%;
  border-radius: 8px;
  transition: width 0.6s ease;
}
.sb-value {
  width: 40px;
  font-size: 13px;
  font-weight: 500;
}
:deep(.rank-first) {
  color: #f56c6c;
  font-weight: bold;
}
:deep(.rank-good) {
  color: #67c23a;
}
.weakness-subject {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}
.weakness-subject:last-child {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}
.weakness-subject-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.weakness-subject-name {
  font-weight: 600;
  font-size: 15px;
  color: #303133;
}
.weakness-subject-accuracy {
  font-size: 14px;
  font-weight: 500;
}
.weakness-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.weakness-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}
.weakness-item-left {
  width: 160px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 6px;
}
.weakness-kp-name {
  font-size: 13px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 120px;
}
.weakness-kp-attempt {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}
.weakness-item-right {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
}
.weakness-bar-track {
  flex: 1;
  height: 12px;
  background: #f0f0f0;
  border-radius: 6px;
  overflow: hidden;
}
.weakness-bar-fill {
  height: 100%;
  border-radius: 6px;
  transition: width 0.6s ease;
}
.weakness-bar-value {
  width: 40px;
  font-size: 13px;
  font-weight: 500;
  text-align: right;
}
</style>
