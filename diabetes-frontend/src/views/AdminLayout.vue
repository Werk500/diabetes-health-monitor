<template>
  <el-container class="admin-layout">
    <el-aside :width="isCollapse ? '68px' : '240px'" class="layout-aside">
      <div class="aside-header" @click="router.push('/admin/dashboard')">
        <div class="logo-mark">
          <svg viewBox="0 0 40 40" width="36" height="36" aria-hidden="true">
            <defs>
              <linearGradient id="adminLogoGradient" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0%" stop-color="#00d4ff" />
                <stop offset="100%" stop-color="#a855f7" />
              </linearGradient>
            </defs>
            <circle cx="20" cy="20" r="18" fill="none" stroke="url(#adminLogoGradient)" stroke-width="1.5" opacity="0.7" />
            <path d="M12 20 Q16 11 20 10 Q24 11 28 20 Q29 26 20 30 Q11 26 12 20Z" fill="url(#adminLogoGradient)" opacity="0.2" />
            <path d="M14 20h4l1.8-4 3 8 1.7-4H27" fill="none" stroke="#00d4ff" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </div>
        <div v-show="!isCollapse" class="brand-copy">
          <span class="logo-text">健康监测</span>
          <span class="logo-subtitle">ADMIN CONSOLE</span>
        </div>
      </div>

      <div v-show="!isCollapse" class="console-badge">
        <span class="pulse-dot"></span>
        管理服务在线
      </div>

      <el-scrollbar>
        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapse"
          :collapse-transition="false"
          router
          class="admin-menu"
        >
          <el-menu-item index="/admin/dashboard">
            <el-icon><DataAnalysis /></el-icon>
            <template #title>运营概览</template>
          </el-menu-item>
          <el-menu-item index="/admin/users">
            <el-icon><UserFilled /></el-icon>
            <template #title>用户治理</template>
          </el-menu-item>
          <el-menu-item index="/admin/audit">
            <el-icon><Tickets /></el-icon>
            <template #title>操作审计</template>
          </el-menu-item>

          <div class="menu-divider"></div>

          <el-menu-item index="/admin/articles">
            <el-icon><Document /></el-icon>
            <template #title>内容运营</template>
          </el-menu-item>
          <el-menu-item index="/admin/exercise">
            <el-icon><Bicycle /></el-icon>
            <template #title>运动配置</template>
          </el-menu-item>
        </el-menu>
      </el-scrollbar>

      <div v-show="!isCollapse" class="aside-footer">
        <div class="version-tag">
          <span class="version-dot"></span>
          OPERATIONS v1.0
        </div>
      </div>
    </el-aside>

    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <button class="collapse-btn" type="button" aria-label="折叠侧边栏" @click="toggleCollapse">
            <el-icon :size="20"><Fold v-if="!isCollapse" /><Expand v-else /></el-icon>
          </button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/admin/dashboard' }">管理控制台</el-breadcrumb-item>
            <el-breadcrumb-item>{{ pageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <span class="role-indicator">ADMIN</span>
          <el-dropdown trigger="click">
            <div class="user-info">
              <el-avatar :size="32" class="admin-avatar">
                {{ (userInfo?.realName || userInfo?.username || 'A').charAt(0).toUpperCase() }}
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
        <div class="page-container">
          <router-view />
        </div>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowDown,
  Bicycle,
  DataAnalysis,
  Document,
  Expand,
  Fold,
  Tickets,
  UserFilled
} from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const userInfo = userStore.userInfo
const isCollapse = ref(false)
const activeMenu = computed(() => route.path)
const pageTitle = computed(() => route.meta?.title || '运营概览')

const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

const logout = async () => {
  await userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.admin-layout {
  min-height: 100dvh;
  position: relative;
  z-index: 1;
}

.layout-aside {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: linear-gradient(180deg, rgba(10, 14, 23, 0.98), rgba(13, 17, 23, 0.96));
  border-right: 1px solid rgba(51, 65, 85, 0.32);
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.aside-header {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 20px 18px;
  cursor: pointer;
  border-bottom: 1px solid rgba(51, 65, 85, 0.28);
}

.logo-mark {
  flex-shrink: 0;
  filter: drop-shadow(0 0 10px rgba(0, 212, 255, 0.18));
}

.brand-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.logo-text {
  font-family: var(--font-mono);
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 2px;
  color: var(--text-primary);
  white-space: nowrap;
}

.logo-subtitle {
  font-family: var(--font-mono);
  font-size: 9px;
  letter-spacing: 1.8px;
  color: var(--text-muted);
  white-space: nowrap;
}

.console-badge {
  display: flex;
  align-items: center;
  gap: 7px;
  margin: 14px 14px 4px;
  padding: 8px 10px;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.5px;
  color: #8de6d1;
  background: rgba(16, 185, 129, 0.06);
  border: 1px solid rgba(16, 185, 129, 0.16);
  border-radius: 8px;
}

.pulse-dot,
.version-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--neon-green);
  box-shadow: 0 0 8px rgba(16, 185, 129, 0.7);
}

.pulse-dot {
  animation: admin-pulse 2.2s ease-in-out infinite;
}

.admin-menu {
  --el-menu-bg-color: transparent;
  --el-menu-hover-bg-color: transparent;
  --el-menu-text-color: var(--text-secondary);
  --el-menu-active-color: var(--neon-blue);
  background: transparent !important;
  border-right: 0 !important;
  padding: 8px 0;
}

.layout-aside :deep(.el-scrollbar__view),
.layout-aside :deep(.el-scrollbar__wrap) {
  background: transparent;
}

.admin-menu :deep(.el-menu-item) {
  margin: 3px 8px;
  border-radius: 8px;
  color: var(--text-secondary) !important;
  transition:
    color 0.2s ease,
    background-color 0.2s ease,
    transform 0.2s ease;
}

.admin-menu :deep(.el-menu-item:hover) {
  background: rgba(0, 212, 255, 0.06) !important;
  color: var(--text-primary) !important;
}

.admin-menu :deep(.el-menu-item.is-active) {
  color: var(--neon-blue) !important;
  background: linear-gradient(90deg, rgba(0, 212, 255, 0.12), rgba(168, 85, 247, 0.06)) !important;
  box-shadow: inset 2px 0 0 var(--neon-blue);
}

.menu-divider {
  height: 1px;
  margin: 10px 20px;
  background: linear-gradient(90deg, transparent, rgba(0, 212, 255, 0.16), transparent);
}

.aside-footer {
  margin-top: auto;
  padding: 14px 20px 18px;
  border-top: 1px solid rgba(51, 65, 85, 0.2);
}

.version-tag {
  display: flex;
  align-items: center;
  gap: 7px;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.6px;
  color: var(--text-muted);
}

.layout-header {
  display: flex;
  height: 56px;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: rgba(13, 17, 23, 0.84);
  border-bottom: 1px solid rgba(51, 65, 85, 0.3);
  backdrop-filter: blur(14px);
}

.header-left,
.header-right,
.user-info {
  display: flex;
  align-items: center;
}

.header-left {
  gap: 16px;
}

.header-right {
  gap: 16px;
}

.collapse-btn {
  display: grid;
  width: 32px;
  height: 32px;
  padding: 0;
  place-items: center;
  color: var(--text-secondary);
  background: transparent;
  border: 0;
  border-radius: 8px;
  transition:
    color 0.2s ease,
    background-color 0.2s ease;
}

.collapse-btn:hover {
  color: var(--neon-blue);
  background: rgba(0, 212, 255, 0.06);
}

.collapse-btn:focus-visible {
  outline: 2px solid var(--neon-blue);
  outline-offset: 2px;
}

.role-indicator {
  padding: 4px 7px;
  font-family: var(--font-mono);
  font-size: 9px;
  letter-spacing: 1.2px;
  color: var(--neon-blue);
  background: rgba(0, 212, 255, 0.06);
  border: 1px solid rgba(0, 212, 255, 0.16);
  border-radius: 5px;
}

.user-info {
  gap: 8px;
  cursor: pointer;
}

.admin-avatar {
  color: #041018;
  font-weight: 700;
  background: linear-gradient(135deg, #00d4ff, #a855f7);
}

.username {
  color: var(--text-secondary);
  font-size: 13px;
}

.layout-header :deep(.el-breadcrumb__inner) {
  color: var(--text-muted) !important;
}

.layout-header :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: var(--text-accent) !important;
}

.layout-main {
  min-height: calc(100dvh - 56px);
  padding: 24px;
  background: transparent;
}

.page-container {
  width: min(100%, 1440px);
  margin: 0 auto;
}

@keyframes admin-pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.45;
  }
}

@media (max-width: 720px) {
  .layout-header {
    padding: 0 14px;
  }

  .layout-main {
    padding: 16px;
  }

  .role-indicator,
  .username {
    display: none;
  }
}
</style>
