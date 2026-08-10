<template>
  <div class="register-page">
    <div class="hex-bg">
      <div v-for="i in 4" :key="i" class="hex" :style="hexStyle(i)"></div>
    </div>

    <div class="register-center" @mousemove="onCardTilt" @mouseleave="resetCardTilt">
      <div ref="regCard" class="cyber-card">
        <div class="card-accent"></div>

        <div class="card-header">
          <h2>创建账号</h2>
          <p>开始您的健康管理之旅</p>
        </div>

        <el-form :model="form" :rules="rules" ref="formRef" label-width="80px" size="default" class="register-form">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" class="cyber-input" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password class="cyber-input" />
          </el-form-item>
          <el-form-item label="确认密码" prop="repassword">
            <el-input v-model="form.repassword" type="password" placeholder="请确认密码" show-password class="cyber-input" />
          </el-form-item>
          <el-form-item label="姓名" prop="realName">
            <el-input v-model="form.realName" placeholder="请输入真实姓名" class="cyber-input" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="form.phone" placeholder="请输入手机号" class="cyber-input" />
          </el-form-item>
          <el-form-item label="性别">
            <el-radio-group v-model="form.gender">
              <el-radio :value="1">男</el-radio>
              <el-radio :value="2">女</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="年龄" prop="age">
            <el-input-number v-model="form.age" :min="1" :max="120" class="cyber-input-number" />
          </el-form-item>
          <el-form-item label="身高(cm)">
            <el-input-number v-model="form.height" :min="100" :max="250" :precision="1" class="cyber-input-number" />
          </el-form-item>
          <el-form-item label="糖尿病类型">
            <el-select v-model="form.diabetesType" placeholder="请选择" class="cyber-select">
              <el-option :value="1" label="1型糖尿病" />
              <el-option :value="2" label="2型糖尿病" />
              <el-option :value="3" label="妊娠期糖尿病" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="register" :loading="loading" class="cyber-btn">
              <span class="btn-text">注册</span>
            </el-button>
          </el-form-item>
        </el-form>

        <div class="card-footer">
          <span>已有账号？</span>
          <router-link to="/login" class="link-neon">返回登录</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { userApi } from '../api'
import { ElMessage } from 'element-plus'
import JSEncrypt from 'jsencrypt'

const router = useRouter()
const regCard = ref(null)
const formRef = ref(null)
const loading = ref(false)
const form = reactive({
  username: '', password: '', repassword: '', realName: '', phone: '',
  gender: 1, age: 40, height: 170.0, diabetesType: 2
})

const validatePass = (rule, value, callback) => {
  if (value !== form.password) callback(new Error('两次密码不一致'))
  else callback()
}
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  repassword: [{ required: true, validator: validatePass, trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  age: [{ required: true, message: '请输入年龄', trigger: 'blur' }]
}

// ==================== RSA 加密 ====================
const publicKey = ref('')
const encrypt = (text) => {
  if (!text || !publicKey.value) return text
  const jse = new JSEncrypt()
  jse.setPublicKey(publicKey.value)
  return jse.encrypt(text) || text
}

const hexStyle = (i) => ({
  left: `${15 + (i - 1) * 23}%`,
  top: `${10 + (i % 2) * 35}%`,
  animationDelay: `${i * 0.7}s`
})


// ==================== 卡片倾斜跟手效果 ====================
function onCardTilt(e) {
  if (!regCard.value) return
  const rect = regCard.value.getBoundingClientRect()
  const centerX = rect.left + rect.width / 2
  const centerY = rect.top + rect.height / 2
  const rotateX = ((e.clientY - centerY) / (rect.height / 2)) * -3
  const rotateY = ((e.clientX - centerX) / (rect.width / 2)) * 3
  regCard.value.style.transition = 'transform 0.15s ease-out'
  regCard.value.style.transform = `perspective(1000px) rotateX(${rotateX}deg) rotateY(${rotateY}deg)`
}
function resetCardTilt() {
  if (!regCard.value) return
  regCard.value.style.transition = 'transform 0.6s ease-out'
  regCard.value.style.transform = 'perspective(1000px) rotateX(0deg) rotateY(0deg)'
}

onMounted(async () => {
  try {
    const res = await fetch('/api/user/public-key')
    const data = await res.json()
    if (data.code === 200) publicKey.value = data.data
  } catch (e) { console.error('获取公钥失败', e) }
})

const register = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const encryptedForm = { ...form, password: encrypt(form.password) }
      const res = await userApi.register(encryptedForm)

      if (res.data.code === 200) {
        ElMessage.success('注册成功，请登录')
        router.push('/login')
      } else { ElMessage.error(res.data.msg) }
    } catch (e) { ElMessage.error('注册失败') }
    loading.value = false
  })
}
</script>

