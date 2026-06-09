<template>
  <div>
    <el-row justify="space-between" align="middle">
      <h3>账户管理</h3>
      <el-button type="primary" @click="openCreateDialog">新建账号</el-button>
    </el-row>

    <el-card shadow="never" style="margin-top: 16px">
      <el-form :model="search" layout="inline" size="small">
        <el-row :gutter="12">
          <el-col :span="6">
            <el-input v-model="search.keyword" placeholder="用户名 / 姓名" clearable @change="fetchAccounts" />
          </el-col>
          <el-col :span="4">
            <el-select v-model="search.userType" placeholder="账户类型" clearable style="width:100%" @change="fetchAccounts">
              <el-option label="管理员" value="TENANT_ADMIN" />
              <el-option label="教师" value="TEACHER" />
              <el-option label="学生" value="STUDENT" />
            </el-select>
          </el-col>
          <el-col :span="4">
            <el-select v-model="search.status" placeholder="账户状态" clearable style="width:100%" @change="fetchAccounts">
              <el-option label="启用" :value="1" />
              <el-option label="禁用" :value="0" />
            </el-select>
          </el-col>
          <el-col :span="2">
            <el-button type="primary" @click="fetchAccounts">搜索</el-button>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <el-table :data="accountList" border stripe style="margin-top: 16px" v-loading="loading">
      <el-table-column prop="username" label="用户名" min-width="140" />
      <el-table-column prop="realName" label="姓名" min-width="140" />
      <el-table-column prop="userType" label="类型" min-width="110">
        <template #default="{ row }">
          <el-tag :type="typeTag(row.userType)" size="small">{{ typeLabel(row.userType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" min-width="170">
        <template #default="{ row }">
          <el-switch
            :model-value="row.status === 1"
            :loading="row._toggling"
            @change="toggleStatus(row)"
            active-text="启用"
            inactive-text="禁用"
          />
        </template>
      </el-table-column>
      <el-table-column prop="email" label="邮箱" min-width="160" />
      <el-table-column prop="phone" label="手机号" min-width="140" />
      <el-table-column label="操作" width="310" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openResetPwdDialog(row)">重置密码</el-button>
          <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
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
      @change="fetchAccounts"
    />

    <el-dialog v-model="createDialogVisible" title="新建账号" width="500px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="createForm.username" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="createForm.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="createForm.realName" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="createForm.email" />
        </el-form-item>
        <el-form-item label="类型" prop="userType">
          <el-select v-model="createForm.userType" style="width: 100%">
            <el-option label="管理员" value="TENANT_ADMIN" />
            <el-option label="教师" value="TEACHER" />
            <el-option label="学生" value="STUDENT" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="creating">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editDialogVisible" title="编辑账号" width="500px">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="editForm.username" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="editForm.realName" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="editForm.email" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="editForm.phone" />
        </el-form-item>
        <el-form-item label="类型" prop="userType">
          <el-select v-model="editForm.userType" style="width: 100%">
            <el-option label="管理员" value="TENANT_ADMIN" />
            <el-option label="教师" value="TEACHER" />
            <el-option label="学生" value="STUDENT" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleEdit" :loading="editing">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resetPwdDialogVisible" title="重置密码" width="400px">
      <el-form ref="resetPwdFormRef" :model="resetPwdForm" :rules="resetPwdRules" label-width="80px">
        <el-form-item label="账号">
          <span>{{ resetPwdAccount }}</span>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="resetPwdForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="resetPwdForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPwdDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleResetPwd" :loading="resetting">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import request from '@/api/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const loading = ref(false)
const accountList = ref<any[]>([])
const total = ref(0)

const query = reactive({ page: 1, size: 10, tenantId: 0 })
const search = reactive({ keyword: '', userType: '', status: '' })

// --- 新建账号 ---
const createDialogVisible = ref(false)
const creating = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive({
  username: '', password: '', realName: '', email: '', userType: 'TEACHER', tenantId: 0,
})
const createRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码至少6位', trigger: 'blur' }],
  userType: [{ required: true, message: '请选择类型', trigger: 'change' }],
}

// --- 编辑账号 ---
const editDialogVisible = ref(false)
const editing = ref(false)
const editFormRef = ref<FormInstance>()
const editForm = reactive({
  id: null as number | null, username: '', realName: '', email: '', phone: '', userType: '', tenantId: 0,
})
const editRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  userType: [{ required: true, message: '请选择类型', trigger: 'change' }],
}

// --- 重置密码 ---
const resetPwdDialogVisible = ref(false)
const resetting = ref(false)
const resetPwdAccount = ref('')
const resetPwdFormRef = ref<FormInstance>()
const resetPwdForm = reactive({ newPassword: '', confirmPassword: '' })
const resetPwdUserId = ref<number | null>(null)
const resetPwdRules: FormRules = {
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { min: 6, message: '密码至少6位', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: (_, v, cb) => v === resetPwdForm.newPassword ? cb() : cb(new Error('两次密码不一致')), trigger: 'blur' },
  ],
}

