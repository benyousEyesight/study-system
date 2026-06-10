<template>
  <div v-loading="loading">
    <h3>{{ isEdit ? '编辑考试' : '创建考试' }}</h3>

    <el-card style="margin-top: 16px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="考试名称" required>
          <el-input v-model="form.title" placeholder="如：期中数学考试" />
        </el-form-item>
        <el-form-item label="关联试卷" required>
          <el-select v-model="form.paperId" placeholder="选择试卷" style="width: 100%">
            <el-option v-for="p in papers" :key="p.id" :label="p.title" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="考试说明">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="时间模式" required>
          <el-radio-group v-model="form.timeMode">
            <el-radio value="FIXED_WINDOW">固定时段</el-radio>
            <el-radio value="FLEXIBLE">灵活时长</el-radio>
            <el-radio value="BOTH">两者结合</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-row :gutter="20" v-if="form.timeMode !== 'FLEXIBLE'">
          <el-col :span="12">
            <el-form-item label="开始时间" required>
              <el-date-picker v-model="form.startTime" type="datetime" placeholder="选择开始时间" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间" required>
              <el-date-picker v-model="form.endTime" type="datetime" placeholder="选择结束时间" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="作答时长(分钟)" v-if="form.timeMode !== 'FIXED_WINDOW'">
          <el-input-number v-model="form.durationMinutes" :min="0" style="width: 200px" />
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 16px">
      <template #header><span>分配考生</span></template>
      <el-radio-group v-model="assignMode" style="margin-bottom: 12px">
        <el-radio value="USER">按用户</el-radio>
        <el-radio value="ROLE">按角色</el-radio>
        <el-radio value="CLASS">按班级</el-radio>
        <el-radio value="EXAM_CODE">考试码</el-radio>
      </el-radio-group>

      <div v-if="assignMode === 'USER'">
        <el-button size="small" type="primary" @click="showUserSelector">选择用户</el-button>
        <el-tag v-for="u in selectedUsers" :key="u.id" closable @close="removeUser(u)" style="margin-left: 8px">{{ u.realName || u.username }}</el-tag>
      </div>
      <div v-if="assignMode === 'ROLE'">
        <el-select v-model="selectedRoles" multiple placeholder="选择角色" style="width: 300px">
          <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
        </el-select>
      </div>
      <div v-if="assignMode === 'CLASS'">
        <el-select v-model="selectedClasses" multiple placeholder="选择班级" style="width: 100%">
          <el-option-group v-for="g in gradeList" :key="g.id" :label="g.name">
            <el-option v-for="c in (classesByGrade[g.id] || [])" :key="c.id" :label="c.name" :value="c" />
          </el-option-group>
        </el-select>
        <div style="color:#909399;font-size:12px;margin-top:6px">将分配给学生名单中所有成员</div>
      </div>
      <div v-if="assignMode === 'EXAM_CODE'">
        <el-input v-model="form.examCode" placeholder="输入考试码，留空自动生成" style="width: 300px" />
      </div>
    </el-card>

    <el-card style="margin-top: 16px">
      <template #header><span>防作弊设置</span></template>
      <el-form label-width="120px">
        <el-form-item label="切屏检测">
          <el-switch v-model="antiCheatEnabled" />
        </el-form-item>
        <el-form-item label="允许切屏次数" v-if="antiCheatEnabled">
          <el-input-number v-model="form.maxTabSwitches" :min="1" :max="10" />
        </el-form-item>
      </el-form>
    </el-card>

    <div style="margin-top: 16px; text-align: center">
      <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      <el-button @click="router.back()">取消</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { createExam, updateExam, getExamById } from '@/api/exam'
import { getPaperPage } from '@/api/paper'
import { getGradeList, getClazzList, getClazzStudents } from '@/api/org'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const isEdit = ref(false)
const loading = ref(false)
const saving = ref(false)
const papers = ref<any[]>([])
const roles = ref<any[]>([])
const selectedUsers = ref<any[]>([])
const selectedRoles = ref<number[]>([])
const selectedClasses = ref<any[]>([])
const gradeList = ref<any[]>([])
const classesByGrade = ref<Record<number, any[]>>({})
const assignMode = ref('EXAM_CODE')
const antiCheatEnabled = ref(false)

const form = reactive({
  paperId: undefined as number | undefined,
  title: '',
  description: '',
  examCode: '',
  timeMode: 'FLEXIBLE',
  startTime: undefined,
  endTime: undefined,
  durationMinutes: 60,
  maxTabSwitches: 3,
  antiCheatConfig: '',
})

onMounted(async () => {
  const res: any = await getPaperPage({ page: 1, size: 999 })
  papers.value = res.data.records

  // 加载年级班级
  const gradeRes: any = await getGradeList(0)
  gradeList.value = gradeRes.data || []
  const clsRes: any = await getClazzList(0)
  const allClasses: any[] = (clsRes.data || []).filter((c: any) => c.status === 1)
  for (const c of allClasses) {
    if (!classesByGrade.value[c.gradeId]) classesByGrade.value[c.gradeId] = []
    classesByGrade.value[c.gradeId].push(c)
  }

  if (route.params.id) {
    isEdit.value = true
    loading.value = true
    try {
      const res: any = await getExamById(Number(route.params.id))
      const d = res.data
      Object.assign(form, { title: d.title, paperId: d.paperId, description: d.description || '',
        examCode: d.examCode || '', timeMode: d.timeMode, startTime: d.startTime,
        endTime: d.endTime, durationMinutes: d.durationMinutes, maxTabSwitches: d.maxTabSwitches || 3 })
    } finally { loading.value = false }
  }
})

async function handleSave() {
  if (!form.title || !form.paperId) { ElMessage.warning('请填写名称和关联试卷'); return }
  saving.value = true
  try {
    const payload: any = { ...form, maxTabSwitches: antiCheatEnabled.value ? form.maxTabSwitches : 0 }
    payload.assignments = []
    if (assignMode.value === 'USER') {
      for (const u of selectedUsers.value) payload.assignments.push({ assignType: 'USER', assigneeId: u.id })
    } else if (assignMode.value === 'ROLE') {
      for (const r of selectedRoles.value) payload.assignments.push({ assignType: 'ROLE', assigneeId: r })
    } else if (assignMode.value === 'CLASS') {
      const userIds = new Set<number>()
      for (const clz of selectedClasses.value) {
        const res: any = await getClazzStudents(clz.id)
        for (const s of (res.data || [])) {
          if (s.studentId && !userIds.has(s.studentId)) {
            userIds.add(s.studentId)
            payload.assignments.push({ assignType: 'USER', assigneeId: s.studentId })
          }
        }
      }
    }
    if (isEdit.value) {
      await updateExam(Number(route.params.id), payload)
      ElMessage.success('更新成功')
    } else {
      await createExam(payload)
      ElMessage.success('创建成功')
    }
    router.push('/exams')
  } catch {} finally { saving.value = false }
}

function showUserSelector() { /* TODO: 用户选择对话框 */ ElMessage.info('请通过角色或考试码分配') }
function removeUser(u: any) { selectedUsers.value = selectedUsers.value.filter((x: any) => x.id !== u.id) }
</script>
