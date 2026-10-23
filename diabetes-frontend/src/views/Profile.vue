<template>
  <div class="profile-page">
    <el-card shadow="hover" class="profile-card">
      <template #header>
        <div class="card-header">
          <el-icon :size="20" color="#00d4ff"><UserFilled /></el-icon>
          <span>个人资料</span>
        </div>
      </template>

      <el-form :model="form" label-width="100px" class="profile-form" :rules="rules" ref="formRef">
        <el-form-item label="用户名">
          <el-input v-model="form.username" disabled />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="save" :loading="saving">保存修改</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover" class="profile-card password-card">
      <template #header>
        <div class="card-header">
          <el-icon :size="20" color="#f59e0b"><Lock /></el-icon>
          <span>修改密码</span>
        </div>
      </template>

      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-width="100px"
        class="profile-form"
      >
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="6-32位新密码" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="warning" @click="changePassword" :loading="passwordSaving">修改密码</el-button>
          <span class="password-tip">修改成功后所有设备需要重新登录</span>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { UserFilled, Lock } from '@element-plus/icons-vue'
import { userApi } from '../api/index'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import JSEncrypt from 'jsencrypt'

const userStore = useUserStore()
const router = useRouter()
const formRef = ref(null)
const passwordFormRef = ref(null)
const saving = ref(false)
const passwordSaving = ref(false)
const publicKey = ref('')
const form = ref({
  id: null,
  username: '',
  realName: '',
  phone: ''
})
const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const rules = {
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }]
}
const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度需为6到32位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== passwordForm.value.newPassword) {
          callback(new Error('两次密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const encrypt = (text) => {
  if (!text || !publicKey.value) return text
  const jse = new JSEncrypt()
  jse.setPublicKey(publicKey.value)
  return jse.encrypt(text) || text
}

onMounted(async () => {
  try {
    const keyRes = await userApi.publicKey()
    if (keyRes.data.code === 200) {
      publicKey.value = keyRes.data.data
    }
  } catch (e) {
    ElMessage.error('加载加密公钥失败')
  }

  const userInfo = userStore.userInfo
  if (!userInfo || !userInfo.id) return
  try {
    const res = await userApi.info(userInfo.id)
    if (res.data.code === 200) {
      const u = res.data.data
      form.value = { id: u.id, username: u.username, realName: u.realName || '', phone: u.phone || '' }
    }
  } catch (e) {
    ElMessage.error('加载用户信息失败')
  }
})

const save = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await userApi.update({ realName: form.value.realName, phone: form.value.phone })
    ElMessage.success('保存成功')
    // 更新 store 中的用户信息
    const u = userStore.userInfo
    if (u) { u.realName = form.value.realName; u.phone = form.value.phone }
    userStore.userInfo = { ...u }
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const changePassword = async () => {
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return

  passwordSaving.value = true
  try {
    const res = await userApi.changePassword({
      oldPassword: encrypt(passwordForm.value.oldPassword),
      newPassword: encrypt(passwordForm.value.newPassword)
    })
    if (res.data.code === 200) {
      ElMessage.success('密码修改成功，请重新登录')
      await userStore.logout()
      router.push('/login')
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '密码修改失败')
  } finally {
    passwordSaving.value = false
  }
}
</script>

<style scoped>
.profile-page { max-width: 600px; margin: 0 auto; }
.card-header { display: flex; align-items: center; gap: 8px; font-size: 16px; font-weight: 600; }
.profile-form { margin-top: 8px; }
.password-card { margin-top: 18px; }
.password-tip { margin-left: 12px; color: #94a3b8; font-size: 12px; }
</style>
