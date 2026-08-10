<template>
  <el-container class="user-layout">
    <!-- 侧边栏：深空背景 + 霓虹光效 -->
    <el-aside :width="isCollapse ? '68px' : '240px'" class="layout-aside">
      <!-- Logo -->
      <div class="aside-header" @click="router.push('/user/dashboard')">
        <div class="logo-svg">
          <svg viewBox="0 0 40 40" width="36" height="36">
            <defs>
              <linearGradient id="sideGrad" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0%" stop-color="#00d4ff"/>
                <stop offset="100%" stop-color="#a855f7"/>
              </linearGradient>
            </defs>
            <circle cx="20" cy="20" r="18" fill="none" stroke="url(#sideGrad)" stroke-width="1.5" opacity="0.7"/>
            <path d="M12 20 Q16 11 20 10 Q24 11 28 20 Q29 26 20 30 Q11 26 12 20Z" fill="url(#sideGrad)" opacity="0.2"/>
            <circle cx="18" cy="17" r="1.2" fill="#00d4ff"/>
            <circle cx="22" cy="17" r="1.2" fill="#00d4ff"/>
          </svg>
        </div>
        <span v-show="!isCollapse" class="logo-text">健康监测</span>
      </div>

      <!-- 菜单 -->
      <el-scrollbar>
        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapse"
          :collapse-transition="false"
          background-color="transparent"
          text-color="#94a3b8"
          active-text-color="#00d4ff"
          router
          class="cyber-menu"
        >
          <el-menu-item index="/user/dashboard">
            <el-icon><DataAnalysis /></el-icon>
            <template #title>仪表盘</template>
          </el-menu-item>
          <el-menu-item index="/user/body">
            <el-icon><ScaleToOriginal /></el-icon>
            <template #title>身体记录</template>
          </el-menu-item>
          <el-menu-item index="/user/bloodSugar">
            <el-icon><Watermelon /></el-icon>
            <template #title>血糖记录</template>
          </el-menu-item>
          <el-menu-item index="/user/diet">
            <el-icon><DishDot /></el-icon>
            <template #title>饮食记录</template>
          </el-menu-item>
          <el-menu-item index="/user/exercise">
            <el-icon><Bicycle /></el-icon>
            <template #title>运动记录</template>
          </el-menu-item>

          <div class="menu-divider"></div>

          <el-menu-item index="/user/ai-chat">
            <el-icon><ChatDotRound /></el-icon>
            <template #title>AI 智能对话</template>
          </el-menu-item>
          <el-menu-item index="/user/ai-analysis">
            <el-icon><TrendCharts /></el-icon>
            <template #title>AI 智能分析</template>
          </el-menu-item>
          <el-menu-item index="/user/food-recognition">
            <el-icon><Camera /></el-icon>
            <template #title>食物拍照识别</template>
          </el-menu-item>

          <div class="menu-divider"></div>

          <el-menu-item index="/user/health-report">
            <el-icon><DocumentCopy /></el-icon>
            <template #title>健康报告</template>
          </el-menu-item>
          <el-menu-item index="/user/articles">
            <el-icon><Document /></el-icon>
            <template #title>健康文章</template>
          </el-menu-item>
        </el-menu>
      </el-scrollbar>

      <!-- 底部版本信息 -->
      <div class="aside-footer" v-show="!isCollapse">
        <div class="version-tag">
          <span class="dot"></span>
          SYSTEM v1.0
        </div>
      </div>
    </el-aside>

    <!-- 右侧主体 -->
    <el-container>
      <!-- 顶栏 -->
      <el-header class="layout-header">
        <div class="header-left">
          <div class="collapse-btn" @click="toggleCollapse">
            <el-icon :size="20"><Fold v-if="!isCollapse" /><Expand v-else /></el-icon>
          </div>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/user/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ pageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <div class="demo-toggle">
            <span>演示数据</span>
            <el-switch v-model="demoOn" size="small" @change="handleDemoToggle" />
          </div>
          <el-dropdown trigger="click">
            <div class="user-info">
              <el-avatar :size="30" style="background: linear-gradient(135deg, #00d4ff, #a855f7);">
                {{ (userInfo?.realName || userInfo?.username || 'U').charAt(0).toUpperCase() }}
              </el-avatar>
              <span class="username">{{ userInfo?.realName || userInfo?.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/user/body')">个人资料</el-dropdown-item>
                <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Fold, Expand, ArrowDown, DataAnalysis, ScaleToOriginal, Watermelon, DishDot, Bicycle, ChatDotRound, TrendCharts, Camera, DocumentCopy, Document } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import { demoEnabled, setDemoMode } from '../mock/demoManager'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const userInfo = userStore.userInfo
const isCollapse = ref(false)
const activeMenu = computed(() => route.path)
const pageTitle = computed(() => route.meta?.title || '仪表盘')
const demoOn = ref(demoEnabled.value)

const toggleCollapse = () => { isCollapse.value = !isCollapse.value }
const handleDemoToggle = (val) => { setDemoMode(val); location.reload() }
const logout = () => { userStore.logout(); router.push('/login') }
</script>

<style scoped>
.user-layout { min-height: 100vh; position: relative; z-index: 1; }

/* 侧边栏 */
.layout-aside {
  background: linear-gradient(180deg, rgba(10, 14, 23, 0.98), rgba(13, 17, 23, 0.96));
  border-right: 1px solid rgba(51, 65, 85, 0.3);
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.aside-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 18px;
  cursor: pointer;
  border-bottom: 1px solid rgba(51, 65, 85, 0.3);
}
.logo-svg { flex-shrink: 0; }
.logo-text {
  font-family: var(--font-mono);
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 3px;
  background: linear-gradient(90deg, var(--neon-blue), var(--neon-purple));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  white-space: nowrap;
}

/* 菜单 */
.cyber-menu {
  border-right: none !important;
  padding: 8px 0;
}
.cyber-menu :deep(.el-menu-item) {
  margin: 2px 8px;
  border-radius: 8px;
  transition: all 0.3s;
}
.cyber-menu :deep(.el-menu-item:hover) {
  background: rgba(0, 212, 255, 0.06) !important;
}
.cyber-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(0, 212, 255, 0.1), rgba(168, 85, 247, 0.08)) !important;
  border-right: 2px solid var(--neon-blue);
}
.menu-divider {
  height: 1px;
  margin: 8px 20px;
  background: linear-gradient(90deg, transparent, rgba(0, 212, 255, 0.15), transparent);
}

