<template>
  <div>
    <el-row justify="space-between" align="middle">
      <h3>用户管理</h3>
      <el-button type="primary" @click="openDialog(null)">新增用户</el-button>
    </el-row>

    <el-table :data="userList" border stripe style="margin-top: 20px" v-loading="loading">
      <el-table-column prop="username" label="用户名" width="150" />
      <el-table-column prop="realName" label="姓名" width="120" />
      <el-table-column prop="email" label="邮箱" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="userType" label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="typeTag(row.userType)" size="small">
            {{ typeLabel(row.userType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status ? 'success' : 'danger'" size="small">
            {{ row.status ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
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
      @change="fetchUsers"
    />

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!isEdit">
          <el-input v-model="form.password" type="password" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="类型" prop="userType">
          <el-select v-model="form.userType" style="width: 100%">
            <el-option label="管理员" value="TENANT_ADMIN" />
            <el-option label="教师" value="TEACHER" />
            <el-option label="学生" value="STUDENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple style="width: 100%" placeholder="选择角色">
            <el-option v-for="r in roleList" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getUserPage, createUser, updateUser, deleteUser } from '@/api/user'
import { getRoleList, assignUserRoles, getUserRoles } from '@/api/role'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const loading = ref(false)
const saving = ref(false)
const userList = ref<any[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const roleList = ref<any[]>([])

const query = reactive({ page: 1, size: 10, tenantId: 0 })

const form = reactive({
  id: null as number | null,
  username: '',
  password: '',
  realName: '',
  email: '',
  phone: '',
  userType: 'TEACHER',
  status: 1,
  tenantId: 0,
  roleIds: [] as number[],
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  userType: [{ required: true, message: '请选择类型', trigger: 'change' }],
}

onMounted(() => {
  fetchUsers()
  fetchRoles()
})

async function fetchRoles() {
  try {
    const res: any = await getRoleList(0)
    roleList.value = res.data
  } catch {}
}

async function fetchUsers() {
  loading.value = true
  try {
    const res: any = await getUserPage(query)
    userList.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function openDialog(row: any) {
  isEdit.value = !!row
  if (row) {
    Object.assign(form, row, { password: '', roleIds: [] })
    // 加载已分配的角色
    try {
      const res: any = await getUserRoles(row.id)
      form.roleIds = res.data || []
    } catch {}
  } else {
    Object.assign(form, { id: null, username: '', password: '', realName: '', email: '', phone: '', userType: 'TEACHER', status: 1, tenantId: 0, roleIds: [] })
  }
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (isEdit.value) {
      await updateUser(form)
      // 保存角色分配
      await assignUserRoles(form.id!, form.roleIds)
      ElMessage.success('更新成功')
    } else {
      const res: any = await createUser(form)
      // 新建后需要获取用户ID再分配角色
      if (form.roleIds.length > 0) {
        // createUser 不返回ID，需刷新列表后再分配角色
        await fetchUsers()
        const created = userList.value.find(u => u.username === form.username)
        if (created) {
          await assignUserRoles(created.id, form.roleIds)
        }
      }
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchUsers()
  } finally {
    saving.value = false
  }
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确定删除该用户吗？', '提示')
  await deleteUser(id)
  ElMessage.success('删除成功')
  fetchUsers()
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
