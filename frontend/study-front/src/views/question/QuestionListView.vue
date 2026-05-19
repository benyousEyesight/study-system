<template>
  <div>
    <el-row justify="space-between" align="middle">
      <h3>题目列表</h3>
      <el-button type="primary" @click="router.push('/questions/create')">创建题目</el-button>
    </el-row>

    <el-card style="margin-top: 16px">
      <el-form :inline="true" :model="query">
        <el-form-item label="科目">
          <el-select v-model="query.subjectId" placeholder="选择科目" clearable style="width: 150px">
            <el-option v-for="s in subjects" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="题型">
          <el-select v-model="query.type" placeholder="选择题型" clearable style="width: 150px">
            <el-option label="单选题" value="SINGLE_CHOICE" />
            <el-option label="多选题" value="MULTIPLE_CHOICE" />
            <el-option label="判断题" value="TRUE_FALSE" />
            <el-option label="填空题" value="FILL_BLANK" />
            <el-option label="简答题" value="SHORT_ANSWER" />
            <el-option label="论述题" value="ESSAY" />
            <el-option label="组合题" value="COMPOSITE" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="选择状态" clearable style="width: 120px">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="已归档" value="ARCHIVED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchQuestions">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-table :data="questionList" border stripe style="margin-top: 16px" v-loading="loading">
      <el-table-column label="题目内容" min-width="300">
        <template #default="{ row }">
          <div class="question-content">{{ truncateContent(row.contentJson) }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="type" label="题型" width="100">
        <template #default="{ row }">{{ typeLabel(row.type) }}</template>
      </el-table-column>
      <el-table-column prop="difficulty" label="难度" width="80">
        <template #default="{ row }">
          <el-tag :type="diffTag(row.difficulty)" size="small">{{ row.difficulty }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'" size="small">
            {{ row.status === 'PUBLISHED' ? '已发布' : row.status === 'DRAFT' ? '草稿' : '已归档' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="editQuestion(row.id)">编辑</el-button>
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
      @change="fetchQuestions"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getQuestionPage, deleteQuestion, getSubjectList } from '@/api/question'
import request from '@/api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const questionList = ref<any[]>([])
const total = ref(0)
const subjects = ref<any[]>([])

const query = reactive({ page: 1, size: 10, subjectId: undefined, type: undefined, status: undefined, tenantId: 0 })

onMounted(() => { fetchSubjects(); fetchQuestions() })

async function fetchSubjects() {
  try {
    const res: any = await getSubjectList(0)
    subjects.value = res.data
  } catch {}
}

async function fetchQuestions() {
  loading.value = true
  try {
    const res: any = await getQuestionPage(query)
    questionList.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  Object.assign(query, { page: 1, subjectId: undefined, type: undefined, status: undefined })
  fetchQuestions()
}

function editQuestion(id: number) {
  router.push(`/questions/create?id=${id}`)
}

async function toggleStatus(row: any) {
  const newStatus = row.status === 'PUBLISHED' ? 'DRAFT' : 'PUBLISHED'
  await request.put(`/questions/${row.id}/status?status=${newStatus}`)
  ElMessage.success(newStatus === 'PUBLISHED' ? '已发布' : '已下架')
  fetchQuestions()
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确定删除该题目吗？', '提示')
  await deleteQuestion(id)
  ElMessage.success('删除成功')
  fetchQuestions()
}

function truncateContent(jsonStr: string) {
  try {
    const obj = JSON.parse(jsonStr)
    return obj.text?.substring(0, 80) || obj.passage?.substring(0, 80) || jsonStr.substring(0, 80)
  } catch {
    return jsonStr.substring(0, 80)
  }
}

function typeLabel(type: string) {
  const map: Record<string, string> = { SINGLE_CHOICE: '单选', MULTIPLE_CHOICE: '多选', TRUE_FALSE: '判断', FILL_BLANK: '填空', SHORT_ANSWER: '简答', ESSAY: '论述', COMPOSITE: '组合' }
  return map[type] || type
}

function diffTag(d: number) {
  if (d >= 2.5) return 'danger'
  if (d >= 1.5) return 'warning'
  return 'success'
}
</script>

<style scoped>
.question-content { color: #606266; font-size: 14px; }
</style>
