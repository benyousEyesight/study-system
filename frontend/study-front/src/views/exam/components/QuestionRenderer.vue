<template>
  <div class="question-renderer">
    <div class="question-text" v-html="textContent"></div>

    <!-- 单选题 -->
    <el-radio-group v-if="type === 'SINGLE_CHOICE'" v-model="selectedValue" @change="onChange">
      <el-radio v-for="(opt, k) in options" :key="k" :value="k" class="option-item">{{ k }}. {{ opt }}</el-radio>
    </el-radio-group>

    <!-- 多选题 -->
    <el-checkbox-group v-else-if="type === 'MULTIPLE_CHOICE'" v-model="selectedArray" @change="onChange">
      <el-checkbox v-for="(opt, k) in options" :key="k" :value="k" class="option-item">{{ k }}. {{ opt }}</el-checkbox>
    </el-checkbox-group>

    <!-- 判断题 -->
    <el-radio-group v-else-if="type === 'TRUE_FALSE'" v-model="selectedValue" @change="onChange">
      <el-radio value="true" class="option-item">正确</el-radio>
      <el-radio value="false" class="option-item">错误</el-radio>
    </el-radio-group>

    <!-- 填空题 -->
    <el-input v-else-if="type === 'FILL_BLANK'" v-model="textValue" @change="onChange"
      placeholder="请输入答案" style="width: 300px" />

    <!-- 简答/论述题 -->
    <el-input v-else-if="type === 'SHORT_ANSWER' || type === 'ESSAY'" v-model="textValue" @change="onChange"
      type="textarea" :rows="6" placeholder="请输入答案" />

    <!-- 组合题 -->
    <div v-else-if="type === 'COMPOSITE'">
      <el-alert type="info" :closable="false" show-icon title="组合题" description="请按子题依次作答" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'

const props = defineProps<{ type: string; content: any; answer?: string }>()
const emit = defineEmits<{ answer: [value: string] }>()

const selectedValue = ref('')
const selectedArray = ref<string[]>([])
const textValue = ref('')

watch(() => props.answer, (v) => {
  if (!v) return
  if (props.type === 'SINGLE_CHOICE' || props.type === 'TRUE_FALSE') selectedValue.value = v
  else if (props.type === 'MULTIPLE_CHOICE') {
    try { selectedArray.value = JSON.parse(v) } catch { selectedArray.value = [] }
  } else textValue.value = v
}, { immediate: true })

const textContent = computed(() => props.content?.text || props.content?.passage || '')
const options = computed(() => props.content?.options || {})

function onChange() {
  let val = ''
  if (props.type === 'SINGLE_CHOICE' || props.type === 'TRUE_FALSE') val = selectedValue.value
  else if (props.type === 'MULTIPLE_CHOICE') val = JSON.stringify(selectedArray.value)
  else val = textValue.value
  emit('answer', val)
}
</script>

<style scoped>
.question-renderer { padding: 16px 0; }
.question-text { font-size: 15px; line-height: 1.8; margin-bottom: 16px; white-space: pre-wrap; }
.option-item { display: flex; margin: 8px 0; }
</style>
