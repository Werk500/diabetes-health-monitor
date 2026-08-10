<template>
  <div class="login-page">

    <!-- ==================== 糖尿病血糖曲线背景 Canvas ==================== -->
    <canvas ref="bgCanvas" class="bg-canvas"></canvas>

    <!-- ==================== 登录覆盖层 ==================== -->
    <div class="login-overlay" @mousemove="onCardTilt" @mouseleave="resetCardTilt">
      <div ref="loginCard" class="cyber-card">
        <!-- 流动光边 -->
        <div class="card-border"></div>
        <!-- 卡片扫描线 -->
        <div class="card-scan"></div>

        <div class="card-content">
          <!-- 标题 -->
          <div ref="titleSec" class="title-section">
            <h2 class="card-title">糖尿病健康监测系统</h2>
            <p class="card-subtitle">智能血糖管理 · 守护健康每一天</p>
          </div>

          <!-- 登录方式切换 -->
          <div ref="tabSec" class="tab-section">
            <button :class="['tab-btn', { active: loginMode === 'password' }]" @click="switchMode('password')">密码登录</button>
            <button :class="['tab-btn', { active: loginMode === 'sms' }]" @click="switchMode('sms')">短信验证</button>
            <div class="tab-underline" :class="{ right: loginMode === 'sms' }"></div>
          </div>

          <!-- 表单区域 -->
          <div ref="formSec">
            <el-form :model="form" :rules="rules" ref="formRef" size="large" class="login-form" @keyup.enter="handleLogin">
              <!-- 用户名 -->
              <el-form-item prop="username">
                <el-input v-model="form.username" :placeholder="loginMode === 'sms' ? '手机号码' : '用户名 / 手机号'" :prefix-icon="User" class="cyber-input" />
              </el-form-item>

              <!-- 密码 -->
              <el-form-item v-if="loginMode === 'password'" prop="password">
                <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password class="cyber-input" />
              </el-form-item>

              <!-- 短信验证码 -->
              <el-form-item v-if="loginMode === 'sms'" prop="smsCode">
                <el-input v-model="form.smsCode" placeholder="验证码" :prefix-icon="Message" class="cyber-input">
                  <template #append>
                    <el-button :disabled="smsCountdown > 0" @click="sendSms" class="sms-btn">
                      {{ smsCountdown > 0 ? smsCountdown + 's' : '获取验证码' }}
                    </el-button>
                  </template>
                </el-input>
              </el-form-item>

              <!-- 登录按钮 -->
              <el-form-item ref="btnSec">
                <el-button type="primary" @click="handleLogin" :loading="loading" class="cyber-btn" size="large">
                  <span class="btn-text">登 录</span>
                </el-button>
              </el-form-item>
            </el-form>
          </div>

          <!-- 辅助链接 -->
          <div ref="linkSec" class="link-section">
            <router-link to="/register" class="aux-link">注册账号</router-link>
            <span class="link-divider">|</span>
            <a href="#" class="aux-link" @click.prevent="forgotPwd">忘记密码</a>
          </div>
        </div>
      </div>

      <!-- 版权信息 -->
      <div ref="copyright" class="copyright">
        Copyright &copy; 2026 糖尿病健康监测 · 智能血糖管理
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock, Message } from '@element-plus/icons-vue'
import { userApi } from '../api'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import JSEncrypt from 'jsencrypt'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const loginMode = ref('password')
const smsCountdown = ref(0)
let smsTimer = null

