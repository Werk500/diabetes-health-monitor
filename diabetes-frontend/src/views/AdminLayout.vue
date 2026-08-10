<template>
  <el-container class="admin-layout">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="layout-aside">
      <div class="aside-header" @click="router.push('/admin/dashboard')">
        <div class="logo-icon">
          <svg viewBox="0 0 36 36" width="36" height="36">
            <circle cx="18" cy="18" r="17" fill="none" stroke="#f56c6c" stroke-width="1.5"/>
            <path d="M14 11 L22 11 L22 16 L14 16Z" fill="#f56c6c" opacity="0.6"/>
            <path d="M15 16 L18 25 L21 16" fill="#f56c6c" opacity="0.4"/>
            <rect x="13" y="26" width="10" height="2" rx="1" fill="#f56c6c" opacity="0.5"/>
          </svg>
        </div>
        <span v-show="!isCollapse" class="logo-text">后台管理</span>
      </div>
      <el-scrollbar>
        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapse"
          :collapse-transition="false"
          background-color="#1f2d3d"
          text-color="#bfcbd9"
          active-text-color="#f56c6c"
          router
        >
          <el-menu-item index="/admin/dashboard">
            <el-icon><DataAnalysis /></el-icon>
            <template #title>管理仪表盘</template>
          </el-menu-item>
          <el-menu-item index="/admin/users">
            <el-icon><UserFilled /></el-icon>
            <template #title>用户管理</template>
          </el-menu-item>
          <el-menu-item index="/admin/articles">
            <el-icon><Document /></el-icon>
            <template #title>文章管理</template>
          </el-menu-item>
          <el-menu-item index="/admin/exercise">
            <el-icon><Bicycle /></el-icon>
            <template #title>运动类型管理</template>
          </el-menu-item>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <el-icon class="collapse-btn" :size="22" @click="toggleCollapse">
            <Fold v-if="!isCollapse" /><Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/admin/dashboard' }">管理后台</el-breadcrumb-item>
            <el-breadcrumb-item>{{ pageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown trigger="click">
            <div class="user-info">
              <el-avatar :size="32" style="background:#f56c6c">
                {{ (userInfo?.realName || userInfo?.username || 'A').charAt(0) }}
              </el-avatar>
              <span class="username">{{ userInfo?.realName || userInfo?.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/user/dashboard')">切换到用户端</el-dropdown-item>
                <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Fold, Expand, ArrowDown, DataAnalysis, UserFilled, Document, Bicycle } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const userInfo = userStore.userInfo
const isCollapse = ref(false)
const activeMenu = computed(() => route.path)
const pageTitle = computed(() => route.meta?.title || '管理仪表盘')

const toggleCollapse = () => { isCollapse.value = !isCollapse.value }
const logout = () => { userStore.logout(); router.push('/login') }
</script>

<style scoped>
.admin-layout { min-height: 100vh; }
.layout-aside {
  background: #1f2d3d;
  transition: width 0.3s;
  overflow: hidden;
}
.aside-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 18px 16px;
  cursor: pointer;
  border-bottom: 1px solid #2d3a4a;
}
.logo-icon { flex-shrink: 0; }
.logo-text {
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  white-space: nowrap;
}
.el-menu { border-right: none; }
.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 20px;
  height: 56px;
}
.header-left { display: flex; align-items: center; gap: 14px; }
.collapse-btn { cursor: pointer; color: #606266; transition: color 0.3s; }
.collapse-btn:hover { color: #f56c6c; }
.header-right { display: flex; align-items: center; gap: 18px; }
.user-info { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.username { font-size: 14px; color: #303133; }
.layout-main {
  background: #f0f2f5;
  padding: 20px;
  min-height: calc(100vh - 56px);
}
</style>