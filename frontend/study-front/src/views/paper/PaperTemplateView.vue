<template>
  <div>
    <el-row justify="space-between" align="middle">
      <h3>组卷模板</h3>
      <el-button type="primary" @click="router.push('/paper-templates/create')">创建模板</el-button>
    </el-row>

    <el-card style="margin-top: 16px">
      <el-form :inline="true" :model="query">
        <el-form-item label="名称">
          <el-input v-model="query.name" placeholder="搜索名称" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="科目">
          <el-select v-model="query.subjectId" placeholder="选择科目" clearable style="width: 150px">
            <el-option v-for="s in subjects" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="选择状态" clearable style="width: 120px">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已发布" value="PUBLISHED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchTemplates">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-table :data="templateList" border stripe style="margin-top: 16px" v-loading="loading">
      <el-table-column prop="name" label="名称" min-width="200" />
      <el-table-column prop="totalScore" label="总分" width="80" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'" size="small">
            {{ row.status === 'PUBLISHED' ? '已发布' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170" />
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="router.push(`/paper-templates/${row.id}/edit`)">编辑</el-button>
          <el-button size="small" :type="row.status === 'PUBLISHED' ? 'warning' : 'success'"
            @click="toggleStatus(row)">
            {{ row.status === 'PUBLISHED' ? '下架' : '发布' }}
          </el-button>
          <el-button size="small" type="primary" @click="handleGenerate(row)">生成试卷</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="query.page"
      v-model:page-size="query.size"
      :total="total"
      layout="total, prev, pager, next"
      style="margin-top: 16px; justify-content: flex-end"
      @change="fetchTemplates"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getTemplatePage, deleteTemplate, generatePaper } from '@/api/paper'
import { getSubjectList } from '@/api/question'
import request from '@/api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const templateList = ref<any[]>([])
const total = ref(0)
const subjects = ref<any[]>([])

const query = reactive({ page: 1, size: 10, name: undefined, subjectId: undefined, status: undefined, tenantId: 0 })

onMounted(() => { fetchSubjects(); fetchTemplates() })

async function fetchSubjects() {
  try {
    const res: any = await getSubjectList(0)
    subjects.value = res.data
  } catch {}
}

async function fetchTemplates() {
  loading.value = true
  try {
    const res: any = await getTemplatePage(query)
    templateList.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  Object.assign(query, { page: 1, name: undefined, subjectId: undefined, status: undefined })
  fetchTemplates()
}

async function toggleStatus(row: any) {
  const newStatus = row.status === 'PUBLISHED' ? 'DRAFT' : 'PUBLISHED'
  await request.put(`/paper-templates/${row.id}/status?status=${newStatus}`)
  ElMessage.success(newStatus === 'PUBLISHED' ? '已发布' : '已下架')
  fetchTemplates()
}

async function handleGenerate(row: any) {
  await ElMessageBox.confirm(`确定从模板"${row.name}"生成试卷吗？`, '提示')
  await generatePaper({ templateId: row.id })
  ElMessage.success('试卷生成成功，请在试卷列表中查看')
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确定删除该模板吗？', '提示')
  await deleteTemplate(id)
  ElMessage.success('删除成功')
  fetchTemplates()
}
</script>
