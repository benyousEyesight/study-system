<template>
  <div v-loading="loading">
    <h3>{{ isEdit ? '编辑试卷' : '创建试卷' }}</h3>

    <el-card style="margin-top: 16px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="试卷标题" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="科目" required>
              <el-select v-model="form.subjectId" placeholder="选择科目" style="width: 100%">
                <el-option v-for="s in subjects" :key="s.id" :label="s.name" :value="s.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="总分">
              <el-input-number v-model="form.totalScore" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="时长(分钟)">
              <el-input-number v-model="form.durationMinutes" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 16px">
      <template #header>
        <span>板块管理</span>
        <el-button size="small" type="primary" style="margin-left: 12px" @click="addSection">添加板块</el-button>
      </template>

      <div v-for="(section, si) in form.sections" :key="si" style="margin-bottom: 16px; padding: 12px; border: 1px solid #e6e6e6; border-radius: 4px">
        <el-row :gutter="16" align="middle">
          <el-col :span="8">
            <el-input v-model="section.title" placeholder="板块标题，如：一、选择题" />
          </el-col>
          <el-col :span="4">
            <el-input-number v-model="section.totalScore" :min="0" placeholder="板块总分" style="width: 100%" />
          </el-col>
          <el-col :span="4">
            <el-input-number v-model="section.sort" :min="0" placeholder="排序" style="width: 100%" />
          </el-col>
          <el-col :span="6">
            <el-button size="small" @click="showQuestionSelector(si)">选择题目</el-button>
          </el-col>
          <el-col :span="2" style="text-align: right">
            <el-button size="small" type="danger" text @click="form.sections.splice(si, 1)">删除</el-button>
          </el-col>
        </el-row>

        <el-table :data="section.questions" border stripe style="margin-top: 8px" v-if="section.questions.length">
          <el-table-column label="#" type="index" width="50" />
          <el-table-column label="题型" width="80">
            <template #default="{ row }">{{ typeLabel(row.questionType) }}</template>
          </el-table-column>
          <el-table-column label="题目内容" min-width="300">
            <template #default="{ row }">{{ row.questionContent }}</template>
          </el-table-column>
          <el-table-column label="分值" width="100">
            <template #default="{ row }">
              <el-input-number v-model="row.score" :min="0" size="small" style="width: 100px" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="60">
            <template #default="{ $index }">
              <el-button size="small" type="danger" text @click="section.questions.splice($index, 1)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <div style="margin-top: 16px; text-align: center">
      <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      <el-button @click="router.back()">取消</el-button>
    </div>

    <!-- 题目选择对话框 -->
    <el-dialog v-model="selectorVisible" title="选择题目" width="80%" destroy-on-close>
      <el-form :inline="true" :model="selectorQuery">
        <el-form-item label="科目">
          <el-select v-model="selectorQuery.subjectId" placeholder="选择科目" clearable style="width: 150px">
            <el-option v-for="s in subjects" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="题型">
          <el-select v-model="selectorQuery.type" placeholder="选择题型" clearable style="width: 150px">
            <el-option label="单选题" value="SINGLE_CHOICE" />
            <el-option label="多选题" value="MULTIPLE_CHOICE" />
            <el-option label="判断题" value="TRUE_FALSE" />
            <el-option label="填空题" value="FILL_BLANK" />
            <el-option label="简答题" value="SHORT_ANSWER" />
            <el-option label="论述题" value="ESSAY" />
            <el-option label="组合题" value="COMPOSITE" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="searchQuestions">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="selectorQuestions" border stripe @selection-change="onSelectionChange" ref="selectorTableRef">
        <el-table-column type="selection" width="50" />
        <el-table-column label="题型" width="80">
          <template #default="{ row }">{{ typeLabel(row.type) }}</template>
        </el-table-column>
        <el-table-column label="题目内容" min-width="300">
          <template #default="{ row }">{{ truncateContent(row.contentJson) }}</template>
        </el-table-column>
        <el-table-column label="难度" width="80" prop="difficulty" />
      </el-table>

      <el-pagination
        v-model:current-page="selectorQuery.page"
        v-model:page-size="selectorQuery.size"
        :total="selectorTotal"
        layout="total, prev, pager, next"
        style="margin-top: 12px; justify-content: flex-end"
        @change="searchQuestions"
      />

      <template #footer>
        <el-button @click="selectorVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmSelection">确定({{ selectedQuestions.length }})</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getPaperById, createPaper, updatePaper } from '@/api/paper'
