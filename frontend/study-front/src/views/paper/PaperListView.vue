<template>
  <div>
    <el-row justify="space-between" align="middle">
      <h3>试卷列表</h3>
      <el-button type="primary" @click="router.push('/papers/create')">创建试卷</el-button>
    </el-row>

    <el-card style="margin-top: 16px">
      <el-form :inline="true" :model="query">
        <el-form-item label="标题">
          <el-input v-model="query.title" placeholder="搜索标题" clearable style="width: 160px" />
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
          <el-button type="primary" @click="fetchPapers">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-table :data="paperList" border stripe style="margin-top: 16px" v-loading="loading">
      <el-table-column prop="title" label="标题" min-width="200" />
      <el-table-column prop="totalScore" label="总分" width="80" />
      <el-table-column prop="durationMinutes" label="时长(分钟)" width="100" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'" size="small">
            {{ row.status === 'PUBLISHED' ? '已发布' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="router.push(`/papers/${row.id}`)">详情</el-button>
          <el-button size="small" @click="editPaper(row.id)">编辑</el-button>
          <el-button size="small" :type="row.status === 'PUBLISHED' ? 'warning' : 'success'"
            @click="toggleStatus(row)">
            {{ row.status === 'PUBLISHED' ? '下架' : '发布' }}
          </el-button>
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
      @change="fetchPapers"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getPaperPage, deletePaper, updatePaperStatus } from '@/api/paper'
import { getSubjectList } from '@/api/question'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const paperList = ref<any[]>([])
const total = ref(0)
const subjects = ref<any[]>([])

const query = reactive({ page: 1, size: 10, title: undefined, subjectId: undefined, status: undefined, tenantId: 0 })

onMounted(() => { fetchSubjects(); fetchPapers() })

async function fetchSubjects() {
  try {
    const res: any = await getSubjectList(0)
    subjects.value = res.data
  } catch {}
}

async function fetchPapers() {
  loading.value = true
  try {
    const res: any = await getPaperPage(query)
    paperList.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  Object.assign(query, { page: 1, title: undefined, subjectId: undefined, status: undefined })
  fetchPapers()
}

function editPaper(id: number) {
  router.push(`/papers/create?id=${id}`)
}

async function toggleStatus(row: any) {
  const newStatus = row.status === 'PUBLISHED' ? 'DRAFT' : 'PUBLISHED'
  await updatePaperStatus(row.id, newStatus)
  ElMessage.success(newStatus === 'PUBLISHED' ? '已发布' : '已下架')
  fetchPapers()
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确定删除该试卷吗？', '提示')
  await deletePaper(id)
  ElMessage.success('删除成功')
  fetchPapers()
}
</script>
