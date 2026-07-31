<template>
  <div class="login-container">
    <div class="login-card">
      <h1>用户注册</h1>
      <p class="subtitle">创建账号，开始健康管理</p>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="确认密码" prop="repassword">
          <el-input v-model="form.repassword" type="password" placeholder="请确认密码" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="年龄" prop="age">
          <el-input-number v-model="form.age" :min="1" :max="120" />
        </el-form-item>
        <el-form-item label="身高(cm)">
          <el-input-number v-model="form.height" :min="100" :max="250" :precision="1" />
        </el-form-item>
        <el-form-item label="糖尿病类型">
          <el-select v-model="form.diabetesType" placeholder="请选择">
            <el-option :value="1" label="1型糖尿病" />
            <el-option :value="2" label="2型糖尿病" />
            <el-option :value="3" label="妊娠期糖尿病" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="register" :loading="loading" style="width: 100%">注 册</el-button>
        </el-form-item>
        <div class="footer-link">
          <span>已有账号？</span>
          <router-link to="/login">返回登录</router-link>
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
const form = reactive({
  username: '', password: '', repassword: '', realName: '',
  gender: 1, age: 40, height: 170.0, diabetesType: 2
});

const validatePass = (rule, value, callback) => {
  if (value !== form.password) callback(new Error('两次密码不一致'));
  else callback();
};
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  repassword: [{ required: true, validator: validatePass, trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  age: [{ required: true, message: '请输入年龄', trigger: 'blur' }],
};

const register = async () => {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    loading.value = true;
    try {
      const res = await userApi.register(form);
      if (res.data.code === 200) {
        ElMessage.success('注册成功，请登录');
        router.push('/login');
      } else {
        ElMessage.error(res.data.msg);
      }
    } catch (e) {
      ElMessage.error('注册失败');
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
  width: 480px; padding: 40px; background: #fff; border-radius: 12px; box-shadow: 0 20px 60px rgba(0,0,0,0.3);
}
.login-card h1 { text-align: center; color: #303133; font-size: 24px; margin-bottom: 8px; }
.subtitle { text-align: center; color: #909399; font-size: 14px; margin-bottom: 30px; }
.footer-link { text-align: center; color: #909399; font-size: 14px; margin-top: 12px; }
.footer-link a { color: #667eea; margin-left: 4px; }
</style>