const form = reactive({ username: '', password: '', smsCode: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名/手机号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  smsCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}
// ==================== RSA 加密 ====================
const publicKey = ref('')
const encrypt = (text) => {
  if (!text || !publicKey.value) return text
  const jse = new JSEncrypt()
  jse.setPublicKey(publicKey.value)
  return jse.encrypt(text) || text
}

// DOM refs for GSAP
const loginCard = ref(null)
const titleSec = ref(null)
const tabSec = ref(null)
const formSec = ref(null)
const btnSec = ref(null)
const linkSec = ref(null)
const copyright = ref(null)
const bgCanvas = ref(null)

// ==================== GSAP 入场动画 ====================
let cardFloatTween = null
let bgAnimId = null

onMounted(async () => {
  await nextTick()
  // 获取 RSA 公钥
  try {
    const res = await fetch('/api/user/public-key')
    const data = await res.json()
    if (data.code === 200) publicKey.value = data.data
  } catch (e) { console.error('获取公钥失败', e) }
  animateBgCanvas()
  initGsapAnimations()
  initCardFloat()
})

onUnmounted(() => {
  if (cardFloatTween) cardFloatTween.kill()
  if (smsTimer) clearInterval(smsTimer)
  if (bgAnimId) cancelAnimationFrame(bgAnimId)
})

function initGsapAnimations() {
  const gsap = window.gsap
  if (!gsap || !loginCard.value) return
  const tl = gsap.timeline({ defaults: { ease: 'power3.out' } })
  tl.fromTo(loginCard.value, { y: 60, opacity: 0, scale: 0.94 }, { y: 0, opacity: 1, scale: 1, duration: 1.2 }, '+=0.3')
  tl.fromTo(titleSec.value, { y: 20, opacity: 0 }, { y: 0, opacity: 1, duration: 0.6 }, '-=0.6')
  tl.fromTo(tabSec.value, { y: 16, opacity: 0 }, { y: 0, opacity: 1, duration: 0.5 }, '-=0.3')
  tl.fromTo(formSec.value, { y: 16, opacity: 0 }, { y: 0, opacity: 1, duration: 0.5 }, '-=0.3')
  tl.fromTo(btnSec.value, { y: 12, opacity: 0, scale: 0.95 }, { y: 0, opacity: 1, scale: 1, duration: 0.5 }, '-=0.2')
  tl.fromTo(linkSec.value, { y: 8, opacity: 0 }, { y: 0, opacity: 1, duration: 0.5 }, '-=0.3')
  tl.fromTo(copyright.value, { opacity: 0 }, { opacity: 1, duration: 0.5 }, '-=0.2')
}

function initCardFloat() {
  const gsap = window.gsap
  if (!gsap || !loginCard.value) return
  cardFloatTween = gsap.to(loginCard.value, { y: -8, duration: 2.8, repeat: -1, yoyo: true, ease: 'sine.inOut' })
}


// ==================== 卡片倾斜跟手效果 ====================
function onCardTilt(e) {
  if (!loginCard.value) return
  const rect = loginCard.value.getBoundingClientRect()
  const centerX = rect.left + rect.width / 2
  const centerY = rect.top + rect.height / 2
  const rotateX = ((e.clientY - centerY) / (rect.height / 2)) * -3
  const rotateY = ((e.clientX - centerX) / (rect.width / 2)) * 3
  loginCard.value.style.transition = 'transform 0.15s ease-out'
  loginCard.value.style.transform = `perspective(1000px) rotateX(${rotateX}deg) rotateY(${rotateY}deg)`
}
function resetCardTilt() {
  if (!loginCard.value) return
  loginCard.value.style.transition = 'transform 0.6s ease-out'
  loginCard.value.style.transform = 'perspective(1000px) rotateX(0deg) rotateY(0deg)'
}

// ==================== 切换登录方式 ====================
function switchMode(mode) { loginMode.value = mode }

// ==================== 登录 ====================
const handleLogin = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const isSms = loginMode.value === 'sms'
      const res = isSms
        ? await userApi.smsLogin({ phone: form.username, code: encrypt(form.smsCode) })
        : await userApi.login({ username: form.username, password: encrypt(form.password) })
      if (res.data.code === 200) {
        const data = res.data.data
        userStore.setToken(data.token)
        userStore.setUserInfo(data.user)
        ElMessage.success('登录成功，欢迎回来！')
        router.push(data.user.role === 1 ? '/admin/dashboard' : '/user/dashboard')
      } else {
        ElMessage.error(res.data.msg || '登录失败')
      }
    } catch (e) {
      ElMessage.error('登录失败，请重试')
    } finally {
      loading.value = false
    }
  })
}

