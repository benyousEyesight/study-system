import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/token'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/LoginView.vue'),
    },
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/dashboard/DashboardView.vue'),
          meta: { title: '仪表盘' },
        },
        {
          path: 'system/users',
          name: 'Users',
          component: () => import('@/views/system/UserView.vue'),
          meta: { title: '用户管理' },
        },
        {
          path: 'system/roles',
          name: 'Roles',
          component: () => import('@/views/system/RoleView.vue'),
          meta: { title: '角色管理' },
        },
        {
          path: 'questions',
          name: 'Questions',
          component: () => import('@/views/question/QuestionListView.vue'),
          meta: { title: '题目列表' },
        },
        {
          path: 'questions/create',
          name: 'QuestionCreate',
          component: () => import('@/views/question/QuestionCreateView.vue'),
          meta: { title: '创建题目' },
        },
        {
          path: 'subjects',
          name: 'Subjects',
          component: () => import('@/views/question/SubjectView.vue'),
          meta: { title: '科目管理' },
        },
      ],
    },
  ],
})

router.beforeEach((to) => {
  if (to.path !== '/login' && !getToken()) {
    return '/login'
  }
})

export default router
