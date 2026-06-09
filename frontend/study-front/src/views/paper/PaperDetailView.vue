<template>
  <div v-loading="loading">
    <el-row justify="space-between" align="middle">
      <h3>试卷详情</h3>
      <el-button @click="router.back()">返回</el-button>
    </el-row>

    <el-card style="margin-top: 16px" v-if="paper">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="标题" :span="2">{{ paper.title }}</el-descriptions-item>
        <el-descriptions-item label="总分">{{ paper.totalScore }}</el-descriptions-item>
        <el-descriptions-item label="时长(分钟)">{{ paper.durationMinutes }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="paper.status === 'PUBLISHED' ? 'success' : 'info'" size="small">
            {{ paper.status === 'PUBLISHED' ? '已发布' : '草稿' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ paper.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ paper.description || '无' }}</el-descriptions-item>
      </el-descriptions>

      <el-divider />

      <div v-for="(section, si) in paper.sections" :key="section.id">
        <h4>{{ section.title }}（共 {{ section.questions?.length || 0 }} 题，{{ section.totalScore }} 分）</h4>
        <el-table :data="section.questions" border stripe style="margin-top: 8px">
          <el-table-column label="#" type="index" width="50" />
          <el-table-column label="题型" width="80">
            <template #default="{ row }">{{ typeLabel(row.questionInfo?.type) }}</template>
          </el-table-column>
          <el-table-column label="题目内容" min-width="300">
            <template #default="{ row }">
              {{ truncateContent(row.questionInfo?.contentJson) }}
            </template>
          </el-table-column>
          <el-table-column label="分值" width="80" prop="score" />
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getPaperById } from '@/api/paper'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const paper = ref<any>(null)

onMounted(async () => {
  loading.value = true
  try {
    const res: any = await getPaperById(Number(route.params.id))
    paper.value = res.data
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
})

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