// ==================== 短信验证码 ====================
async function sendSms() {
  if (smsCountdown.value > 0 || !form.username) return
  try {
    await userApi.sendSms(form.username)
    ElMessage.success('验证码已发送')
    smsCountdown.value = 60
    smsTimer = setInterval(() => {
      smsCountdown.value--
      if (smsCountdown.value <= 0) { clearInterval(smsTimer); smsTimer = null }
    }, 1000)
  } catch (e) {
    ElMessage.error('验证码发送失败')
  }
}

function forgotPwd() { ElMessage.info('请联系管理员重置密码') }


// ==================== 糖尿病 Canvas 背景 ====================

// 配色常量
var bgColors = { cyan: '0,212,255', green: '16,185,129', purple: '168,85,247', muted: '100,116,139' };

function initBgCanvas() {
  var c = bgCanvas.value; if (!c) return;
  c.width = window.innerWidth; c.height = window.innerHeight;
  window.addEventListener('resize', function() { c.width = window.innerWidth; c.height = window.innerHeight; });
}

function animateBgCanvas() {
  var canvas = bgCanvas.value;
  if (!canvas) return;
  var ctx = canvas.getContext('2d');
  var time = 0;

  function draw() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    time += 0.008;
    var W = canvas.width, H = canvas.height;

    // 1. 背景渐变
    var bgGrad = ctx.createRadialGradient(W*0.5, H*0.35, 0, W*0.5, H*0.5, Math.max(W,H)*0.7);
    bgGrad.addColorStop(0, 'rgba(10,22,40,0.5)');
    bgGrad.addColorStop(1, 'rgba(6,13,26,0.95)');
    ctx.fillStyle = bgGrad;
    ctx.fillRect(0, 0, W, H);

    // 2. 医疗科技网格
    ctx.strokeStyle = 'rgba(0,212,255,0.03)';
    ctx.lineWidth = 0.5;
    var gs = 50;
    ctx.beginPath();
    for (var x = gs; x < W; x += gs) { ctx.moveTo(x, 0); ctx.lineTo(x, H); }
    for (var y = gs; y < H; y += gs) { ctx.moveTo(0, y); ctx.lineTo(W, y); }
    ctx.stroke();
    ctx.strokeStyle = 'rgba(0,212,255,0.05)'; ctx.lineWidth = 1;
    ctx.beginPath();
    for (var x = 200; x < W; x += 200) { ctx.moveTo(x, 0); ctx.lineTo(x, H); }
    for (var y = 200; y < H; y += 200) { ctx.moveTo(0, y); ctx.lineTo(W, y); }
    ctx.stroke();

    // 3. 六边形分子纹理
    var hr = 28, hh = hr * Math.sqrt(3);
    var hcols = Math.ceil(W / (hr * 1.5)) + 1, hrows = Math.ceil(H / hh) + 1;
    for (var row = 0; row < hrows; row++) {
      for (var col = 0; col < hcols; col++) {
        var cx = col * hr * 1.5 + (row % 2) * hr * 0.75;
        var cy = row * hh * 0.5;
        var ha = 0.015 + Math.sin(cx*0.02 + cy*0.02 + time*0.3) * 0.01;
        ctx.beginPath();
        for (var j = 0; j < 6; j++) {
          var a = Math.PI/3*j - Math.PI/6;
          var hx = cx + hr*Math.cos(a), hy = cy + hr*Math.sin(a);
          j === 0 ? ctx.moveTo(hx, hy) : ctx.lineTo(hx, hy);
        }
        ctx.closePath();
        ctx.strokeStyle = 'rgba(0,212,255,' + ha + ')';
        ctx.lineWidth = 0.5;
        ctx.stroke();
      }
    }

    // 4. 血糖波动曲线
    var curves = [
      { by: H*0.32, amp: 30, col: bgColors.cyan,  spd: 1.8, al: 0.18 },
      { by: H*0.42, amp: 22, col: bgColors.green, spd: 1.5, al: 0.14 },
      { by: H*0.52, amp: 18, col: bgColors.purple,spd: 1.2, al: 0.10 }
    ];
    curves.forEach(function(c, i) {
      ctx.beginPath();
      ctx.moveTo(0, c.by + Math.sin(time*c.spd + i)*c.amp);
      for (var x = 0; x <= W; x += 3) {
        var y = c.by + Math.sin(x*0.003 + time*c.spd + i)*c.amp
               + Math.sin(x*0.008 + time*1.1 + i*2)*c.amp*0.5
               + Math.sin(x*0.015 + time*0.7 + i)*c.amp*0.25;
        ctx.lineTo(x, y);
      }
      ctx.lineTo(W, H); ctx.lineTo(0, H); ctx.closePath();
      var fg = ctx.createLinearGradient(0, c.by-c.amp, 0, c.by+c.amp*2);
      fg.addColorStop(0, 'rgba('+c.col+',0.04)');
      fg.addColorStop(1, 'rgba('+c.col+',0)');
      ctx.fillStyle = fg; ctx.fill();
      ctx.beginPath(); ctx.strokeStyle = 'rgba('+c.col+','+c.al+')'; ctx.lineWidth = 2.5;
      for (var x = 0; x <= W; x += 3) {
        var y = c.by + Math.sin(x*0.003 + time*c.spd + i)*c.amp
               + Math.sin(x*0.008 + time*1.1 + i*2)*c.amp*0.5
               + Math.sin(x*0.015 + time*0.7 + i)*c.amp*0.25;
        x===0 ? ctx.moveTo(x,y) : ctx.lineTo(x,y);
      }
      ctx.stroke();
      ctx.beginPath(); ctx.strokeStyle = 'rgba('+c.col+',0.06)'; ctx.lineWidth = 6;
      for (var x = 0; x <= W; x += 3) {
        var y = c.by + Math.sin(x*0.003 + time*c.spd + i)*c.amp
               + Math.sin(x*0.008 + time*1.1 + i*2)*c.amp*0.5
               + Math.sin(x*0.015 + time*0.7 + i)*c.amp*0.25;
        x===0 ? ctx.moveTo(x,y) : ctx.lineTo(x,y);
      }
      ctx.stroke();
      for (var p = 0; p < 8; p++) {
        var px = ((time*20 + p*W/8) % W + W) % W;
        var py = c.by + Math.sin(px*0.003 + time*c.spd + i)*c.amp
                     + Math.sin(px*0.008 + time*1.1 + i*2)*c.amp*0.5;
        var pulse = 0.5 + Math.sin(time*3.5 + p)*0.5;
        ctx.beginPath();
        ctx.arc(px, py, 2+pulse*3, 0, Math.PI*2);
        ctx.fillStyle = 'rgba('+c.col+','+(0.3+pulse*0.3)+')';
        ctx.fill();
        ctx.beginPath();
        ctx.arc(px, py, 5+pulse*6, 0, Math.PI*2);
        ctx.strokeStyle = 'rgba('+c.col+','+(0.08+pulse*0.06)+')';
        ctx.lineWidth = 1; ctx.stroke();
      }
    });

    // 5. 浮动医学数据粒子
    var texts = [
      { t: '血糖 5.6 mmol/L', x:0.10, y:0.12, s:0.25 },
      { t: 'HbA1c 6.2%',       x:0.80, y:0.18, s:0.35 },
      { t: '空腹血糖 4.8',     x:0.15, y:0.65, s:0.30 },
      { t: '餐后2h 7.2',      x:0.75, y:0.72, s:0.32 },
      { t: 'BMI 23.5',         x:0.45, y:0.85, s:0.28 },
      { t: '胰岛素 8.2 mU/L',  x:0.85, y:0.55, s:0.30 },
      { t: 'C肽 1.8 ng/mL',   x:0.08, y:0.45, s:0.33 },
      { t: '糖化 6.8%',        x:0.60, y:0.08, s:0.27 }
    ];
    texts.forEach(function(ft) {
      var x = (ft.x + Math.sin(time*ft.s)*0.03) * W;
      var y = (ft.y + Math.cos(time*ft.s*0.7)*0.02) * H;
      var alpha = 0.07 + Math.sin(time*1.2 + ft.s*8)*0.04;
      ctx.fillStyle = 'rgba(100,116,139,'+alpha+')';
      ctx.font = '12px "JetBrains Mono","Courier New",monospace';
      ctx.fillText(ft.t, x, y);
    });

    // 6. 边缘渐变遮罩
    var tg = ctx.createLinearGradient(0, 0, 0, H*0.08);
    tg.addColorStop(0, 'rgba(6,13,26,0.6)');
    tg.addColorStop(1, 'rgba(6,13,26,0)');
    ctx.fillStyle = tg; ctx.fillRect(0, 0, W, H*0.08);

    var bg = ctx.createLinearGradient(0, H*0.92, 0, H);
    bg.addColorStop(0, 'rgba(6,13,26,0)');
    bg.addColorStop(1, 'rgba(6,13,26,0.6)');
    ctx.fillStyle = bg; ctx.fillRect(0, H*0.92, W, H*0.08);

    bgAnimId = requestAnimationFrame(draw);
  }
  draw();
}
</script>

