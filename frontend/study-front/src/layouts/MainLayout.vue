<template>
  <el-container style="height: 100vh">
    <el-aside :width="isCollapsed ? '64px' : '220px'" class="app-aside">
      <div class="logo" @click="router.push('/')">
        <span v-if="!isCollapsed" class="logo-text">考试系统</span>
        <span v-else class="logo-text-mini">考</span>
      </div>
      <el-menu
        :default-active="route.path"
        :collapse="isCollapsed"
        :router="true"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <template #title>仪表盘</template>
        </el-menu-item>

        <el-sub-menu index="system">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/system/users">用户管理</el-menu-item>
          <el-menu-item index="/system/roles">角色管理</el-menu-item>
          <el-menu-item index="/system/accounts">账户管理</el-menu-item>
          <el-menu-item index="/system/grades">年级管理</el-menu-item>
          <el-menu-item index="/system/classes">班级管理</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="question">
          <template #title>
            <el-icon><Notebook /></el-icon>
            <span>题库管理</span>
          </template>
          <el-menu-item index="/subjects">科目管理</el-menu-item>
          <el-menu-item index="/questions">题目列表</el-menu-item>
          <el-menu-item index="/questions/create">创建题目</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="paper">
          <template #title>
            <el-icon><Document /></el-icon>
            <span>试卷管理</span>
          </template>
          <el-menu-item index="/papers">试卷列表</el-menu-item>
          <el-menu-item index="/papers/create">创建试卷</el-menu-item>
          <el-menu-item index="/paper-templates">组卷模板</el-menu-item>
          <el-menu-item index="/paper-templates/create">创建模板</el-menu-item>
          <el-menu-item index="/exams">考试安排</el-menu-item>
          <el-menu-item index="/my-exams">我的考试</el-menu-item>
          <el-menu-item index="/grading/exams">批改管理</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="app-header">
        <div class="header-left">
          <el-icon
            style="cursor: pointer; font-size: 20px"
            @click="appStore.toggleSidebar"
          >
            <Fold v-if="!isCollapsed" />
            <Expand v-else />
          </el-icon>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              {{ userStore.userInfo?.realName || userStore.userInfo?.username || '用户' }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人信息</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Odometer, Setting, Notebook, Document, Fold, Expand, ArrowDown } from '@element-plus/icons-vue'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { removeToken } from '@/utils/token'

const router = useRouter()
const route = useRoute()
const appStore = useAppStore()
const userStore = useUserStore()

const isCollapsed = computed(() => appStore.sidebarCollapsed)

function handleCommand(command: string) {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    removeToken()
    router.push('/login')
  }
}
</script>

<style scoped>
.app-aside {
  background-color: #304156;
  overflow: hidden;
  transition: width 0.3s;
}
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
  font-weight: bold;
  cursor: pointer;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}
.logo-text-mini {
  font-size: 24px;
}
.app-header {
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}
.header-right .user-info {
  cursor: pointer;
  color: #606266;
  display: flex;
  align-items: center;
  gap: 4px;
}
.app-main {
  background: #f0f2f5;
  min-height: calc(100vh - 60px);
}
</style>
