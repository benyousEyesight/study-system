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
          path: 'system/accounts',
          name: 'Accounts',
          component: () => import('@/views/system/AccountView.vue'),
          meta: { title: '账户管理' },
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
        {
          path: 'papers',
          name: 'Papers',
          component: () => import('@/views/paper/PaperListView.vue'),
          meta: { title: '试卷列表' },
        },
        {
          path: 'papers/create',
          name: 'PaperCreate',
          component: () => import('@/views/paper/PaperCreateView.vue'),
          meta: { title: '创建试卷' },
        },
        {
          path: 'papers/:id',
          name: 'PaperDetail',
          component: () => import('@/views/paper/PaperDetailView.vue'),
          meta: { title: '试卷详情' },
        },
        {
          path: 'paper-templates',
          name: 'PaperTemplates',
          component: () => import('@/views/paper/PaperTemplateView.vue'),
          meta: { title: '组卷模板' },
        },
        {
          path: 'paper-templates/create',
          name: 'PaperTemplateCreate',
          component: () => import('@/views/paper/PaperTemplateCreateView.vue'),
          meta: { title: '创建模板' },
        },
        {
          path: 'paper-templates/:id/edit',
          name: 'PaperTemplateEdit',
          component: () => import('@/views/paper/PaperTemplateCreateView.vue'),
          meta: { title: '编辑模板' },
        },
        {
          path: 'exams',
          name: 'Exams',
          component: () => import('@/views/exam/ExamListView.vue'),
          meta: { title: '考试安排' },
        },
        {
          path: 'exams/create',
          name: 'ExamCreate',
          component: () => import('@/views/exam/ExamCreateView.vue'),
          meta: { title: '创建考试' },
        },
        {
          path: 'exams/:id',
          name: 'ExamDetail',
          component: () => import('@/views/exam/ExamDetailView.vue'),
          meta: { title: '考试详情' },
        },
        {
          path: 'exams/:id/edit',
          name: 'ExamEdit',
          component: () => import('@/views/exam/ExamCreateView.vue'),
          meta: { title: '编辑考试' },
        },
        {
          path: 'my-exams',
          name: 'MyExams',
          component: () => import('@/views/exam/MyExamsView.vue'),
          meta: { title: '我的考试' },
        },
        {
          path: 'exam/session/:sessionId',
          name: 'ExamSession',
          component: () => import('@/views/exam/ExamSessionView.vue'),
          meta: { title: '在线答题' },
        },
        {
          path: 'exam/result/:sessionId',
          name: 'ExamResult',
          component: () => import('@/views/exam/ExamResultView.vue'),
          meta: { title: '考试成绩' },
        },
        {
          path: 'grading/exams',
          name: 'GradingExams',
          component: () => import('@/views/exam/GradingExamListView.vue'),
          meta: { title: '批改管理' },
        },
        {
          path: 'grading/exams/:examId/sessions',
          name: 'GradingSessions',
          component: () => import('@/views/exam/GradingSessionView.vue'),
          meta: { title: '批改答卷' },
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
