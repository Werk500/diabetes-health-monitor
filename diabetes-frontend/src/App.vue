<template>
  <div id="app-container" class="sci-fi-app">
    <SciFiBackground />
    <CursorEffect />
    <router-view v-slot="{ Component }">
      <transition name="cyber-fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </div>
</template>

<script setup>
import SciFiBackground from './components/SciFiBackground.vue'
import CursorEffect from './components/CursorEffect.vue'
</script>

<style>
@import './styles/theme.css';

.sci-fi-app {
  position: relative;
  min-height: 100vh;
}

/* 页面过渡动画：赛博淡入 + 缩放入场 */
.cyber-fade-enter-active {
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}
.cyber-fade-leave-active {
  transition: all 0.25s cubic-bezier(0.4, 0, 1, 1);
}
.cyber-fade-enter-from {
  opacity: 0;
  transform: scale(0.97);
  filter: blur(4px);
}
.cyber-fade-leave-to {
  opacity: 0;
  transform: scale(1.02);
}
</style>