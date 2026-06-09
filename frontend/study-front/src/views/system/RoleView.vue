<template>
  <div>
    <el-row justify="space-between" align="middle">
      <h3>角色管理</h3>
      <el-button type="primary" @click="openDialog(null)">新增角色</el-button>
    </el-row>

    <el-table :data="roleList" border stripe style="margin-top: 20px" v-loading="loading">
      <el-table-column prop="name" label="角色名称" width="150" />
      <el-table-column prop="code" label="角色编码" width="150" />
      <el-table-column prop="description" label="描述" />
      <el-table-column prop="isSystem" label="系统内置" width="100">
        <template #default="{ row }">
          <el-tag :type="row.isSystem ? 'danger' : 'info'" size="small">
            {{ row.isSystem ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="340" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" @click="openPermissionDialog(row)">权限</el-button>
          <el-button size="small" @click="openUserDialog(row)">用户</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.id)" :disabled="row.isSystem">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑角色' : '新增角色'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="角色名" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="编码" prop="code">
          <el-input v-model="form.code" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="permDialogVisible" title="配置权限" width="520px">
      <el-tree
        ref="permTreeRef"
        :data="permTree"
        show-checkbox
        node-key="id"
        :props="{ label: 'name', children: 'children' }"
        default-expand-all
      />
      <template #footer>
        <el-button @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSavePermissions" :loading="permSaving">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="userDialogVisible" title="授权用户" width="600px">
      <el-transfer
        v-model="selectedUserIds"
        :data="allUsers"
        :titles="['未授权', '已授权']"
        filterable
        filter-placeholder="搜索用户"
      />
      <template #footer>
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveUsers" :loading="userSaving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'
import { getRolePermissions, assignRolePermissions } from '@/api/role'
import type { ElTree } from 'element-plus'

const loading = ref(false)
const saving = ref(false)
const roleList = ref<any[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()

const permDialogVisible = ref(false)
const permSaving = ref(false)
const permTreeRef = ref<InstanceType<typeof ElTree>>()
const permTree = ref<any[]>([])
const currentRoleId = ref<number | null>(null)

const userDialogVisible = ref(false)
const userSaving = ref(false)
const allUsers = ref<any[]>([])
const selectedUserIds = ref<number[]>([])

const form = reactive({
  id: null as number | null,
  name: '',
  code: '',
  description: '',
})

const rules = {
  name: [{ required: true, message: '请输入角色名', trigger: 'blur' }],
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
}

onMounted(() => fetchRoles())

async function fetchRoles() {
  loading.value = true
  try {
    const res: any = await request.get('/roles/page', { params: { page: 1, size: 100, tenantId: 0 } })
    roleList.value = res.data.list
  } finally {
    loading.value = false
  }
}

function openDialog(row: any) {
  isEdit.value = !!row
  Object.assign(form, row || { id: null, name: '', code: '', description: '' })
  dialogVisible.value = true
}

async function handleSave() {
  saving.value = true
  try {
    if (isEdit.value) {
      await request.put('/roles', form)
    } else {
      await request.post('/roles', form)
    }
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
    fetchRoles()
  } finally {
    saving.value = false
  }
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确定删除该角色吗？', '提示')
  await request.delete(`/roles/${id}`)
  ElMessage.success('删除成功')
  fetchRoles()
}

async function openPermissionDialog(row: any) {
  currentRoleId.value = row.id
  try {
    const res: any = await request.get('/permissions/list')
    permTree.value = buildPermTree(res.data || [])
  } catch {}
  try {
    const res: any = await getRolePermissions(row.id)
    const checkedIds = res.data || []
    await nextTick()
    permTreeRef.value?.setCheckedKeys(checkedIds)
  } catch {}
  permDialogVisible.value = true
}

function buildPermTree(perms: any[]): any[] {
  if (!perms.length) return []
  const roots = perms.filter(p => !p.parentId || p.parentId === 0)
  return roots.map(root => ({
    ...root,
    children: perms.filter(p => p.parentId === root.id).map(child => ({
      ...child,
      children: perms.filter(p => p.parentId === child.id),
    })),
  }))
}

async function handleSavePermissions() {
  if (!currentRoleId.value) return
  permSaving.value = true
  try {
    const checkedIds = (permTreeRef.value?.getCheckedKeys() || []) as number[]
    const halfCheckedIds = (permTreeRef.value?.getHalfCheckedKeys() || []) as number[]
    await assignRolePermissions(currentRoleId.value, [...checkedIds, ...halfCheckedIds])
    ElMessage.success('权限配置成功')
    permDialogVisible.value = false
  } finally {
    permSaving.value = false
  }
}

async function openUserDialog(row: any) {
  currentRoleId.value = row.id
  selectedUserIds.value = []
  // 加载所有用户
  try {
    const res: any = await request.get('/users/list', { params: { tenantId: 0 } })
    allUsers.value = (res.data || []).map((u: any) => ({
      key: u.id,
      label: `${u.realName || u.username} (${u.username})`,
    }))
  } catch {}
  // 加载已授权的用户ID
  try {
    const res: any = await request.get(`/roles/${row.id}/users`)
    selectedUserIds.value = res.data || []
  } catch {}
  userDialogVisible.value = true
}

async function handleSaveUsers() {
  if (!currentRoleId.value) return
  userSaving.value = true
  try {
    await request.put(`/roles/${currentRoleId.value}/users`, { userIds: selectedUserIds.value })
    ElMessage.success('用户授权成功')
    userDialogVisible.value = false
  } finally {
    userSaving.value = false
  }
}

function nextTick() {
  return new Promise(resolve => setTimeout(resolve, 0))
}
</script>
