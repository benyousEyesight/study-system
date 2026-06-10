<template>
  <div class="login-container">
    <el-card class="login-card" shadow="always">
      <template #header>
        <div class="login-header">
          <h2>在线教育考试系统</h2>
          <p class="login-subtitle">教师 & 学生管理平台</p>
        </div>
      </template>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        size="large"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            show-password
            :prefix-icon="Lock"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            @click="handleLogin"
            style="width: 100%"
          >
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { login } from '@/api/auth'
import { setToken, setRefreshToken, setUserInfo } from '@/utils/token'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()

const form = reactive({
  username: '',
  password: '',
  tenantId: 0,
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const loading = ref(false)

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res: any = await login(form)
    setToken(res.data.accessToken)
    setRefreshToken(res.data.refreshToken)
    setUserInfo(res.data.userInfo)
    userStore.setInfo(res.data.userInfo)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0c3483 0%, #a2b6df 50%, #6b8cce 100%);
}

.login-card {
  width: 420px;
  border-radius: 8px;
}

.login-header {
  text-align: center;
}

.login-header h2 {
  margin: 0;
  color: #303133;
  font-size: 24px;
}

.login-subtitle {
  margin: 8px 0 0;
  color: #909399;
  font-size: 14px;
}
</style>