import { getSubjectList, getQuestionPage } from '@/api/question'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const isEdit = ref(false)
const loading = ref(false)
const saving = ref(false)
const subjects = ref<any[]>([])

const form = reactive({
  tenantId: 0,
  subjectId: undefined as number | undefined,
  title: '',
  totalScore: 0,
  durationMinutes: 0,
  description: '',
  sections: [] as any[],
})

// 题目选择器
const selectorVisible = ref(false)
const currentSectionIdx = ref(0)
const selectorQuestions = ref<any[]>([])
const selectorTotal = ref(0)
const selectedQuestions = ref<any[]>([])
const selectorQuery = reactive({ page: 1, size: 10, subjectId: undefined as any, type: undefined, status: 'PUBLISHED', tenantId: 0 })

onMounted(async () => {
  try {
    const res: any = await getSubjectList(0)
    subjects.value = res.data
  } catch {}

  if (route.query.id) {
    isEdit.value = true
    loading.value = true
    try {
      const res: any = await getPaperById(Number(route.query.id))
      const data = res.data
      form.title = data.title
      form.subjectId = data.subjectId
      form.totalScore = data.totalScore
      form.durationMinutes = data.durationMinutes
      form.description = data.description || ''
      form.sections = (data.sections || []).map((s: any) => ({
        title: s.title,
        sort: s.sort,
        totalScore: s.totalScore,
        description: s.description || '',
        questions: (s.questions || []).map((q: any) => ({
          questionId: q.questionId,
          score: q.score,
          sort: q.sort,
          questionType: q.questionInfo?.type,
          questionContent: truncateContent(q.questionInfo?.contentJson),
        })),
      }))
    } finally {
      loading.value = false
    }
  }
})

function addSection() {
  form.sections.push({ title: '', sort: form.sections.length, totalScore: 0, description: '', questions: [] })
}

function showQuestionSelector(si: number) {
  currentSectionIdx.value = si
  selectorVisible.value = true
  selectorQuery.subjectId = form.subjectId
  selectedQuestions.value = []
  searchQuestions()
}

async function searchQuestions() {
  const res: any = await getQuestionPage(selectorQuery)
  selectorQuestions.value = res.data.records
  selectorTotal.value = res.data.total
}

function onSelectionChange(rows: any[]) {
  selectedQuestions.value = rows
}

function confirmSelection() {
  const section = form.sections[currentSectionIdx.value]
  for (const q of selectedQuestions.value) {
    if (!section.questions.find((pq: any) => pq.questionId === q.id)) {
      section.questions.push({
        questionId: q.id,
        score: 5,
        sort: section.questions.length,
        questionType: q.type,
        questionContent: truncateContent(q.contentJson),
      })
    }
  }
  selectorVisible.value = false
}

async function handleSave() {
  if (!form.title || !form.subjectId) {
    ElMessage.warning('请填写标题和科目')
    return
  }
  saving.value = true
  try {
    const payload = {
      ...form,
      sections: form.sections.map((s: any) => ({
        ...s,
        questions: s.questions.map((q: any) => ({ questionId: q.questionId, score: q.score, sort: q.sort })),
      })),
    }
    if (isEdit.value) {
      await updatePaper(Number(route.query.id), payload)
      ElMessage.success('更新成功')
    } else {
      await createPaper(payload)
      ElMessage.success('创建成功')
    }
    router.push('/papers')
  } catch {
    // error handled by interceptor
  } finally {
    saving.value = false
  }
}

function typeLabel(type: string) {
  const map: Record<string, string> = { SINGLE_CHOICE: '单选', MULTIPLE_CHOICE: '多选', TRUE_FALSE: '判断', FILL_BLANK: '填空', SHORT_ANSWER: '简答', ESSAY: '论述', COMPOSITE: '组合' }
  return map[type] || type
}

function truncateContent(jsonStr: string) {
  if (!jsonStr) return ''
  try {
    const obj = JSON.parse(jsonStr)
    return obj.text?.substring(0, 80) || obj.passage?.substring(0, 80) || jsonStr.substring(0, 80)
  } catch {
    return jsonStr.substring(0, 80)
  }
}
</script>
