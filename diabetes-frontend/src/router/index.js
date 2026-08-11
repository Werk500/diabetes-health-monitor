import { createRouter, createWebHashHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import { useUserStore } from '../stores/user'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', name: 'Login', component: Login },
  { path: '/register', name: 'Register', component: Register },
  {
    path: '/user',
    component: () => import('../views/UserLayout.vue'),
    children: [
      { path: 'profile', name: 'Profile', component: () => import('../views/Profile.vue'), meta: { title: '个人资料' } },
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '仪表盘' } },
      { path: 'body', name: 'BodyRecord', component: () => import('../views/BodyRecord.vue'), meta: { title: '身体记录' } },
      { path: 'bloodSugar', name: 'BloodSugar', component: () => import('../views/BloodSugarRecord.vue'), meta: { title: '血糖记录' } },
      { path: 'diet', name: 'Diet', component: () => import('../views/DietRecord.vue'), meta: { title: '饮食记录' } },
      { path: 'exercise', name: 'Exercise', component: () => import('../views/ExerciseRecord.vue'), meta: { title: '运动记录' } },
      { path: 'ai-chat', name: 'AiChat', component: () => import('../views/AiChat.vue'), meta: { title: 'AI 对话' } },
      { path: 'ai-analysis', name: 'AiAnalysis', component: () => import('../views/AiAnalysis.vue'), meta: { title: 'AI 分析' } },
      { path: 'food-recognition', name: 'FoodRecognition', component: () => import('../views/FoodRecognition.vue'), meta: { title: '食物识别' } },
      { path: 'health-report', name: 'HealthReport', component: () => import('../views/HealthReport.vue'), meta: { title: '健康报告' } },
      { path: 'articles', name: 'Articles', component: () => import('../views/Articles.vue'), meta: { title: '健康文章' } },
    ]
  },
  {
    path: '/admin',
    component: () => import('../views/AdminLayout.vue'),
    children: [
      { path: 'dashboard', name: 'AdminDashboard', component: () => import('../views/admin/Dashboard.vue'), meta: { title: '管理仪表盘' } },
      { path: 'users', name: 'UserManage', component: () => import('../views/admin/UserManage.vue'), meta: { title: '用户管理' } },
      { path: 'articles', name: 'ArticleManage', component: () => import('../views/admin/ArticleManage.vue'), meta: { title: '文章管理' } },
      { path: 'exercise', name: 'ExerciseManage', component: () => import('../views/admin/ExerciseManage.vue'), meta: { title: '运动类型管理' } },
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  if (to.path === '/login' || to.path === '/register') {
    if (userStore.isLoggedIn) {
      next(userStore.isAdmin ? '/admin/dashboard' : '/user/dashboard')
    } else {
      next()
    }
  } else if (to.path.startsWith('/admin')) {
    if (!userStore.isLoggedIn) {
      next('/login')
    } else if (!userStore.isAdmin) {
      next('/user/dashboard')
    } else {
      next()
    }
  } else {
    userStore.isLoggedIn ? next() : next('/login')
  }
})

export default router