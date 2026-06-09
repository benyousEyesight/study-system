<template>
  <div :class="['exam-timer', { warning: remaining <= 300 }]">
    <el-icon><Timer /></el-icon>
    <span class="timer-text">{{ formatted }}</span>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { Timer } from '@element-plus/icons-vue'

const props = defineProps<{ remainingSeconds: number }>()
const emit = defineEmits<{ expired: [] }>()

const remaining = ref(props.remainingSeconds)
let timer: number | null = null

onMounted(() => {
  timer = window.setInterval(() => {
    remaining.value--
    if (remaining.value <= 0) { clearInterval(timer!); emit('expired') }
  }, 1000)
})
onUnmounted(() => { if (timer) clearInterval(timer) })

const formatted = computed(() => {
  const h = Math.floor(remaining.value / 3600)
  const m = Math.floor((remaining.value % 3600) / 60)
  const s = remaining.value % 60
  return `${String(h).padStart(2,'0')}:${String(m).padStart(2,'0')}:${String(s).padStart(2,'0')}`
})
</script>

<style scoped>
.exam-timer { font-size: 24px; font-weight: bold; color: #409EFF; }
.exam-timer.warning { color: #E6A23C; animation: blink 1s infinite; }
@keyframes blink { 50% { opacity: 0.5; } }
.timer-text { margin-left: 8px; }
</style>
