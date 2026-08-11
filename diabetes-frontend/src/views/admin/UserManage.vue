<template>
  <div class="user-manage">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <el-icon :size="18" color="#409eff"><UserFilled /></el-icon>
          <span>用户管理</span>
        </div>
      </template>

      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索用户名或姓名" style="width:240px" clearable @clear="load" @keyup.enter="load" />
        <el-button type="primary" @click="load" style="margin-left:12px">搜索</el-button>
      </div>

      <el-table :data="users" stripe class="user-table">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column label="性别" width="60">
          <template #default="s">{{ ['未知','男','女'][s.row.gender] || '未知' }}</template>
        </el-table-column>
        <el-table-column prop="age" label="年龄" width="60" />
        <el-table-column label="角色" width="80">
          <template #default="s">
            <el-tag :type="s.row.role === 1 ? 'danger' : ''" size="small">
              {{ s.row.role === 1 ? '管理员' : '用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="s">
            <el-tag :type="s.row.status === 1 ? 'success' : 'danger'" size="small">
              {{ s.row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="170" />
        <el-table-column label="操作" width="140">
          <template #default="s">
            <el-button v-if="s.row.role !== 1" type="danger" size="small" plain @click="del(s.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          :total="total"
          :page-size="10"
          layout="prev, pager, next"
          background
          @current-change="load"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { UserFilled } from '@element-plus/icons-vue'
import { adminApi } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const users = ref([])
const keyword = ref('')
const page = ref(1)
const total = ref(0)

const load = async () => {
  const res = await adminApi.userList({ page: page.value, size: 10, keyword: keyword.value })
  if (res.data.code === 200) {
    users.value = res.data.data.records
    total.value = res.data.data.total
  }
}

const toggle = async (id, currentStatus) => {
  const action = currentStatus === 1 ? '绂佺敤' : '鍚敤'
  await ElMessageBox.confirm(`纭畾${action}璇ョ敤鎴凤紵`, '鎻愮ず', { type: 'warning' })
  await adminApi.toggleStatus(id)
  ElMessage.success(`${action}鎴愬姛`)
  load()
}

const del = async (id) => {
  await ElMessageBox.confirm('确定删除该用户？', '提示', { type: 'warning' })
  await adminApi.deleteUser(id)
  ElMessage.success('删除成功')
  load()
}

load()
</script>

<style scoped>
.user-manage { max-width: 1200px; }
.card-header { display: flex; align-items: center; gap: 8px; font-size: 16px; font-weight: 600; }
.toolbar { margin-bottom: 16px; display: flex; align-items: center; }
.user-table { margin-top: 8px; }
.pagination-wrap { display: flex; justify-content: center; margin-top: 20px; }
</style>