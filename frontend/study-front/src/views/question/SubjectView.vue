<template>
  <div>
    <el-row justify="space-between" align="middle">
      <h3>科目管理</h3>
      <el-button type="primary" @click="openSubjectDialog(null)">新增科目</el-button>
    </el-row>

    <el-table :data="subjectList" border stripe style="margin-top: 16px" v-loading="loading">
      <el-table-column prop="name" label="科目名称" />
      <el-table-column prop="code" label="编码" />
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status ? 'success' : 'info'" size="small">{{ row.status ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="openSubjectDialog(row)">编辑</el-button>
          <el-button size="small" @click="openKpDialog(row)">知识点</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 科目对话框 -->
    <el-dialog v-model="subjectVisible" :title="isEditSubject ? '编辑科目' : '新增科目'" width="400px">
      <el-form ref="subjectFormRef" :model="subjectForm" :rules="subjectRules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="subjectForm.name" />
        </el-form-item>
        <el-form-item label="编码" prop="code">
          <el-input v-model="subjectForm.code" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="subjectForm.sort" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="subjectVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSubject">保存</el-button>
      </template>
    </el-dialog>

    <!-- 知识点对话框 -->
    <el-dialog v-model="kpVisible" :title="`知识点管理 - ${currentSubject?.name}`" width="600px">
      <el-tree
        :data="kpTree"
        :props="{ label: 'name', children: 'children' }"
        node-key="id"
        default-expand-all
        :expand-on-click-node="false"
      >
        <template #default="{ node, data }">
          <span style="flex: 1; display: flex; align-items: center; justify-content: space-between">
            <span>{{ data.name }}</span>
            <span>
              <el-button size="small" text @click="openKpItemDialog(data)">编辑</el-button>
              <el-button size="small" text type="primary" @click="addChildKp(data)">添加子级</el-button>
              <el-button size="small" text type="danger" @click="deleteKp(data.id)">删除</el-button>
            </span>
          </span>
        </template>
      </el-tree>
      <el-button size="small" style="margin-top: 12px" @click="addChildKp({ id: null })">添加根节点</el-button>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getSubjectList } from '@/api/question'
import request from '@/api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const subjectList = ref<any[]>([])

// Subject dialog
const subjectVisible = ref(false)
const isEditSubject = ref(false)
const subjectFormRef = ref()
const subjectForm = reactive({ id: null as number | null, name: '', code: '', sort: 0, tenantId: 0 })
const subjectRules = { name: [{ required: true, message: '请输入名称', trigger: 'blur' }] }

// Knowledge point dialog
const kpVisible = ref(false)
const currentSubject = ref<any>(null)
const kpTree = ref<any[]>([])
const kpForm = reactive({ id: null as number | null, name: '', parentId: null as number | null })

onMounted(() => fetchSubjects())

async function fetchSubjects() {
  loading.value = true
  try {
    const res: any = await getSubjectList(0)
    subjectList.value = res.data
  } finally { loading.value = false }
}

function openSubjectDialog(row: any) {
  isEditSubject.value = !!row
  Object.assign(subjectForm, row || { id: null, name: '', code: '', sort: 0, tenantId: 0 })
  subjectVisible.value = true
}

async function saveSubject() {
  if (isEditSubject.value) {
    await request.put('/subjects', subjectForm)
  } else {
    await request.post('/subjects', subjectForm)
  }
  ElMessage.success(isEditSubject.value ? '更新成功' : '创建成功')
  subjectVisible.value = false
  fetchSubjects()
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确定删除该科目吗？', '提示')
  await request.delete(`/subjects/${id}`)
  ElMessage.success('删除成功')
  fetchSubjects()
}

async function openKpDialog(subject: any) {
  currentSubject.value = subject
  kpVisible.value = true
  const res: any = await request.get('/knowledge-points/tree', { params: { subjectId: subject.id, tenantId: 0 } })
  kpTree.value = res.data
}

async function addChildKp(data: any) {
  const parentId = data?.id || null
  const name = prompt('请输入知识点名称：')
  if (!name) return
  await request.post('/knowledge-points', {
    name, subjectId: currentSubject.value.id, parentId, tenantId: 0
  })
  ElMessage.success('创建成功')
  const res: any = await request.get('/knowledge-points/tree', { params: { subjectId: currentSubject.value.id, tenantId: 0 } })
  kpTree.value = res.data
}

async function deleteKp(id: number) {
  await ElMessageBox.confirm('确定删除该知识点（含子节点）吗？', '提示')
  await request.delete(`/knowledge-points/${id}`)
  ElMessage.success('删除成功')
  const res: any = await request.get('/knowledge-points/tree', { params: { subjectId: currentSubject.value.id, tenantId: 0 } })
  kpTree.value = res.data
}
</script>