<style scoped>
.register-page {
  position: relative; z-index: 1;
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  overflow: hidden;
}
.hex-bg { position: absolute; inset: 0; pointer-events: none; }
.hex {
  position: absolute; width: 120px; height: 140px;
  background: transparent;
  border: 1px solid rgba(0, 212, 255, 0.05);
  clip-path: polygon(50% 0, 100% 25%, 100% 75%, 50% 100%, 0 75%, 0 25%);
  animation: hex-float 8s ease-in-out infinite;
}
@keyframes hex-float {
  0%, 100% { transform: translateY(0) rotate(0deg); opacity: 0.2; }
  50% { transform: translateY(-15px) rotate(3deg); opacity: 0.5; }
}

.register-center { position: relative; z-index: 2; }
.cyber-card {
  width: 540px; padding: 40px 44px 32px;
  background: linear-gradient(135deg, rgba(17, 24, 39, 0.85), rgba(15, 23, 42, 0.9));
  backdrop-filter: blur(24px);
  border: 1px solid rgba(0, 212, 255, 0.12);
  border-radius: 20px;
  position: relative; overflow: hidden;
}
.cyber-card::after {
  content: ''; position: absolute; inset: 0; border-radius: 20px; padding: 1px;
  background: linear-gradient(135deg, rgba(0, 212, 255, 0.25), transparent 40%, transparent 60%, rgba(168, 85, 247, 0.25));
  -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor; mask-composite: exclude; pointer-events: none;
}
.card-accent {
  position: absolute; top: 0; left: 0; right: 0; height: 2px;
  background: linear-gradient(90deg, transparent, var(--neon-blue), var(--neon-purple), transparent);
  opacity: 0.7;
}
.card-header { text-align: center; margin-bottom: 30px; }
.card-header h2 {
  font-family: var(--font-mono); font-size: 22px; font-weight: 700; letter-spacing: 4px;
  background: linear-gradient(90deg, var(--neon-blue), var(--neon-purple));
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;
}
.card-header p { font-size: 13px; color: var(--text-muted); margin-top: 6px; }

.register-form :deep(.el-form-item__label) { color: var(--text-secondary) !important; }
.cyber-input :deep(.el-input__wrapper) {
  background: rgba(17, 24, 39, 0.6) !important;
  border: 1px solid rgba(51, 65, 85, 0.4) !important;
}
.cyber-input :deep(.el-input__wrapper:hover) { border-color: rgba(0, 212, 255, 0.25) !important; }
.cyber-input :deep(.el-input__wrapper.is-focus) {
  border-color: var(--neon-blue) !important;
  box-shadow: 0 0 8px rgba(0, 212, 255, 0.12) !important;
}
.cyber-input :deep(.el-input__inner) { color: #e2e8f0 !important; }
.cyber-input :deep(.el-input__inner::placeholder) { color: #64748b !important; }

.cyber-input-number :deep(.el-input__wrapper) {
  background: rgba(17, 24, 39, 0.6) !important;
  border: 1px solid rgba(51, 65, 85, 0.4) !important;
}

.cyber-select :deep(.el-input__wrapper) {
  background: rgba(17, 24, 39, 0.6) !important;
  border: 1px solid rgba(51, 65, 85, 0.4) !important;
}

.cyber-btn {
  width: 100% !important; height: 44px !important;
  font-size: 16px !important; letter-spacing: 6px !important;
}

.card-footer {
  text-align: center; color: var(--text-muted); font-size: 13px; margin-top: 8px;
}
.link-neon {
  color: var(--neon-blue); margin-left: 4px; text-decoration: none;
  transition: all 0.3s;
}
.link-neon:hover { text-shadow: 0 0 8px rgba(0, 212, 255, 0.5); }

@media screen and (max-width: 600px) {
  .cyber-card { width: 94%; padding: 28px 20px 24px; }
}
</style>
