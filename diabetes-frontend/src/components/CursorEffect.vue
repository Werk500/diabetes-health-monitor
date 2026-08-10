<template>
  <!-- 简洁霓虹光标：小圆点 + 跟随光圈（无 mix-blend-mode，不刺眼） -->
  <div ref="dot" class="cursor-dot"></div>
  <div ref="ring" class="cursor-ring"></div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const dot = ref(null)
const ring = ref(null)
let mouseX = 0, mouseY = 0
let dotX = 0, dotY = 0
let ringX = 0, ringY = 0

const onMove = (e) => { mouseX = e.clientX; mouseY = e.clientY }

const animate = () => {
  dotX += (mouseX - dotX) * 0.25
  dotY += (mouseY - dotY) * 0.25
  ringX += (mouseX - ringX) * 0.08
  ringY += (mouseY - ringY) * 0.08

  if (dot.value) { dot.value.style.transform = `translate(${dotX - 3}px, ${dotY - 3}px)` }
  if (ring.value) { ring.value.style.transform = `translate(${ringX - 14}px, ${ringY - 14}px)` }

  requestAnimationFrame(animate)
}

onMounted(() => {
  document.addEventListener('mousemove', onMove, { passive: true })
  animate()
})
onUnmounted(() => document.removeEventListener('mousemove', onMove))
</script>

<style scoped>
.cursor-dot {
  width: 6px; height: 6px;
  background: var(--neon-blue, #00d4ff);
  border-radius: 50%;
  position: fixed; pointer-events: none; z-index: 99999;
  box-shadow: 0 0 8px rgba(0, 212, 255, 0.6);
  will-change: transform;
}
.cursor-ring {
  width: 28px; height: 28px;
  border: 1.5px solid rgba(0, 212, 255, 0.3);
  border-radius: 50%;
  position: fixed; pointer-events: none; z-index: 99998;
  will-change: transform;
  transition: border-color 0.3s, width 0.3s, height 0.3s;
}
</style>