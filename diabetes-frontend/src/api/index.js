import axios from 'axios';
import { ElMessage } from 'element-plus';

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
});

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = 'Bearer ' + token;
  return config;
});

api.interceptors.response.use(
  response => {
    const res = response.data;
    if (res.code && res.code !== 200) {
      ElMessage.error(res.msg || 'request failed');
    }
    return response;
  },
  error => {
    if (error.response) {
      const status = error.response.status;
      if (status === 401) {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        window.location.hash = '#/login';
        ElMessage.error('login expired');
      } else if (status === 403) {
        ElMessage.error('no permission');
      } else {
        ElMessage.error(error.response.data?.msg || 'system busy');
      }
    } else {
      ElMessage.error('network error');
    }
    return Promise.reject(error);
  }
);

export default api;

// ============ User API ============
export const userApi = {
  login: (data) => api.post('/user/login', data),
  register: (data) => api.post('/user/register', data),
  info: (id) => api.get('/user/info/' + id),
  update: (data) => api.put('/user/update', data),
  sendSms: (phone) => api.post('/user/sms/send', { phone }),
  smsLogin: (data) => api.post('/user/sms/login', data),
  articleList: (params) => api.get('/user/article/list', { params }),
};


// ============ Record API ============
export const recordApi = {
  addBody: (data) => api.post('/record/body', data),
  bodyList: (userId, params) => api.get('/record/body/list/' + userId, { params }),
  bodyTrend: (userId, params) => api.get('/record/body/trend/' + userId, { params }),
  bodyLatest: (userId) => api.get('/record/body/latest/' + userId),
  deleteBody: (id) => api.delete('/record/body/' + id),
  addBloodSugar: (data) => api.post('/record/bloodSugar', data),
  bloodSugarList: (userId, params) => api.get('/record/bloodSugar/list/' + userId, { params }),
  bloodSugarTrend: (userId, params) => api.get('/record/bloodSugar/trend/' + userId, { params }),
  bloodSugarLatest: (userId) => api.get('/record/bloodSugar/latest/' + userId),
  deleteBloodSugar: (id) => api.delete('/record/bloodSugar/' + id),
  addDiet: (data) => api.post('/record/diet', data),
  dietList: (userId, params) => api.get('/record/diet/list/' + userId, { params }),
  dietStats: (userId, params) => api.get('/record/diet/stats/' + userId, { params }),
  deleteDiet: (id) => api.delete('/record/diet/' + id),
  addExercise: (data) => api.post('/record/exercise', data),
  exerciseList: (userId, params) => api.get('/record/exercise/list/' + userId, { params }),
  exerciseTrend: (userId, params) => api.get('/record/exercise/trend/' + userId, { params }),
  deleteExercise: (id) => api.delete('/record/exercise/' + id),
  exerciseTypes: () => api.get('/record/exerciseType/list'),
  exportRecords: () => api.get('/record/export', { responseType: 'blob' }),
};

// ============ Admin API ============
export const adminApi = {
  userList: (params) => api.get('/admin/user/list', { params }),
  deleteUser: (id) => api.delete('/admin/user/' + id),
  getExerciseTypes: () => api.get('/admin/exerciseType/list'),
  exportRecords: () => api.get('/record/export', { responseType: 'blob' }),
  addExerciseType: (data) => api.post('/admin/exerciseType', data),
  updateExerciseType: (data) => api.put('/admin/exerciseType', data),
  deleteExerciseType: (id) => api.delete('/admin/exerciseType/' + id),
  articleList: (params) => api.get('/admin/article/list', { params }),
  addArticle: (data) => api.post('/admin/article', data),
  updateArticle: (data) => api.put('/admin/article', data),
  deleteArticle: (id) => api.delete('/admin/article/' + id),
  pushArticle: (id) => api.put('/admin/article/push/' + id),
  categoryStats: () => api.get('/admin/article/categoryStats'),
  toggleStatus: (id) => api.put('/user/status/' + id),
  stats: () => api.get('/admin/stats'),
};

// ============ Dashboard API ============
export const dashboardApi = {
  get: (userId, params) => api.get('/dashboard/' + userId, { params }),
};

// ============ AI Chat API ============
export const aiApi = {
  chat: (data) => api.post('/ai/chat', data),
  bloodSugarAnalysis: (data) => api.post('/ai/analysis/blood-sugar', data),
  dietAnalysis: (data) => api.post('/ai/analysis/diet', data),
  dailyReport: () => api.post('/ai/analysis/daily-report'),
};

// ============ Food Recognition API ============
export const foodApi = {
  recognize: (formData) => api.post('/ai/food/recognize', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
};

// ============ Health Report API ============
export const reportApi = {
  download: () => api.get('/ai/report', { responseType: 'blob' }),
};