onMounted(() => fetchAccounts())

async function fetchAccounts() {
  loading.value = true
  try {
    const params: any = { page: query.page, size: query.size, tenantId: 0 }
    if (search.keyword) params.keyword = search.keyword
    if (search.userType) params.userType = search.userType
    if (search.status !== '') params.status = search.status
    const res: any = await request.get('/users/page', { params })
    accountList.value = (res.data.list || []).map((u: any) => ({ ...u, _toggling: false }))
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

// --- 新建 ---
function openCreateDialog() {
  Object.assign(createForm, { username: '', password: '', realName: '', email: '', userType: 'TEACHER', tenantId: 0 })
  createDialogVisible.value = true
}

async function handleCreate() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return
  creating.value = true
  try {
    await request.post('/users', createForm)
    ElMessage.success('账号创建成功')
    createDialogVisible.value = false
    fetchAccounts()
  } finally {
    creating.value = false
  }
}

// --- 编辑 ---
function openEditDialog(row: any) {
  Object.assign(editForm, { id: row.id, username: row.username, realName: row.realName, email: row.email, phone: row.phone, userType: row.userType, tenantId: 0 })
  editDialogVisible.value = true
}

async function handleEdit() {
  const valid = await editFormRef.value?.validate().catch(() => false)
  if (!valid) return
  editing.value = true
  try {
    await request.put('/users', editForm)
    ElMessage.success('账号信息已更新')
    editDialogVisible.value = false
    fetchAccounts()
  } finally {
    editing.value = false
  }
}

// --- 重置密码 ---
function openResetPwdDialog(row: any) {
  resetPwdUserId.value = row.id
  resetPwdAccount.value = `${row.username} (${row.realName || '-'})`
  resetPwdForm.newPassword = ''
  resetPwdForm.confirmPassword = ''
  resetPwdDialogVisible.value = true
}

async function handleResetPwd() {
  const valid = await resetPwdFormRef.value?.validate().catch(() => false)
  if (!valid || !resetPwdUserId.value) return
  resetting.value = true
  try {
    await request.put(`/users/${resetPwdUserId.value}/password`, resetPwdForm.newPassword, {
      headers: { 'Content-Type': 'application/json' },
    })
    ElMessage.success('密码已重置')
    resetPwdDialogVisible.value = false
  } finally {
    resetting.value = false
  }
}

// --- 启用/禁用 ---
async function toggleStatus(row: any) {
  row._toggling = true
  try {
    await request.put('/users', { id: row.id, status: row.status === 1 ? 0 : 1 })
    row.status = row.status === 1 ? 0 : 1
    ElMessage.success(row.status ? '账号已启用' : '账号已禁用')
  } finally {
    row._toggling = false
  }
}

// --- 删除 ---
async function handleDelete(id: number) {
  await ElMessageBox.confirm('确定删除该账号吗？此操作不可恢复。', '提示', { confirmButtonText: '删除', type: 'warning' })
  await request.delete(`/users/${id}`)
  ElMessage.success('账号已删除')
  fetchAccounts()
}

function typeLabel(type: string) {
  const map: Record<string, string> = { SUPER_ADMIN: '超级管理员', TENANT_ADMIN: '管理员', TEACHER: '教师', STUDENT: '学生' }
  return map[type] || type
}
function typeTag(type: string) {
  const map: Record<string, string> = { SUPER_ADMIN: 'danger', TENANT_ADMIN: 'warning', TEACHER: 'primary', STUDENT: 'success' }
  return map[type] || ''
}
</script>
