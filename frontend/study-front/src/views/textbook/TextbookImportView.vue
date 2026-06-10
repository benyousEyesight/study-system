<template>
  <div>
    <h3>教材导入</h3>
    <p style="color:#909399;font-size:14px">上传电子版教材（PDF/DOCX/TXT），自动提取章节结构生成知识点</p>

    <el-row :gutter="20" style="margin-top:16px">
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>上传文件</template>
          <el-upload
            drag
            :auto-upload="false"
            :show-file-list="false"
            accept=".pdf,.docx,.txt"
            :on-change="handleFileChange"
          >
            <el-icon style="font-size:48px;color:#c0c4cc"><UploadFilled /></el-icon>
            <div style="margin-top:8px">拖拽文件到此处，或点击选择</div>
            <template #tip>
              <div style="color:#909399;font-size:12px;margin-top:4px">支持 PDF、DOCX、TXT 格式，最大 50MB</div>
            </template>
          </el-upload>

          <div v-if="selectedFile" style="margin-top:12px">
            <div class="file-info">
              <el-icon style="margin-right:4px"><Document /></el-icon>
              <span>{{ selectedFile.name }}</span>
              <span style="color:#999;margin-left:8px">({{ formatSize(selectedFile.size) }})</span>
            </div>
          </div>

          <el-form style="margin-top:12px" label-width="80px">
            <el-form-item label="所属科目">
              <el-select v-model="selectedSubjectId" placeholder="选择科目" style="width:100%">
                <el-option v-for="s in subjects" :key="s.id" :label="s.name" :value="s.id" />
              </el-select>
            </el-form-item>
          </el-form>

          <el-button type="primary" @click="handleParse" :loading="parsing" :disabled="!selectedFile || !selectedSubjectId" style="width:100%">
            {{ parsing ? '解析中...' : '开始解析' }}
          </el-button>
        </el-card>

        <el-card shadow="never" style="margin-top:16px" v-if="parseResult">
          <template #header>文件信息</template>
          <el-descriptions :column="1" size="small" border>
            <el-descriptions-item label="文件名">{{ parseResult.fileName }}</el-descriptions-item>
            <el-descriptions-item label="类型">{{ parseResult.fileType }}</el-descriptions-item>
            <el-descriptions-item label="页数" v-if="parseResult.totalPages">{{ parseResult.totalPages }}</el-descriptions-item>
            <el-descriptions-item label="章节数">{{ chapterCount }}</el-descriptions-item>
          </el-descriptions>

          <el-divider />
          <div style="font-size:13px;color:#606266">
            <div style="font-weight:500;margin-bottom:4px">内容预览</div>
            <p style="white-space:pre-wrap;line-height:1.6;max-height:200px;overflow:auto">{{ parseResult.previewText }}</p>
          </div>
        </el-card>
      </el-col>

      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <span>提取的章节结构</span>
            <span style="color:#999;font-size:12px;margin-left:8px">— 可编辑节点名称，取消勾选不需要的节点</span>
          </template>

          <div v-if="!parseResult" style="text-align:center;padding:60px 0;color:#c0c4cc">
            请先上传并解析文件
          </div>

          <div v-else>
            <div class="tree-controls">
              <el-button size="small" @click="expandAll">全部展开</el-button>
              <el-button size="small" @click="collapseAll">全部收起</el-button>
              <el-button size="small" type="primary" @click="handleSave" :loading="saving" :disabled="editableChapters.length === 0">
                保存为知识点
              </el-button>
            </div>

            <el-tree
              ref="treeRef"
              :data="editableChapters"
              node-key="id"
              show-checkbox
              default-expand-all
              draggable
              :filter-node-method="filterNode"
            >
              <template #default="{ node, data }">
                <span class="tree-node">
                  <el-input
                    v-model="data._name"
                    size="small"
                    style="width:300px"
                    @click.stop
                  />
                  <el-button size="small" text type="danger" @click="removeNode(data)" style="margin-left:4px">删除</el-button>
                </span>
              </template>
            </el-tree>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { parseTextbook, saveTextbookKnowledgePoints } from '@/api/textbook'
