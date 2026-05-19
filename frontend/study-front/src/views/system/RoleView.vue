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
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'

const loading = ref(false)
const saving = ref(false)
const roleList = ref<any[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()

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
</script>
