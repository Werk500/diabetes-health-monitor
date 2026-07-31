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


// response 拦截器 — 在 axios.create 之后、export default api 之后加
api.interceptors.response.use(
    response => {
      const res = response.data
      // 业务错误
      if (res.code && res.code !== 200) {
        ElMessage.error(res.msg || '请求失败')
        return Promise.reject(new Error(res.msg))
      }
      return response
    },
    error => {
      // HTTP 错误
      if (error.response) {
        const status = error.response.status
        if (status === 401) {
          localStorage.removeItem('token')
          localStorage.removeItem('user')
          window.location.hash = '#/login'
          ElMessage.error('登录已过期，请重新登录')
        } else if (status === 403) {
          ElMessage.error('无权限访问')
        } else {
          ElMessage.error(error.response.data?.msg || '系统繁忙')
        }
      } else {
        ElMessage.error('网络异常，请检查连接')
      }
      return Promise.reject(error)
    }
)

export default router;