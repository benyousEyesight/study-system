<template>
  <div>
    <el-row justify="space-between" align="middle">
      <h3>年级管理</h3>
      <el-button type="primary" @click="openDialog(null)">新增年级</el-button>
    </el-row>

    <el-table :data="gradeList" border stripe style="margin-top: 20px" v-loading="loading">
      <el-table-column prop="name" label="年级名称" min-width="200" />
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status ? 'success' : 'danger'" size="small">{{ row.status ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑年级' : '新增年级'" width="420px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="年级名称" prop="name">
          <el-input v-model="form.name" placeholder="如 高一、高二、高三" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" />
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
import { getGradeList, createGrade, updateGrade, deleteGrade } from '@/api/org'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const saving = ref(false)
const gradeList = ref<any[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()

const form = reactive({ id: null as number | null, name: '', sort: 0, status: 1, tenantId: 0 })
const rules = { name: [{ required: true, message: '请输入年级名称', trigger: 'blur' }] }

onMounted(() => fetchGrades())

async function fetchGrades() {
  loading.value = true
  try {
    const res: any = await getGradeList(0)
    gradeList.value = res.data || []
  } finally { loading.value = false }
}

function openDialog(row: any) {
  isEdit.value = !!row
  Object.assign(form, row || { id: null, name: '', sort: 0, status: 1, tenantId: 0 })
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (isEdit.value) {
      await updateGrade(form)
      ElMessage.success('更新成功')
    } else {
      await createGrade(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchGrades()
  } finally { saving.value = false }
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确定删除该年级吗？', '提示')
  await deleteGrade(id)
  ElMessage.success('已删除')
  fetchGrades()
}
</script>