.aside-footer {
  margin-top: auto;
  padding: 14px 20px;
  border-top: 1px solid rgba(51, 65, 85, 0.2);
}
.version-tag {
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--text-muted);
  display: flex;
  align-items: center;
  gap: 6px;
}
.dot {
  width: 6px; height: 6px;
  background: var(--neon-green);
  border-radius: 50%;
  box-shadow: 0 0 6px var(--neon-green);
}

/* 顶栏 */
.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(13, 17, 23, 0.85);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(51, 65, 85, 0.3);
  padding: 0 24px;
  height: 56px;
}
.header-left { display: flex; align-items: center; gap: 16px; }
.collapse-btn {
  cursor: pointer;
  color: var(--text-secondary);
  transition: color 0.3s;
}
.collapse-btn:hover { color: var(--neon-blue); }

.header-right { display: flex; align-items: center; gap: 20px; }
.demo-toggle { display: flex; align-items: center; gap: 8px; font-size: 12px; color: var(--text-muted); }
.user-info { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.username { font-size: 14px; color: var(--text-secondary); }

/* 面包屑 */
.layout-header :deep(.el-breadcrumb__inner) { color: var(--text-muted) !important; }
.layout-header :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) { color: var(--text-accent) !important; }

/* 内容区 */
.layout-main {
  background: transparent;
  padding: 24px;
  min-height: calc(100vh - 56px);
}
</style>