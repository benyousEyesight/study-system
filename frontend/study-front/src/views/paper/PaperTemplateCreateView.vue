<template>
  <div v-loading="loading">
    <h3>{{ isEdit ? '编辑模板' : '创建模板' }}</h3>

    <el-card style="margin-top: 16px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="模板名称" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="科目" required>
              <el-select v-model="form.subjectId" placeholder="选择科目" style="width: 100%">
                <el-option v-for="s in subjects" :key="s.id" :label="s.name" :value="s.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="总分">
              <el-input-number v-model="form.totalScore" :min="0" style="width: 100%" />
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
        <span>抽题规则</span>
        <el-button size="small" type="primary" style="margin-left: 12px" @click="addRule">添加规则</el-button>
      </template>

      <div v-for="(rule, ri) in form.rules" :key="ri" style="margin-bottom: 16px; padding: 12px; border: 1px solid #e6e6e6; border-radius: 4px">
        <el-row :gutter="16" align="middle">
          <el-col :span="6">
            <el-input v-model="rule.sectionTitle" placeholder="板块标题，如：一、选择题" />
          </el-col>
          <el-col :span="4">
            <el-select v-model="rule.questionType" placeholder="题型" style="width: 100%">
              <el-option label="单选题" value="SINGLE_CHOICE" />
              <el-option label="多选题" value="MULTIPLE_CHOICE" />
              <el-option label="判断题" value="TRUE_FALSE" />
              <el-option label="填空题" value="FILL_BLANK" />
              <el-option label="简答题" value="SHORT_ANSWER" />
              <el-option label="论述题" value="ESSAY" />
              <el-option label="组合题" value="COMPOSITE" />
            </el-select>
          </el-col>
          <el-col :span="3">
            <el-select v-model="rule.difficulty" placeholder="难度" clearable style="width: 100%">
              <el-option label="不限" :value="undefined" />
              <el-option label="易" :value="1.0" />
              <el-option label="中" :value="2.0" />
              <el-option label="难" :value="3.0" />
            </el-select>
          </el-col>
          <el-col :span="2">
            <el-input-number v-model="rule.questionCount" :min="1" placeholder="题数" style="width: 100%" />
          </el-col>
          <el-col :span="2">
            <el-input-number v-model="rule.scorePerQuestion" :min="0" placeholder="每题分值" style="width: 100%" />
          </el-col>
          <el-col :span="2">
            <el-input-number v-model="rule.sort" :min="0" placeholder="排序" style="width: 100%" />
          </el-col>
          <el-col :span="3" style="text-align: right">
            <el-button size="small" type="danger" text @click="form.rules.splice(ri, 1)">删除</el-button>
          </el-col>
        </el-row>
        <el-form-item label="知识点" style="margin-top: 8px; margin-bottom: 0">
          <el-select v-model="rule.knowledgePointIds" multiple placeholder="选择知识点（可选）" style="width: 100%" :disabled="!form.subjectId">
            <el-option v-for="kp in knowledgePoints" :key="kp.id" :label="kp.name" :value="kp.id" />
          </el-select>
        </el-form-item>
      </div>
    </el-card>

    <div style="margin-top: 16px; text-align: center">
      <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      <el-button @click="router.back()">取消</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getTemplateById, createTemplate, updateTemplate } from '@/api/paper'
import { getSubjectList, getKnowledgePointTree } from '@/api/question'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const isEdit = ref(false)
const loading = ref(false)
const saving = ref(false)
const subjects = ref<any[]>([])
const knowledgePoints = ref<any[]>([])

const form = reactive({
  tenantId: 0,
  subjectId: undefined as number | undefined,
  name: '',
  totalScore: 0,
  description: '',
  rules: [] as any[],
})

onMounted(async () => {
  try {
    const res: any = await getSubjectList(0)
    subjects.value = res.data
  } catch {}

  if (route.params.id) {
    isEdit.value = true
    loading.value = true
    try {
      const res: any = await getTemplateById(Number(route.params.id))
      const data = res.data
      form.name = data.name
      form.subjectId = data.subjectId
      form.totalScore = data.totalScore
      form.description = data.description || ''
      form.rules = (data.rules || []).map((r: any) => ({
        id: r.id,
        sectionTitle: r.sectionTitle,
        questionType: r.questionType,
        difficulty: r.difficulty,
        questionCount: r.questionCount,
        scorePerQuestion: r.scorePerQuestion,
        sort: r.sort,
        knowledgePointIds: parseKpIds(r.knowledgePointIds),
      }))
      if (data.subjectId) fetchKnowledgePoints(data.subjectId)
    } finally {
      loading.value = false
    }
  }
})

watch(() => form.subjectId, (val) => {
  if (val) fetchKnowledgePoints(val)
})

async function fetchKnowledgePoints(subjectId: number) {
  try {
    const res: any = await getKnowledgePointTree(subjectId, 0)
    knowledgePoints.value = flattenTree(res.data || [])
  } catch {
    knowledgePoints.value = []
  }
}

function flattenTree(nodes: any[]): any[] {
  const result: any[] = []
  for (const n of nodes) {
    result.push({ id: n.id, name: n.name })
    if (n.children) result.push(...flattenTree(n.children))
  }
  return result
}

function addRule() {
  form.rules.push({
    sectionTitle: '',
    questionType: undefined,
    difficulty: undefined,
    questionCount: 1,
    scorePerQuestion: 0,
    sort: form.rules.length,
    knowledgePointIds: [],
  })
}

function parseKpIds(jsonStr: string): number[] {
  if (!jsonStr) return []
  try {
    return JSON.parse(jsonStr)
  } catch {
    return []
  }
}

function formatKpIds(ids: number[]): string {
  return JSON.stringify(ids)
}

async function handleSave() {
  if (!form.name || !form.subjectId) {
    ElMessage.warning('请填写名称和科目')
    return
  }
  saving.value = true
  try {
    const payload = {
      ...form,
      rules: form.rules.map((r: any) => ({
        sectionTitle: r.sectionTitle,
        questionType: r.questionType,
        difficulty: r.difficulty,
        questionCount: r.questionCount,
        scorePerQuestion: r.scorePerQuestion,
        sort: r.sort,
        knowledgePointIds: formatKpIds(r.knowledgePointIds || []),
      })),
    }
    if (isEdit.value) {
      await updateTemplate(Number(route.params.id), payload)
      ElMessage.success('更新成功')
    } else {
      await createTemplate(payload)
      ElMessage.success('创建成功')
    }
    router.push('/paper-templates')
  } catch {
    // error handled by interceptor
  } finally {
    saving.value = false
  }
}
</script>
