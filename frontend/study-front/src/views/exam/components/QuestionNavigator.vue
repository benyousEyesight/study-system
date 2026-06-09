<template>
  <div class="navigator">
    <div v-for="section in sections" :key="section.id" class="nav-section">
      <div class="nav-section-title">{{ section.title }}</div>
      <div class="nav-questions">
        <div v-for="q in section.questions" :key="q.questionId"
          :class="['nav-item', { active: currentId === q.questionId, answered: answeredIds.has(q.questionId) }]"
          @click="$emit('select', q.questionId)">
          {{ q.sort != null ? q.sort + 1 : q.questionId }}
        </div>
      </div>
    </div>
    <div class="nav-legend">
      <span><span class="dot answered"></span> 已答</span>
      <span><span class="dot"></span> 未答</span>
      <span><span class="dot active"></span> 当前</span>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ sections: any[]; currentId: number; answeredIds: Set<number> }>()
defineEmits<{ select: [id: number] }>()
</script>

<style scoped>
.navigator { width: 180px; padding: 12px; }
.nav-section { margin-bottom: 16px; }
.nav-section-title { font-size: 13px; font-weight: bold; margin-bottom: 8px; color: #606266; }
.nav-questions { display: flex; flex-wrap: wrap; gap: 6px; }
.nav-item { width: 36px; height: 36px; display: flex; align-items: center; justify-content: center;
  border: 1px solid #dcdfe6; border-radius: 4px; cursor: pointer; font-size: 13px; }
.nav-item.answered { background: #409EFF; color: #fff; border-color: #409EFF; }
.nav-item.active { border-color: #E6A23C; border-width: 2px; font-weight: bold; }
.nav-legend { display: flex; gap: 12px; margin-top: 16px; font-size: 12px; color: #909399; }
.dot { display: inline-block; width: 10px; height: 10px; border-radius: 2px; border: 1px solid #dcdfe6; margin-right: 4px; }
.dot.answered { background: #409EFF; border-color: #409EFF; }
.dot.active { border-color: #E6A23C; border-width: 2px; }
</style>
