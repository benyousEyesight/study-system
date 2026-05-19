<template>
  <div>
    <h3>{{ isEdit ? '编辑题目' : '创建题目' }}</h3>
    <el-card style="margin-top: 16px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="科目" prop="subjectId">
              <el-select v-model="form.subjectId" style="width: 100%">
                <el-option v-for="s in subjects" :key="s.id" :label="s.name" :value="s.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="题型" prop="type">
              <el-select v-model="form.type" style="width: 100%" @change="onTypeChange">
                <el-option label="单选题" value="SINGLE_CHOICE" />
                <el-option label="多选题" value="MULTIPLE_CHOICE" />
                <el-option label="判断题" value="TRUE_FALSE" />
                <el-option label="填空题" value="FILL_BLANK" />
                <el-option label="简答题" value="SHORT_ANSWER" />
                <el-option label="论述题" value="ESSAY" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="难度" prop="difficulty">
              <el-slider v-model="form.difficulty" :min="1" :max="3" :step="0.5" show-stops />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="知识点">
          <el-tree-select
            v-model="form.knowledgePointIds"
            :data="kpTree"
            :props="{ label: 'name', value: 'id' }"
            multiple
            check-strictly
            placeholder="选择知识点"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="题干" prop="contentText">
          <el-input v-model="form.contentText" type="textarea" :rows="4" placeholder="输入题目内容（支持HTML）" />
        </el-form-item>

        <!-- 选择题选项 -->
        <template v-if="isChoiceType">
          <el-form-item label="选项">
            <div style="width: 100%">
              <div v-for="(opt, idx) in form.options" :key="idx" style="display: flex; gap: 8px; margin-bottom: 8px; align-items: center">
                <el-tag>{{ letter(idx) }}</el-tag>
                <el-input v-model="opt.value" placeholder="选项内容" style="flex: 1" />
                <el-button v-if="form.options.length > 2" type="danger" :icon="Delete" circle size="small" @click="form.options.splice(idx, 1)" />
              </div>
              <el-button size="small" @click="form.options.push({ value: '' })">添加选项</el-button>
            </div>
          </el-form-item>
          <el-form-item label="正确答案">
            <template v-if="form.type === 'SINGLE_CHOICE'">
              <el-radio-group v-model="form.correctAnswer">
                <el-radio v-for="(opt, idx) in form.options" :key="idx" :value="letter(idx)">
                  {{ letter(idx) }}
                </el-radio>
              </el-radio-group>
            </template>
            <template v-else-if="form.type === 'MULTIPLE_CHOICE'">
              <el-checkbox-group v-model="form.correctAnswer">
                <el-checkbox v-for="(opt, idx) in form.options" :key="idx" :value="letter(idx)">
                  {{ letter(idx) }}
                </el-checkbox>
              </el-checkbox-group>
            </template>
          </el-form-item>
        </template>

        <template v-if="form.type === 'TRUE_FALSE'">
          <el-form-item label="正确答案">
            <el-radio-group v-model="form.correctAnswer">
              <el-radio value="true">正确</el-radio>
              <el-radio value="false">错误</el-radio>
            </el-radio-group>
          </el-form-item>
        </template>

        <el-form-item label="答案解析">
          <el-input v-model="form.analysis" type="textarea" :rows="3" placeholder="输入答案解析（可选）" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
          <el-button @click="router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Delete } from '@element-plus/icons-vue'
import { createQuestion, getQuestionById, getSubjectList, getKnowledgePointTree } from '@/api/question'
import request from '@/api/request'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const isEdit = computed(() => !!route.query.id)
const saving = ref(false)
const subjects = ref<any[]>([])
const kpTree = ref<any[]>([])
const formRef = ref()

const form = reactive({
  subjectId: undefined as number | undefined,
  type: 'SINGLE_CHOICE',
  difficulty: 2,
  knowledgePointIds: [] as number[],
  contentText: '',
  options: [{ value: '' }, { value: '' }],
  correctAnswer: '',
  analysis: '',
  tenantId: 0,
})

const rules = {
  subjectId: [{ required: true, message: '请选择科目', trigger: 'change' }],
  type: [{ required: true, message: '请选择题型', trigger: 'change' }],
  contentText: [{ required: true, message: '请输入题目内容', trigger: 'blur' }],
}

const isChoiceType = computed(() => ['SINGLE_CHOICE', 'MULTIPLE_CHOICE'].includes(form.type))

watch(() => form.subjectId, (val) => {
  if (val) loadKpTree(val)
})

onMounted(async () => {
  try {
    const res: any = await getSubjectList(0)
    subjects.value = res.data
  } catch {}
  if (isEdit.value) {
    loadQuestion()
  }
})

async function loadQuestion() {
  try {
    const res: any = await getQuestionById(Number(route.query.id))
    const q = res.data
    const content = JSON.parse(q.contentJson || '{}')
    form.subjectId = q.subjectId
    form.type = q.type
    form.difficulty = q.difficulty
    form.contentText = content.text || content.passage || ''
    form.analysis = q.analysis || ''
    if (content.options) {
      form.options = Object.entries(content.options).map(([key, val]) => ({ value: val as string }))
    }
    const answer = JSON.parse(q.answerJson || '{}')
    form.correctAnswer = answer.correct || ''
    if (q.knowledgePointIds) form.knowledgePointIds = q.knowledgePointIds
    if (form.subjectId) loadKpTree(form.subjectId)
  } catch {}
}

async function loadKpTree(subjectId: number) {
  try {
    const res: any = await getKnowledgePointTree(subjectId, 0)
    kpTree.value = res.data
  } catch {}
}

function onTypeChange() {
  if (form.type === 'SINGLE_CHOICE' || form.type === 'MULTIPLE_CHOICE') {
    if (form.options.length === 0) form.options = [{ value: '' }, { value: '' }]
    form.correctAnswer = form.type === 'MULTIPLE_CHOICE' ? [] : ''
  } else {
    form.correctAnswer = ''
  }
}

function letter(idx: number) {
  return String.fromCharCode(65 + idx)
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const contentJson: any = { text: form.contentText }
    if (isChoiceType.value) {
      const options: Record<string, string> = {}
      form.options.forEach((opt, idx) => { options[letter(idx)] = opt.value })
      contentJson.options = options
    }
    const answerJson = { correct: form.correctAnswer }
    const payload = {
      tenantId: 0,
      subjectId: form.subjectId,
      type: form.type,
      difficulty: form.difficulty,
      contentJson: JSON.stringify(contentJson),
      answerJson: JSON.stringify(answerJson),
      analysis: form.analysis,
      status: 'DRAFT',
      knowledgePointIds: form.knowledgePointIds,
    }
    if (isEdit.value) {
      await request.put(`/questions/${route.query.id}`, payload)
      ElMessage.success('更新成功')
    } else {
      await createQuestion(payload)
      ElMessage.success('创建成功')
    }
    router.push('/questions')
  } finally {
    saving.value = false
  }
}
</script>
