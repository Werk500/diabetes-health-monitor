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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { UserFilled } from '@element-plus/icons-vue'
import { userApi } from '../api/index'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const formRef = ref(null)
const saving = ref(false)
const form = ref({
  id: null,
  username: '',
  realName: '',
  phone: ''
})

const rules = {
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }]
}

onMounted(async () => {
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
    await userApi.update({ id: form.value.id, realName: form.value.realName, phone: form.value.phone })
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
</script>

<style scoped>
.profile-page { max-width: 600px; margin: 0 auto; }
.card-header { display: flex; align-items: center; gap: 8px; font-size: 16px; font-weight: 600; }
.profile-form { margin-top: 8px; }
</style>