import { getSubjectList } from '@/api/question'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled, Document } from '@element-plus/icons-vue'

const subjects = ref<any[]>([])
const selectedSubjectId = ref<number | null>(null)
const selectedFile = ref<File | null>(null)
const parsing = ref(false)
const saving = ref(false)
const parseResult = ref<any>(null)
const editableChapters = ref<any[]>([])
const treeRef = ref()
let nodeIdCounter = 0

const chapterCount = computed(() => countNodes(editableChapters.value))

onMounted(async () => {
  const res: any = await getSubjectList(0)
  subjects.value = res.data || []
})

function handleFileChange(file: any) {
  selectedFile.value = file.raw
  parseResult.value = null
  editableChapters.value = []
}

async function handleParse() {
  if (!selectedFile.value || !selectedSubjectId.value) return
  parsing.value = true
  try {
    const res: any = await parseTextbook(selectedFile.value)
    parseResult.value = res.data
    // Convert to editable tree format
    editableChapters.value = toEditable(res.data.chapters || [])
  } catch {
    ElMessage.error('解析失败，请检查文件格式')
  } finally { parsing.value = false }
}

function toEditable(nodes: any[]): any[] {
  return nodes.map(n => ({
    id: ++nodeIdCounter,
    _name: n.name,
    level: n.level,
    children: n.children ? toEditable(n.children) : [],
  }))
}

function countNodes(nodes: any[]): number {
  let count = nodes.length
  for (const n of nodes) count += countNodes(n.children || [])
  return count
}

function expandAll() { treeRef.value?.expandAll() }
function collapseAll() { treeRef.value?.collapseAll() }

function filterNode(value: string, data: any) {
  if (!value) return true
  return data._name.includes(value)
}

function removeNode(data: any) {
  const remove = (nodes: any[]) => {
    const idx = nodes.indexOf(data)
    if (idx > -1) { nodes.splice(idx, 1); return true }
    for (const n of nodes) { if (n.children && remove(n.children)) return true }
    return false
  }
  remove(editableChapters.value)
}

function collectChecked(nodes: any[]): any[] {
  const checked = treeRef.value?.getCheckedNodes() || []
  // Also include half-checked nodes
  const halfChecked = treeRef.value?.getHalfCheckedNodes() || []
  // Build tree from checked nodes only, preserving structure
  return buildCheckedTree(editableChapters.value, new Set([...checked.map((n: any) => n.id), ...halfChecked.map((n: any) => n.id)]))
}

function buildCheckedTree(nodes: any[], checkedIds: Set<number>): any[] {
  const result: any[] = []
  for (const n of nodes) {
    const children = n.children ? buildCheckedTree(n.children, checkedIds) : []
    if (checkedIds.has(n.id) || children.length > 0) {
      result.push({
        name: n._name,
        level: n.level,
        children: children,
      })
    }
  }
  return result
}

async function handleSave() {
  if (!selectedSubjectId.value) return
  const chapters = collectChecked(editableChapters.value)
  if (chapters.length === 0) { ElMessage.warning('请选择要保存的章节'); return }
  saving.value = true
  try {
    await saveTextbookKnowledgePoints(selectedSubjectId.value, chapters)
    ElMessage.success(`已保存 ${countNodes(chapters)} 个知识点`)
    parseResult.value = null
    editableChapters.value = []
    selectedFile.value = null
  } catch { ElMessage.error('保存失败')
  } finally { saving.value = false }
}

function formatSize(bytes: number) {
  if (bytes < 1024) return bytes + 'B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB'
  return (bytes / 1024 / 1024).toFixed(1) + 'MB'
}
</script>

<style scoped>
.file-info {
  display: flex;
  align-items: center;
  padding: 8px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 14px;
}
.tree-controls {
  margin-bottom: 12px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.tree-node {
  display: flex;
  align-items: center;
  flex: 1;
}
</style>
