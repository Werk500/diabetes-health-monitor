import { createRouter, createWebHashHistory } from 'vue-router';
import Login from '../views/Login.vue';
import Register from '../views/Register.vue';
import { useUserStore } from '../stores/user';
import { ElMessage } from 'element-plus'
import api from "../api/index.js";

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', name: 'Login', component: Login },
  { path: '/register', name: 'Register', component: Register },
  {
    path: '/user',
    component: () => import('../views/UserLayout.vue'),
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/Dashboard.vue') },
      { path: 'body', name: 'BodyRecord', component: () => import('../views/BodyRecord.vue') },
      { path: 'bloodSugar', name: 'BloodSugar', component: () => import('../views/BloodSugarRecord.vue') },
      { path: 'diet', name: 'Diet', component: () => import('../views/DietRecord.vue') },
      { path: 'exercise', name: 'Exercise', component: () => import('../views/ExerciseRecord.vue') },
            { path: 'ai-chat', name: 'AiChat', component: () => import('../views/AiChat.vue') },
      { path: 'ai-analysis', name: 'AiAnalysis', component: () => import('../views/AiAnalysis.vue') },
      { path: 'food-recognition', name: 'FoodRecognition', component: () => import('../views/FoodRecognition.vue') },
      { path: 'health-report', name: 'HealthReport', component: () => import('../views/HealthReport.vue') },
      { path: 'articles', name: 'Articles', component: () => import('../views/Articles.vue') },
    ]
  },
  {
    path: '/admin',
    component: () => import('../views/AdminLayout.vue'),
    children: [
      { path: 'dashboard', name: 'AdminDashboard', component: () => import('../views/admin/Dashboard.vue') },
      { path: 'users', name: 'UserManage', component: () => import('../views/admin/UserManage.vue') },
      { path: 'articles', name: 'ArticleManage', component: () => import('../views/admin/ArticleManage.vue') },
      { path: 'exercise', name: 'ExerciseManage', component: () => import('../views/admin/ExerciseManage.vue') },
    ]
  }
];

const router = createRouter({
  history: createWebHashHistory(),
  routes
});

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


// response 鎷︽埅鍣?鈥?鍦?axios.create 涔嬪悗銆乪xport default api 涔嬪悗鍔?api.interceptors.response.use(
    response => {
      const res = response.data
      // 涓氬姟閿欒
      if (res.code && res.code !== 200) {
        ElMessage.error(res.msg || '璇锋眰澶辫触')
        return Promise.reject(new Error(res.msg))
      }
      return response
    },
    error => {
      // HTTP 閿欒
      if (error.response) {
        const status = error.response.status
        if (status === 401) {
          localStorage.removeItem('token')
          localStorage.removeItem('user')
          window.location.hash = '#/login'
          ElMessage.error('鐧诲綍宸茶繃鏈燂紝璇烽噸鏂扮櫥褰?)
        } else if (status === 403) {
          ElMessage.error('鏃犳潈闄愯闂?)
        } else {
          ElMessage.error(error.response.data?.msg || '绯荤粺绻佸繖')
        }
      } else {
        ElMessage.error('缃戠粶寮傚父锛岃妫€鏌ヨ繛鎺?)
      }
      return Promise.reject(error)
    }
)

export default router;