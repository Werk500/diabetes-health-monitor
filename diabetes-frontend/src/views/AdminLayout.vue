<template>
  <el-container style="min-height:100vh">
    <el-aside width="220px" style="background:#1f2d3d">
      <div class="logo"><h2>鍚庡彴绠＄悊</h2></div>
      <el-menu :default-active="activeMenu" background-color="#1f2d3d" text-color="#bfcbd9" active-text-color="#409eff" router>
        <el-menu-item index="/admin/dashboard"><el-icon><DataAnalysis /></el-icon><span>绠＄悊姒傝</span></el-menu-item>
        <el-menu-item index="/admin/users"><el-icon><User /></el-icon><span>鐢ㄦ埛绠＄悊</span></el-menu-item>
        <el-menu-item index="/admin/articles"><el-icon><Document /></el-icon><span>鏂囩珷绠＄悊</span></el-menu-item>
        <el-menu-item index="/admin/exercise"><el-icon><Bicycle /></el-icon><span>杩愬姩绫诲瀷绠＄悊</span></el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header style="background:#fff;border-bottom:1px solid #dcdfe6;display:flex;align-items:center;justify-content:flex-end;padding:0 20px">
        <span style="color:#909399;margin-right:12px">绠＄悊鍛? {{ userInfo?.realName }}</span>
        <el-button type="danger" size="small" @click="logout">閫€鍑?/el-button>
      </el-header>
      <el-main style="background:#f0f2f5;padding:20px">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const userInfo = userStore.userInfo;
const activeMenu = computed(() => route.path);
if (userInfo.role !== 1) router.push('/login');
const logout = () => { userStore.logout(); router.push('/login'); };
</script>

<style scoped>
.logo { padding: 20px; text-align: center; color: #fff; border-bottom: 1px solid #2d3a4a; }
.logo h2 { font-size: 18px; }
.el-menu { border-right: none; }
</style>