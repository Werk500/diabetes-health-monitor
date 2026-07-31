<template>
  <div class="login-container">
    <div class="login-card">
      <h1>糖尿病人健康监测系统</h1>
      <p class="subtitle">记录健康数据 · 守护血糖健康</p>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="0">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" size="large" @keyup.enter="login" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" @click="login" :loading="loading" style="width: 100%">登 录</el-button>
        </el-form-item>
        <div class="footer-link">
          <span>还没有账号？</span>
          <router-link to="/register">立即注册</router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { userApi } from '../api';
import { ElMessage } from 'element-plus';

const router = useRouter();
const formRef = ref(null);
const loading = ref(false);
const form = reactive({ username: '', password: '' });
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
};

const login = async () => {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    loading.value = true;
    try {
      const res = await userApi.login(form);
      if (res.data.code === 200) {
        const { token, user } = res.data.data;
        import { useUserStore } from '../stores/user'
        const userStore = useUserStore()
        userStore.setToken(res.data.data.token)
        userStore.setUserInfo(res.data.data.user)
        ElMessage.success('登录成功');
        router.push(user.role === 1 ? '/admin/dashboard' : '/user/dashboard');
      } else {
        ElMessage.error(res.data.msg);
      }
    } catch (e) {
      ElMessage.error('登录失败，请检查网络');
    }
    loading.value = false;
  });
};
</script>

<style scoped>
.login-container {
  display: flex; justify-content: center; align-items: center;
  min-height: 100vh; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 420px; padding: 40px; background: #fff; border-radius: 12px; box-shadow: 0 20px 60px rgba(0,0,0,0.3);
}
.login-card h1 { text-align: center; color: #303133; font-size: 24px; margin-bottom: 8px; }
.subtitle { text-align: center; color: #909399; font-size: 14px; margin-bottom: 30px; }
.footer-link { text-align: center; color: #909399; font-size: 14px; }
.footer-link a { color: #667eea; margin-left: 4px; }
</style>