<style scoped>
.login-page { position: relative; width: 100%; min-height: 100vh; overflow: hidden; }
.bg-canvas { position: fixed; inset: 0; z-index: 0; pointer-events: none; }
.login-overlay {
  position: fixed; inset: 0; z-index: 10;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
}

/* ==================== 卡片 ==================== */
.cyber-card {
  width: 420px; position: relative;
  background: rgba(12, 20, 35, 0.72);
  backdrop-filter: blur(36px); -webkit-backdrop-filter: blur(36px);
  border-radius: 20px; overflow: hidden;
}
.card-border {
  position: absolute; inset: 0; border-radius: 20px; padding: 1.5px;
  background: conic-gradient(from var(--a,0deg),transparent,#00d4ff,#a855f7,#00d4ff,transparent);
  -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor; mask-composite: exclude;
  animation: border-spin 4s linear infinite; pointer-events: none;
}
@keyframes border-spin { to { --a: 360deg; } }
@property --a { syntax: '<angle>'; initial-value: 0deg; inherits: false; }
.card-scan {
  position: absolute; left: 10%; width: 80%; height: 2px;
  background: linear-gradient(90deg,transparent,rgba(0,212,255,0.5),transparent);
  animation: scan 4s ease-in-out infinite; pointer-events: none; z-index: 2;
}
@keyframes scan {
  0%{top:-2px;opacity:0} 25%{opacity:1} 60%{opacity:0} 100%{top:102%;opacity:0}
}
.card-content { position: relative; z-index: 1; padding: 44px 40px 36px; }

/* ==================== 标题 ==================== */
.title-section { text-align: center; margin-bottom: 26px; }
.card-title {
  font-size: 20px; font-weight: 700; letter-spacing: 4px;
  background: linear-gradient(90deg,#00d4ff,#22d3ee 40%,#a855f7 100%);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
  background-clip: text;
  filter: drop-shadow(0 0 6px rgba(0,212,255,0.35));
}
.card-subtitle {
  font-size: 12px; color: rgba(148,163,184,0.5);
  margin-top: 8px; letter-spacing: 4px;
}

/* ==================== 切换标签 ==================== */
.tab-section {
  position: relative; display: flex; margin-bottom: 26px;
  background: rgba(15,23,42,0.55);
  border: 1px solid rgba(51,65,85,0.35); border-radius: 10px; padding: 4px;
}
.tab-btn {
  flex: 1; padding: 10px 0; border: none; border-radius: 8px;
  background: transparent; color: rgba(148,163,184,0.5);
  font-size: 14px; cursor: pointer; transition: color .3s;
  position: relative; z-index: 1; font-family: inherit;
}
.tab-btn.active { color: #e2e8f0; }
.tab-btn:hover { color: rgba(0,212,255,0.7); }
.tab-underline {
  position: absolute; bottom: 4px; left: 4px;
  width: calc(50% - 4px); height: 2px;
  background: linear-gradient(90deg,#00d4ff,#a855f7); border-radius: 1px;
  transition: transform .35s cubic-bezier(.4,0,.2,1); z-index: 0;
}
.tab-underline.right { transform: translateX(100%); }

/* ==================== 表单 ==================== */
.login-form { margin-top: 4px; }
.cyber-input :deep(.el-input__wrapper) {
  background: rgba(255,255,255,0.04) !important;
  border: 1px solid rgba(255,255,255,0.1) !important;
  border-radius: 10px !important; box-shadow: none !important;
  transition: all .3s !important;
}
.cyber-input :deep(.el-input__wrapper:hover) {
  border-color: rgba(0,212,255,0.3) !important;
  transform: translateY(-1px);
}
.cyber-input :deep(.el-input__wrapper.is-focus) {
  border-color: #00d4ff !important;
  box-shadow: 0 0 20px rgba(0,212,255,0.2) !important;
}
.cyber-input :deep(.el-input__inner) {
  color: #e2e8f0 !important; font-size: 15px !important;
}
.cyber-input :deep(.el-input__inner::placeholder) { color: rgba(148,163,184,0.35) !important; }
.cyber-input :deep(.el-input-group__append) {
  background: transparent !important; border: none !important; padding: 0 !important;
}

.cyber-input :deep(.el-input-group) { border: none !important; box-shadow: none !important; }
.cyber-input :deep(.el-input-group__prepend),
.cyber-input :deep(.el-input-group__append) {
  background: transparent !important; border: none !important; box-shadow: none !important;
}
.cyber-input :deep(.el-input-group__append .el-button),
.cyber-input :deep(.el-input-group__append .el-button:hover),
.cyber-input :deep(.el-input-group__append .el-button:focus) {
  background: transparent !important; border: none !important; box-shadow: none !important;
  outline: none !important;
}

/* ==================== 短信按钮 ==================== */
.sms-btn {
  border-radius: 0 10px 10px 0 !important; height: 100% !important;
  background: transparent !important; border: none !important;
  color: #00d4ff !important; font-size: 12px !important;
}
.sms-btn.is-disabled { color: #475569 !important; }

/* ==================== 登录按钮 ==================== */
.cyber-btn {
  width: 100% !important; height: 50px !important;
  font-size: 17px !important; font-weight: 700 !important;
  letter-spacing: 12px !important; border-radius: 12px !important;
  background: linear-gradient(135deg,#00d4ff,#06b6d4) !important;
  border: none !important;
  box-shadow: 0 4px 20px rgba(0,212,255,0.3) !important;
  transition: all .3s !important; position: relative; overflow: hidden;
}
.cyber-btn::before {
  content: ''; position: absolute; inset: 0;
  background: linear-gradient(135deg,transparent 30%,rgba(255,255,255,0.15) 50%,transparent 70%);
  transform: translateX(-100%); transition: transform .6s;
}
.cyber-btn:hover::before { transform: translateX(100%); }
.cyber-btn:hover {
  box-shadow: 0 4px 35px rgba(0,212,255,0.5) !important;
  transform: translateY(-2px) !important;
}
.cyber-btn:active { transform: scale(.97) !important; }

/* ==================== 辅助链接 ==================== */
.link-section {
  display: flex; justify-content: center; align-items: center;
  gap: 14px; margin-top: 22px;
}
.aux-link {
  color: rgba(148,163,184,0.45); font-size: 13px;
  text-decoration: none; transition: color .3s; position: relative; padding: 2px 0;
}
.aux-link::after {
  content: ''; position: absolute; bottom: -2px; left: 50%; width: 0; height: 1px;
  background: #00d4ff; border-radius: 1px;
  transition: all .35s cubic-bezier(.4,0,.2,1);
}
.aux-link:hover { color: #00d4ff; text-shadow: 0 0 8px rgba(0,212,255,0.4); }
.aux-link:hover::after { left: 0; width: 100%; }
.link-divider { color: rgba(51,65,85,0.4); font-size: 13px; }

/* ==================== 版权信息 ==================== */
.copyright {
  margin-top: 40px; text-align: center;
  font-size: 12px; color: rgba(100,116,139,0.3); letter-spacing: 2px;
}

@media (max-width: 460px) {
  .cyber-card { width: 92vw; }
  .card-content { padding: 36px 22px 28px; }
  .card-title { font-size: 17px; letter-spacing: 2px; }
}
</style>
