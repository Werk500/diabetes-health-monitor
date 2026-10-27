<template>
  <section class="user-manage">
    <header class="page-heading">
      <div>
        <p class="eyebrow">GOVERNANCE / USERS</p>
        <h1>用户治理</h1>
        <p>查询账号并处理启用状态。管理员账号受保护，不能在此页面停用。</p>
      </div>
      <span class="total-chip">共 {{ total }} 个账号</span>
    </header>

    <section class="table-panel">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          clearable
          placeholder="搜索用户名或姓名"
          class="search-input"
          @clear="search"
          @keyup.enter="search"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" :loading="loading" @click="search">搜索</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="users"
        row-key="id"
        class="user-table"
        empty-text="没有找到匹配的用户"
      >
        <el-table-column prop="id" label="ID" width="72" />
        <el-table-column prop="username" label="用户名" min-width="140">
          <template #default="{ row }">
            <span class="username-cell">{{ row.username }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="realName" label="姓名" min-width="110">
          <template #default="{ row }">{{ row.realName || '未填写' }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="140">
          <template #default="{ row }">{{ row.phone || '未填写' }}</template>
        </el-table-column>
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 1 ? 'warning' : 'info'" effect="plain" size="small">
              {{ row.role === 1 ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <span class="status-cell" :class="{ disabled: row.status !== 1 }">
              <span class="status-dot"></span>
              {{ row.status === 1 ? '启用' : '已禁用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" min-width="170" />
        <el-table-column label="操作" width="128" fixed="right">
          <template #default="{ row }">
            <span v-if="row.role === 1" class="protected-label">
              <el-icon><Lock /></el-icon>
              受保护
            </span>
            <el-button
              v-else
              size="small"
              :type="row.status === 1 ? 'warning' : 'success'"
              plain
              :loading="busyId === row.id"
              @click="toggle(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          :total="total"
          :page-size="pageSize"
          :disabled="loading"
          layout="prev, pager, next"
          background
          @current-change="load"
        />
      </div>
    </section>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { Lock, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '../../api'

const users = ref([])
const keyword = ref('')
const page = ref(1)
const pageSize = 10
const total = ref(0)
const loading = ref(false)
const busyId = ref(null)

const load = async () => {
  loading.value = true

  try {
    const response = await adminApi.userList({
      page: page.value,
      size: pageSize,
      keyword: keyword.value.trim()
    })

    if (response.data.code !== 200) {
      throw new Error(response.data.msg || '用户列表加载失败')
    }

    users.value = response.data.data.records || []
    total.value = Number(response.data.data.total) || 0
  } catch (error) {
    users.value = []
    total.value = 0
    ElMessage.error(error?.response?.data?.msg || error?.message || '用户列表加载失败')
  } finally {
    loading.value = false
  }
}

const search = () => {
  page.value = 1
  load()
}

const toggle = async row => {
  const nextAction = row.status === 1 ? '禁用' : '启用'
  const description = row.status === 1
    ? '禁用后，该账号已登录会话会被吊销，需要重新登录才能继续访问。'
    : '启用后，该账号可以重新登录系统。'

  try {
    await ElMessageBox.confirm(description, `确认${nextAction}用户「${row.username}」？`, {
      type: row.status === 1 ? 'warning' : 'info',
      confirmButtonText: nextAction,
      cancelButtonText: '取消'
    })

    busyId.value = row.id
    const response = await adminApi.updateUserStatus(row.id, row.status === 1 ? 0 : 1)
    if (response.data.code !== 200) {
      throw new Error(response.data.msg || `${nextAction}失败`)
    }

    ElMessage.success(`${nextAction}成功`)
    await load()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(error?.response?.data?.msg || error?.message || `${nextAction}失败`)
  } finally {
    busyId.value = null
  }
}

onMounted(load)
</script>

<style scoped>
.user-manage {
  color: var(--text-primary);
}

.page-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 24px;
}

.eyebrow {
  margin: 0 0 8px;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 1.8px;
  color: var(--neon-blue);
}

h1 {
  margin: 0;
  font-size: clamp(28px, 4vw, 42px);
  font-weight: 600;
  letter-spacing: -1.2px;
  line-height: 1.1;
}

.page-heading p:not(.eyebrow) {
  margin: 10px 0 0;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.7;
}

.total-chip {
  flex-shrink: 0;
  padding: 7px 10px;
  font-family: var(--font-mono);
  font-size: 10px;
  color: var(--text-secondary);
  background: rgba(17, 24, 39, 0.65);
  border: 1px solid rgba(51, 65, 85, 0.42);
  border-radius: 7px;
}

.table-panel {
  padding: 18px;
  background: linear-gradient(145deg, rgba(20, 29, 44, 0.82), rgba(13, 19, 30, 0.72));
  border: 1px solid rgba(51, 65, 85, 0.38);
  border-radius: 14px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.search-input {
  width: min(100%, 310px);
}

.user-table {
  margin-top: 4px;
}

.username-cell {
  font-family: var(--font-mono);
  font-size: 12px;
  color: #c4eaff;
}

.status-cell {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: #83ddba;
  font-size: 11px;
}

.status-cell.disabled {
  color: #8d98a8;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  box-shadow: 0 0 7px currentColor;
}

.protected-label {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--text-muted);
  font-size: 11px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

@media (max-width: 720px) {
  .page-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .search-input {
    width: 100%;
  }
}
</style>
