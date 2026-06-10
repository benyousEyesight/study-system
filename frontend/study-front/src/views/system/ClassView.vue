<template>
  <div>
    <el-row justify="space-between" align="middle">
      <h3>班级管理</h3>
      <el-button type="primary" @click="openDialog(null)">新增班级</el-button>
    </el-row>

    <!-- 筛选 -->
    <el-card shadow="never" style="margin-top: 16px">
      <el-form :model="search" layout="inline" size="small">
        <el-form-item label="年级">
          <el-select v-model="search.gradeId" placeholder="全部年级" clearable style="width: 160px" @change="fetchClazzes">
            <el-option v-for="g in gradeList" :key="g.id" :label="g.name" :value="g.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchClazzes">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-table :data="clazzList" border stripe style="margin-top: 16px" v-loading="loading">
      <el-table-column prop="gradeName" label="年级" width="100" />
      <el-table-column prop="name" label="班级名称" min-width="140" />
      <el-table-column prop="headTeacherName" label="班主任" width="120" />
      <el-table-column prop="sort" label="排序" width="70" />
      <el-table-column prop="status" label="状态" width="70">
        <template #default="{ row }">
          <el-tag :type="row.status ? 'success' : 'danger'" size="small">{{ row.status ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="340" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" @click="openStudentDialog(row)">学生</el-button>
          <el-button size="small" @click="openTeacherDialog(row)">教师</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="query.page"
      v-model:page-size="query.size"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      style="margin-top: 20px; justify-content: flex-end"
      @change="fetchClazzes"
    />

    <!-- 班级编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑班级' : '新增班级'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="所属年级" prop="gradeId">
          <el-select v-model="form.gradeId" style="width: 100%">
            <el-option v-for="g in gradeList" :key="g.id" :label="g.name" :value="g.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级名称" prop="name">
          <el-input v-model="form.name" placeholder="如 1班、3班" />
        </el-form-item>
        <el-form-item label="班主任">
          <el-select v-model="form.headTeacherId" clearable filterable style="width: 100%" placeholder="选择教师">
            <el-option v-for="u in teacherList" :key="u.id" :label="`${u.realName || u.username} (${u.username})`" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 学生管理对话框 -->
    <el-dialog v-model="studentDialogVisible" :title="`班级学生 - ${currentClazzName}`" width="700px">
      <div style="margin-bottom: 12px">
        <el-button size="small" type="primary" @click="openAddStudent()">添加学生</el-button>
      </div>
      <el-table :data="studentList" stripe v-loading="studentLoading">
        <el-table-column prop="studentName" label="姓名" min-width="120" />
        <el-table-column prop="studentUsername" label="用户名" width="120" />
        <el-table-column prop="academicYear" label="学年" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button size="small" type="danger" @click="handleRemoveStudent(row.id)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="studentList.length === 0 && !studentLoading" description="暂无学生" />

      <el-dialog v-model="showAddStudent" title="添加学生" width="500px" append-to-body>
        <el-transfer
          v-model="selectedStudentIds"
          :data="availableStudents"
          :titles="['可选学生', '已选学生']"
          filterable
          filter-placeholder="搜索学生"
        />
        <template #footer>
          <el-button @click="showAddStudent = false">取消</el-button>
          <el-button type="primary" @click="handleAddStudents" :loading="addingStudent">确认添加</el-button>
        </template>
      </el-dialog>
    </el-dialog>

    <!-- 教师管理对话框 -->
    <el-dialog v-model="teacherDialogVisible" :title="`任课教师 - ${currentClazzName}`" width="600px">
      <div style="margin-bottom: 12px">
        <el-button size="small" type="primary" @click="openAssignTeacher()">添加教师</el-button>
      </div>
      <el-table :data="teacherAssignmentList" stripe v-loading="teacherLoading">
        <el-table-column prop="teacherName" label="姓名" min-width="120" />
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button size="small" type="danger" @click="handleRemoveTeacher(row.id)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="teacherAssignmentList.length === 0 && !teacherLoading" description="暂无任课教师" />

      <el-dialog v-model="showAssignTeacher" title="添加教师" width="500px" append-to-body>
        <el-transfer
          v-model="selectedTeacherIds"
          :data="availableTeachers"
          :titles="['可选教师', '已选教师']"
          filterable
          filter-placeholder="搜索教师"
        />
        <template #footer>
          <el-button @click="showAssignTeacher = false">取消</el-button>
          <el-button type="primary" @click="handleAssignTeachers" :loading="assigningTeacher">确认添加</el-button>
        </template>
      </el-dialog>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import {
  getClazzPage, createClazz, updateClazz, deleteClazz,
  getGradeList, getClazzStudents, addClazzStudents, removeClazzStudent,
  getClazzTeachers, assignClazzTeacher, removeClazzTeacher,
} from '@/api/org'
import { getUserPage } from '@/api/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const saving = ref(false)
const total = ref(0)
const clazzList = ref<any[]>([])
const gradeList = ref<any[]>([])
const teacherList = ref<any[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()

const query = reactive({ page: 1, size: 10, tenantId: 0 })
const search = reactive({ gradeId: '' })

const form = reactive({
  id: null as number | null, gradeId: null as number | null, name: '',
  headTeacherId: null as number | null, sort: 0, status: 1, tenantId: 0,
})
const rules = {
  gradeId: [{ required: true, message: '请选择年级', trigger: 'change' }],
  name: [{ required: true, message: '请输入班级名称', trigger: 'blur' }],
}

// --- 学生管理 ---
const studentDialogVisible = ref(false)
const studentLoading = ref(false)
const currentClazzId = ref(0)
const currentClazzName = ref('')
const studentList = ref<any[]>([])
const showAddStudent = ref(false)
const selectedStudentIds = ref<number[]>([])
const availableStudents = ref<any[]>([])
const addingStudent = ref(false)

// --- 教师管理 ---
const teacherDialogVisible = ref(false)
const teacherLoading = ref(false)
const teacherAssignmentList = ref<any[]>([])
const showAssignTeacher = ref(false)
const selectedTeacherIds = ref<number[]>([])
const availableTeachers = ref<any[]>([])
const assigningTeacher = ref(false)

onMounted(() => {
  fetchGrades()
  fetchClazzes()
  fetchTeachers()
})

async function fetchGrades() {
  try { const res: any = await getGradeList(0); gradeList.value = res.data || [] } catch {}
}

async function fetchTeachers() {
  try {
    const res: any = await getUserPage({ page: 1, size: 999, tenantId: 0 })
    teacherList.value = (res.data.list || []).filter((u: any) => u.userType === 'TEACHER')
  } catch {}
}

async function fetchClazzes() {
  loading.value = true
  try {
    const params: any = { page: query.page, size: query.size, tenantId: 0 }
    if (search.gradeId) params.gradeId = search.gradeId
    const res: any = await getClazzPage(params)
    clazzList.value = res.data.list || []
    total.value = res.data.total
  } finally { loading.value = false }
}

function openDialog(row: any) {
  isEdit.value = !!row
  Object.assign(form, row || { id: null, gradeId: null, name: '', headTeacherId: null, sort: 0, status: 1, tenantId: 0 })
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (isEdit.value) { await updateClazz(form); ElMessage.success('更新成功') }
    else { await createClazz(form); ElMessage.success('创建成功') }
    dialogVisible.value = false
    fetchClazzes()
  } finally { saving.value = false }
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确定删除该班级吗？删除后学生和教师关联也会一并移除。', '提示')
  await deleteClazz(id)
  ElMessage.success('已删除')
  fetchClazzes()
}

// --- 学生 ---
async function openStudentDialog(row: any) {
  currentClazzId.value = row.id
  currentClazzName.value = `${row.gradeName || ''} ${row.name}`
  studentDialogVisible.value = true
  await loadStudents()
}

async function loadStudents() {
  studentLoading.value = true
  try {
    const res: any = await getClazzStudents(currentClazzId.value)
    studentList.value = res.data || []
  } finally { studentLoading.value = false }
}

async function openAddStudent() {
  selectedStudentIds.value = []
  try {
    const res: any = await getUserPage({ page: 1, size: 999, tenantId: 0 })
    const allStudents = (res.data.list || []).filter((u: any) => u.userType === 'STUDENT')
    const existingIds = new Set(studentList.value.map((s: any) => s.studentId))
    availableStudents.value = allStudents
      .filter((u: any) => !existingIds.has(u.id))
      .map((u: any) => ({ key: u.id, label: `${u.realName || u.username} (${u.username})` }))
  } catch {}
  showAddStudent.value = true
}

async function handleAddStudents() {
  if (selectedStudentIds.value.length === 0) return
  addingStudent.value = true
  try {
    await addClazzStudents(currentClazzId.value, selectedStudentIds.value)
    ElMessage.success(`已添加 ${selectedStudentIds.value.length} 名学生`)
    showAddStudent.value = false
    await loadStudents()
  } finally { addingStudent.value = false }
}

async function handleRemoveStudent(id: number) {
  await ElMessageBox.confirm('确定将该学生从班级移除吗？', '提示')
  await removeClazzStudent(currentClazzId.value, id)
  ElMessage.success('已移除')
  await loadStudents()
}

// --- 教师 ---
async function openTeacherDialog(row: any) {
  currentClazzId.value = row.id
  currentClazzName.value = `${row.gradeName || ''} ${row.name}`
  teacherDialogVisible.value = true
  await loadTeachers()
}

async function loadTeachers() {
  teacherLoading.value = true
  try {
    const res: any = await getClazzTeachers(currentClazzId.value)
    teacherAssignmentList.value = res.data || []
  } finally { teacherLoading.value = false }
}

async function openAssignTeacher() {
  selectedTeacherIds.value = []
  try {
    const existingIds = new Set(teacherAssignmentList.value.map((t: any) => t.teacherId))
    availableTeachers.value = teacherList.value
      .filter((u: any) => !existingIds.has(u.id))
      .map((u: any) => ({ key: u.id, label: `${u.realName || u.username} (${u.username})` }))
  } catch {}
  showAssignTeacher.value = true
}

async function handleAssignTeachers() {
  if (selectedTeacherIds.value.length === 0) return
  assigningTeacher.value = true
  try {
    for (const tid of selectedTeacherIds.value) {
      await assignClazzTeacher(currentClazzId.value, { teacherId: tid })
    }
    ElMessage.success('已添加教师')
    showAssignTeacher.value = false
    await loadTeachers()
  } finally { assigningTeacher.value = false }
}

async function handleRemoveTeacher(id: number) {
  await ElMessageBox.confirm('确定移除该教师吗？', '提示')
  await removeClazzTeacher(currentClazzId.value, id)
  ElMessage.success('已移除')
  await loadTeachers()
}
</script>
