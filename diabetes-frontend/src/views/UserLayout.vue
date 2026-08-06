<template>
  <el-container style="min-height:100vh">
    <el-aside width="220px" style="background:#1f2d3d">
      <div class="logo">
        <h2>閸嬨儱鎮嶉惄鎴炵ゴ</h2>
      </div>
      <el-menu :default-active="activeMenu" background-color="#1f2d3d" text-color="#bfcbd9" active-text-color="#409eff" router>
        <el-menu-item index="/user/dashboard"><el-icon><DataAnalysis /></el-icon><span>閸嬨儱鎮嶆禒顏囥€冮惄?/span></el-menu-item>
        <el-menu-item index="/user/body"><el-icon><ScaleToOriginal /></el-icon><span>闊偂缍嬮幐鍥ㄧ垼</span></el-menu-item>
        <el-menu-item index="/user/bloodSugar"><el-icon><Watermelon /></el-icon><span>鐞涒偓缁牞顔囪ぐ?/span></el-menu-item>
        <el-menu-item index="/user/diet"><el-icon><DishDot /></el-icon><span>妤楊噣顥ょ拋鏉跨秿</span></el-menu-item>
        <el-menu-item index="/user/exercise"><el-icon><Bicycle /></el-icon><span>鏉╂劕濮╃拋鏉跨秿</span></el-menu-item>
                <el-menu-item index="/user/ai-chat"><el-icon><ChatDotRound /></el-icon><span>AI 智能对话</span></el-menu-item>
        <el-menu-item index="/user/ai-analysis"><el-icon><TrendCharts /></el-icon><span>AI 智能分析</span></el-menu-item>
        <el-menu-item index="/user/food-recognition"><el-icon><Camera /></el-icon><span>食物拍照识别</span></el-menu-item>
        <el-menu-item index="/user/health-report"><el-icon><DocumentCopy /></el-icon><span>健康报告</span></el-menu-item>
        <el-menu-item index="/user/articles"><el-icon><Document /></el-icon><span>健康文章</span></el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header style="background:#fff; border-bottom:1px solid #dcdfe6; display:flex; align-items:center; justify-content:space-between; padding:0 20px">
        <div style="display:flex;align-items:center;gap:16px">
          <span style="font-size:16px;font-weight:bold">{{ pageTitle }}</span>
          <el-tooltip content="瀵偓閸氼垰鎮楅敍灞界秼閻喎鐤勯弫鐗堝祦娑撹櫣鈹栭弮璺虹殺鐏炴洜銇氱粈杞扮伐閺佺増宓佹禒銉よ荡鐎靛瞼鏅棃? placement="bottom">
            <div style="display:flex;align-items:center;gap:6px;font-size:12px;color:#909399">
              <span>缁€杞扮伐閺佺増宓?/span>
              <el-switch v-model="demoOn" size="small" @change="handleDemoToggle" />
            </div>
          </el-tooltip>
        </div>
        <div>
          <span style="color:#909399;margin-right:12px">{{ userInfo?.realName || userInfo?.username }}</span>
          <el-button type="danger" size="small" @click="logout">闁偓閸?/el-button>
        </div>
      </el-header>
      <el-main style="background:#f0f2f5;padding:20px">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { demoEnabled, setDemoMode } from '../mock/demoManager';
import { useUserStore } from '../stores/user';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const userInfo = userStore.userInfo;
const activeMenu = computed(() => route.path);
const pageTitle = computed(() => route.meta?.title || '閸嬨儱鎮嶆禒顏囥€冮惄?);
const demoOn = ref(demoEnabled.value);

const handleDemoToggle = (val) => {
  setDemoMode(val);
  // 閸掗攱鏌婅ぐ鎾冲妞ょ敻娼版禒銉ョ安閻劍娲块弨?  location.reload();
};

const logout = () => {
  userStore.logout();
  router.push('/login');
};
</script>

<style scoped>
.logo { padding: 20px; text-align: center; color: #fff; border-bottom: 1px solid #2d3a4a; }
.logo h2 { font-size: 18px; }
.el-menu { border-right: none; }
</style>