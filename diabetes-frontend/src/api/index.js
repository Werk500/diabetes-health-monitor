import axios from 'axios';
import { ElMessage } from 'element-plus';

const ACCESS_TOKEN_KEY = 'accessToken';

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  withCredentials: true
});

const refreshClient = axios.create({
  baseURL: '/api',
  timeout: 10000,
  withCredentials: true
});

let refreshPromise = null;

export const getAccessToken = () => localStorage.getItem(ACCESS_TOKEN_KEY) || '';

export const setAccessToken = (token) => {
  localStorage.setItem(ACCESS_TOKEN_KEY, token);
  localStorage.removeItem('token');
};

export const clearAuthStorage = () => {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem('token');
  localStorage.removeItem('user');
};

const isAuthEndpoint = (url = '') => [
  '/user/login',
  '/user/register',
  '/user/sms/send',
  '/user/sms/login',
  '/user/public-key',
  '/user/refresh',
  '/user/logout'
].some(path => url.includes(path));

const redirectToLogin = (message = '登录已过期，请重新登录') => {
  clearAuthStorage();
  if (!window.location.hash.startsWith('#/login')) {
    ElMessage.error(message);
    window.location.hash = '#/login';
    window.location.reload();
  }
};

export function refreshAccessToken() {
  if (refreshPromise) return refreshPromise;

  refreshPromise = refreshClient.post('/user/refresh')
    .then(response => {
      const accessToken = response.data?.data?.accessToken;
      if (response.data?.code !== 200 || !accessToken) {
        throw new Error(response.data?.msg || '刷新登录状态失败');
      }
      setAccessToken(accessToken);
      window.dispatchEvent(new CustomEvent('auth:access-token', {
        detail: accessToken
      }));
      return accessToken;
    })
    .finally(() => {
      refreshPromise = null;
    });

  return refreshPromise;
}

export async function authFetch(input, init = {}) {
  const execute = token => fetch(input, {
    ...init,
    headers: {
      ...(init.headers || {}),
      ...(token ? { Authorization: 'Bearer ' + token } : {})
    }
  });

  let response = await execute(getAccessToken());
  if (response.status !== 401) return response;

  try {
    const token = await refreshAccessToken();
    response = await execute(token);
  } catch (error) {
    redirectToLogin();
    throw error;
  }

  if (response.status === 401) {
    redirectToLogin();
  }
  return response;
}

api.interceptors.request.use(config => {
  const token = getAccessToken();
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
  async error => {
    if (error.response) {
      const status = error.response.status;
      const original = error.config;

      if (
        status === 401
        && original
        && !original._retry
        && !isAuthEndpoint(original.url)
      ) {
        original._retry = true;
        try {
          const token = await refreshAccessToken();
          original.headers = {
            ...(original.headers || {}),
            Authorization: 'Bearer ' + token
          };
          return api(original);
        } catch (refreshError) {
          redirectToLogin('登录已过期，请重新登录');
          return Promise.reject(refreshError);
        }
      }

      if (status === 401) {
        if (isAuthEndpoint(original?.url)) {
          ElMessage.error(error.response.data?.msg || '认证失败');
        } else {
          redirectToLogin(error.response.data?.msg || '登录已过期');
        }
      } else if (status === 403) {
        ElMessage.error(error.response.data?.msg || 'no permission');
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
  refresh: () => refreshClient.post('/user/refresh'),
  logout: () => api.post('/user/logout'),
  publicKey: () => api.get('/user/public-key'),
  register: (data) => api.post('/user/register', data),
  info: (id) => api.get('/user/info/' + id),
  update: (data) => api.put('/user/update', data),
  changePassword: (data) => api.put('/user/password', data),
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
  // 指标引擎：TIR / CV / GMI 等（服务端确定性计算，模型只做解释）
  bloodSugarMetrics: (userId, params) => api.get('/record/bloodSugar/metrics/' + userId, { params }),
  // 日聚合趋势（含今日实时兜底与断档补齐）
  bloodSugarDaily: (userId, params) => api.get('/record/bloodSugar/daily/' + userId, { params }),
  // 手动补齐历史聚合（默认最近 7 天）
  backfillDaily: (userId, params) => api.post('/record/bloodSugar/daily/backfill/' + userId, null, { params }),
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
  updateUserStatus: (id, status) => api.put(`/admin/user/${id}/status`, { status }),
  auditList: (params) => api.get('/admin/audit/list', { params }),
  stats: () => api.get('/admin/stats'),
};

// ============ Dashboard API ============
export const dashboardApi = {
  get: (userId, params) => api.get('/dashboard/' + userId, { params }),
};

// ============ Export API（任务制导出） ============
export const exportApi = {
  submit: (data) => api.post('/export/tasks', data),
  list: (params) => api.get('/export/tasks', { params }),
  detail: (id) => api.get('/export/tasks/' + id),
  retry: (id) => api.post('/export/tasks/' + id + '/retry'),
  // 文件流：必须指定 responseType，否则二进制会被当文本解析
  download: (id) => api.get('/export/tasks/' + id + '/download', { responseType: 'blob' }),
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
