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

          <div class="subject-section" v-if="strongSubjects.length > 0">
            <div class="subject-group-label">擅长学科</div>
            <div class="subject-tags">
              <el-tag
                v-for="s in strongSubjects"
                :key="s.subjectId"
                type="success"
                size="large"
                style="margin: 0 8px 8px 0"
              >
                {{ s.subjectName }} ({{ s.accuracy }}%)
              </el-tag>
            </div>
          </div>

          <div class="subject-section" v-if="weakSubjects.length > 0" style="margin-top: 12px">
            <div class="subject-group-label">薄弱学科</div>
            <div class="subject-tags">
              <el-tag
                v-for="s in weakSubjects"
                :key="s.subjectId"
                type="danger"
                size="large"
                style="margin: 0 8px 8px 0"
              >
                {{ s.subjectName }} ({{ s.accuracy }}%)
              </el-tag>
            </div>
          </div>

          <el-empty v-if="subjects.length === 0" description="暂无学科数据" />
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
import { ref, computed, onMounted } from 'vue'
import { getProfile, uploadAvatar } from '@/api/profile'
import { getStudentOverview, getStudentSubjects, getStudentRecentExams } from '@/api/student'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const userInfo = ref<any>(null)
const uploading = ref(false)

const overview = ref<any>(null)
const subjects = ref<any[]>([])
const recentExams = ref<any[]>([])
const recentLoading = ref(false)

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

const strongSubjects = computed(() =>
  subjects.value.filter(s => s.accuracy >= 70).slice(0, 5)
)
const weakSubjects = computed(() =>
  subjects.value.filter(s => s.accuracy < 70).slice(0, 5)
)

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
:deep(.rank-first) {
  color: #f56c6c;
  font-weight: bold;
}
:deep(.rank-good) {
  color: #67c23a;
}
</style>
