<template>
  <!-- 科技感多重背景：粒子 + 六边形网格 + 电路纹理 + 光晕 + 扫描线 -->
  <div class="sci-fi-bg">
    <canvas ref="particleCanvas" class="particle-canvas"></canvas>
    <div class="bg-hex-grid"></div>
    <div class="circuit-lines"></div>
    <div class="bg-glow bg-glow-top"></div>
    <div class="bg-glow bg-glow-bottom"></div>
    <div class="scan-line scan-1"></div>
    <div class="scan-line scan-2"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const particleCanvas = ref(null)
let ctx, animId, particles = []
const PARTICLE_COUNT = 80
const CONNECTION_DIST = 130

class Particle {
  constructor(w, h) {
    this.x = Math.random() * w
    this.y = Math.random() * h
    this.vx = (Math.random() - 0.5) * 0.35
    this.vy = (Math.random() - 0.5) * 0.35
    this.radius = Math.random() * 1.8 + 0.4
    this.opacity = Math.random() * 0.5 + 0.1
    this.baseOpacity = this.opacity
    this.pulseSpeed = 0.005 + Math.random() * 0.015
    this.pulseOffset = Math.random() * Math.PI * 2
  }
  update(w, h) {
    this.x += this.vx
    this.y += this.vy
    if (this.x < 0 || this.x > w) this.vx *= -1
    if (this.y < 0 || this.y > h) this.vy *= -1
    this.opacity = this.baseOpacity + Math.sin(Date.now() * this.pulseSpeed + this.pulseOffset) * 0.15
  }
  draw(ctx) {
    ctx.beginPath()
    ctx.arc(this.x, this.y, this.radius, 0, Math.PI * 2)
    ctx.fillStyle = `rgba(0, 212, 255, ${this.opacity})`
    ctx.fill()
  }
}

const init = () => {
  const c = particleCanvas.value
  c.width = window.innerWidth
  c.height = window.innerHeight
  ctx = c.getContext('2d')
  particles = Array.from({ length: PARTICLE_COUNT }, () => new Particle(c.width, c.height))
  animate()
}

const animate = () => {
  const c = particleCanvas.value
  ctx.clearRect(0, 0, c.width, c.height)
  particles.forEach(p => { p.update(c.width, c.height); p.draw(ctx) })
  // 连线（仅近距离）
  for (let i = 0; i < particles.length; i++) {
    for (let j = i + 1; j < particles.length; j++) {
      const dx = particles[i].x - particles[j].x
      const dy = particles[i].y - particles[j].y
      const dist = Math.sqrt(dx * dx + dy * dy)
      if (dist < CONNECTION_DIST) {
        ctx.beginPath()
        ctx.moveTo(particles[i].x, particles[i].y)
        ctx.lineTo(particles[j].x, particles[j].y)
        ctx.strokeStyle = `rgba(0, 212, 255, ${0.07 * (1 - dist / CONNECTION_DIST)})`
        ctx.lineWidth = 0.5
        ctx.stroke()
      }
    }
  }
  animId = requestAnimationFrame(animate)
}

const onResize = () => {
  const c = particleCanvas.value
  c.width = window.innerWidth
  c.height = window.innerHeight
}

onMounted(() => { init(); window.addEventListener('resize', onResize) })
onUnmounted(() => { cancelAnimationFrame(animId); window.removeEventListener('resize', onResize) })
</script>

<style scoped>
.sci-fi-bg { position: fixed; inset: 0; pointer-events: none; z-index: 0; }
.particle-canvas { position: absolute; inset: 0; }
.bg-hex-grid {
  position: absolute; inset: 0;
  opacity: 0.025;
  background-image: url("data:image/svg+xml,%3Csvg width='60' height='52' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M30 0L60 15v22L30 52 0 37V15z' fill='none' stroke='%2300d4ff' stroke-width='0.5'/%3E%3C/svg%3E");
  background-size: 60px 52px;
}
/* 电路线条纹理 */
.circuit-lines {
  position: absolute; inset: 0; opacity: 0.02;
  background-image:
    linear-gradient(90deg, rgba(0, 212, 255, 0.1) 1px, transparent 1px),
    linear-gradient(0deg, rgba(0, 212, 255, 0.1) 1px, transparent 1px),
    radial-gradient(circle at 30% 50%, rgba(0, 212, 255, 0.15) 0%, transparent 50%),
    radial-gradient(circle at 70% 30%, rgba(168, 85, 247, 0.1) 0%, transparent 50%);
  background-size: 80px 80px, 80px 80px, 100% 100%, 100% 100%;
}
.bg-glow { position: absolute; width: 700px; height: 700px; border-radius: 50%; filter: blur(140px); }
.bg-glow-top { top: -250px; right: -150px; background: rgba(0, 180, 255, 0.025); }
.bg-glow-bottom { bottom: -250px; left: -150px; background: rgba(168, 85, 247, 0.025); }
.scan-line {
  position: absolute; left: 0; width: 100%; height: 1.5px;
  background: linear-gradient(90deg, transparent, rgba(0, 212, 255, 0.10), transparent);
}
.scan-1 { top: 0; animation: scanDown 7s linear infinite; }
.scan-2 { top: 0; animation: scanDown 7s linear 3.5s infinite; }
@keyframes scanDown { 0% { transform: translateY(-100%); } 100% { transform: translateY(100vh); } }
</style>