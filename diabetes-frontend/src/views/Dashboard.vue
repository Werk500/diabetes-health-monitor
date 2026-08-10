<template>
  <div class="dashboard">
    <!-- 标题扫描线效果 -->
    <h2 class="page-title title-scan">仪表盘</h2>

    <!-- 摘要卡片：浮动 + 交错入场 -->
    <el-row :gutter="16" class="summary-row">
      <el-col :xs="12" :sm="8" :md="6" :lg="3" v-for="(card, idx) in summaryCards" :key="card.label">
        <div :ref="el => cardRefs[idx] = el" class="stat-card card-float" :class="[`card-${card.variant}`, `card-float-delay-${idx % 4}`]">
          <div class="stat-icon">
            <el-icon :size="22"><component :is="card.icon" /></el-icon>
          </div>
          <div class="stat-body">
            <span ref="counterRefs" class="stat-value counter-value">{{ card.value }}</span>
            <span class="stat-label">{{ card.label }}</span>
          </div>
          <div class="stat-accent"></div>
          <div class="stat-bg-hex"></div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区：GSAP 滚动触发 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :lg="12">
        <div ref="bodyChartCard" class="chart-card" v-loading="loading">
          <div class="chart-header">
            <span class="chart-title title-scan">体重 / BMI 变化趋势</span>
            <span class="chart-badge">实时</span>
          </div>
          <div ref="bodyChart" class="chart-box"></div>
        </div>
      </el-col>
      <el-col :xs="24" :lg="12">
        <div ref="bsChartCard" class="chart-card" v-loading="loading">
          <div class="chart-header">
            <span class="chart-title title-scan">血糖变化趋势</span>
            <span class="chart-badge">实时</span>
          </div>
          <div ref="bsChart" class="chart-box"></div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :lg="12">
        <div ref="bpChartCard" class="chart-card" v-loading="loading">
          <div class="chart-header">
            <span class="chart-title title-scan">血压 / 心率趋势</span>
          </div>
          <div ref="bpChart" class="chart-box"></div>
        </div>
      </el-col>
      <el-col :xs="24" :lg="12">
        <div ref="exChartCard" class="chart-card" v-loading="loading">
          <div class="chart-header">
            <span class="chart-title title-scan">运动消耗趋势</span>
          </div>
          <div ref="exChart" class="chart-box"></div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :span="24">
        <div ref="dietChartCard" class="chart-card" v-loading="loading">
          <div class="chart-header">
            <span class="chart-title title-scan">今日餐饮营养分布</span>
          </div>
          <div ref="dietChart" class="chart-box chart-box-lg"></div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { ScaleToOriginal, Monitor, Watermelon, DishDot, Bicycle } from '@element-plus/icons-vue'
import { dashboardApi } from '../api'
import * as echarts from 'echarts'
import { demoDashboardData } from '../mock/demoData'
import { getData, isEmpty } from '../mock/demoManager'
import { useUserStore } from '../stores/user'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

gsap.registerPlugin(ScrollTrigger)

const userStore = useUserStore()
const userInfo = userStore.userInfo
const data = reactive({})
const loading = ref(false)
const bodyChart = ref(null); const bsChart = ref(null); const bpChart = ref(null)
const exChart = ref(null); const dietChart = ref(null)
const bodyChartCard = ref(null); const bsChartCard = ref(null)
const bpChartCard = ref(null); const exChartCard = ref(null); const dietChartCard = ref(null)
const cardRefs = ref([])

let charts = []
let scrollTriggers = []

const handleResize = () => charts.forEach(c => c.resize())

const bloodSugarColor = computed(() => {
  const v = data.latestBloodSugar
  if (!v) return '#64748b'
  if (v < 3.9) return '#e6a23c'; if (v <= 7.0) return '#10b981'
  if (v <= 10.0) return '#e6a23c'; return '#f56c6c'
})

const summaryCards = computed(() => [
  { label: '最新血糖', value: data.latestBloodSugar ? data.latestBloodSugar + ' mmol/L' : '--', icon: Watermelon, variant: 'danger' },
  { label: '今日摄入', value: data.todayCalories ? data.todayCalories.toFixed(0) + ' kcal' : '--', icon: DishDot, variant: 'warning' },
  { label: '运动消耗', value: data.todayExerciseCalories ? data.todayExerciseCalories.toFixed(0) + ' kcal' : '--', icon: Bicycle, variant: 'success' },
  { label: '最新体重', value: data.latestWeight ? data.latestWeight + ' kg' : '--', icon: ScaleToOriginal, variant: 'info' },
  { label: 'BMI', value: data.latestBmi || '--', icon: Monitor, variant: 'info' },
  { label: '体脂率', value: data.latestBodyFat ? data.latestBodyFat + '%' : '--', icon: Monitor, variant: 'default' },
  { label: '血压', value: data.latestSystolic ? data.latestSystolic + '/' + data.latestDiastolic + ' mmHg' : '--', icon: Monitor, variant: 'danger' },
  { label: '心率', value: data.latestHeartRate ? data.latestHeartRate + ' bpm' : '--', icon: Monitor, variant: 'warning' }
])

const loadData = async () => {
  loading.value = true
  try {
    const res = await dashboardApi.get(userInfo.id, {})
    const realData = (res.data.code === 200) ? res.data.data : null
    if (isEmpty(realData) || !realData.bodyTrend || (!realData.bodyTrend.dates || realData.bodyTrend.dates.length === 0)) {
      Object.assign(data, demoDashboardData)
    } else { Object.assign(data, realData) }
  } catch { Object.assign(data, demoDashboardData) }
  finally {
    loading.value = false
    await nextTick()
    renderCharts()
    await nextTick()
    initAnimations()
  }
}

const sciFiTheme = () => ({
  textStyle: { color: '#94a3b8' },
  legend: { textStyle: { color: '#94a3b8' } }
})

const renderCharts = () => {
  charts.forEach(c => c.dispose()); charts = []

  if (bodyChart.value && data.bodyTrend) {
    const c = echarts.init(bodyChart.value); charts.push(c)
    c.setOption({
      ...sciFiTheme(),
      tooltip: { trigger: 'axis' },
      legend: { data: ['体重(kg)', 'BMI'], bottom: 0, textStyle: { color: '#94a3b8' } },
      grid: { left: 60, right: 25, top: 25, bottom: 55 },
      xAxis: { type: 'category', data: data.bodyTrend.dates || [], axisLabel: { color: '#94a3b8', rotate: 30 }, axisLine: { lineStyle: { color: '#334155' } } },
      yAxis: [{ type: 'value', name: 'kg', nameTextStyle: { color: '#94a3b8' }, axisLabel: { color: '#94a3b8' }, splitLine: { lineStyle: { color: 'rgba(51,65,85,0.12)' } } }, { type: 'value', name: 'BMI' }],
      series: [
        { name: '体重(kg)', type: 'line', data: data.bodyTrend.weightValues || [], smooth: true, lineStyle: { color: '#00d4ff', width: 2.5, shadowBlur: 10, shadowColor: 'rgba(0,212,255,0.35)' }, itemStyle: { color: '#00d4ff' }, areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(0,212,255,0.18)' }, { offset: 1, color: 'rgba(0,212,255,0)' }] } } },
        { name: 'BMI', type: 'line', yAxisIndex: 1, data: data.bodyTrend.bmiValues || [], smooth: true, lineStyle: { color: '#a855f7', width: 2.5, shadowBlur: 10, shadowColor: 'rgba(168,85,247,0.35)' }, itemStyle: { color: '#a855f7' } }
      ]
    })
  }

  if (bsChart.value && data.bloodSugarTrend) {
    const c = echarts.init(bsChart.value); charts.push(c)
    c.setOption({
      ...sciFiTheme(), tooltip: { trigger: 'axis' },
      legend: { data: ['空腹', '餐前', '餐后', '睡前'], bottom: 0, textStyle: { color: '#94a3b8' } },
      grid: { left: 60, right: 25, top: 25, bottom: 55 },
      xAxis: { type: 'category', data: data.bloodSugarTrend.dates || [], axisLabel: { color: '#94a3b8', rotate: 30 }, axisLine: { lineStyle: { color: '#334155' } } },
      yAxis: { type: 'value', name: 'mmol/L', nameTextStyle: { color: '#94a3b8' }, axisLabel: { color: '#94a3b8' }, splitLine: { lineStyle: { color: 'rgba(51,65,85,0.12)' } } },
      series: [
        { name: '空腹', type: 'line', data: data.bloodSugarTrend.fastingValues || [], smooth: true, lineStyle: { color: '#00d4ff', width: 2 }, itemStyle: { color: '#00d4ff' } },
        { name: '餐前', type: 'line', data: data.bloodSugarTrend.beforeMealValues || [], smooth: true, lineStyle: { color: '#10b981', width: 2 }, itemStyle: { color: '#10b981' } },
        { name: '餐后', type: 'line', data: data.bloodSugarTrend.afterMealValues || [], smooth: true, lineStyle: { color: '#e6a23c', width: 2 }, itemStyle: { color: '#e6a23c' } },
        { name: '睡前', type: 'line', data: data.bloodSugarTrend.bedtimeValues || [], smooth: true, lineStyle: { color: '#f56c6c', width: 2 }, itemStyle: { color: '#f56c6c' } }
      ]
    })
  }

  if (bpChart.value && data.bodyTrend) {
    const c = echarts.init(bpChart.value); charts.push(c)
    c.setOption({
      ...sciFiTheme(), tooltip: { trigger: 'axis' },
      legend: { data: ['收缩压', '舒张压', '心率'], bottom: 0, textStyle: { color: '#94a3b8' } },
      grid: { left: 60, right: 25, top: 25, bottom: 55 },
      xAxis: { type: 'category', data: data.bodyTrend.dates || [], axisLabel: { color: '#94a3b8', rotate: 30 }, axisLine: { lineStyle: { color: '#334155' } } },
      yAxis: [{ type: 'value', name: 'mmHg', nameTextStyle: { color: '#94a3b8' }, axisLabel: { color: '#94a3b8' }, splitLine: { lineStyle: { color: 'rgba(51,65,85,0.12)' } } }, { type: 'value', name: 'bpm' }],
      series: [
        { name: '收缩压', type: 'line', data: data.bodyTrend.systolicValues || [], smooth: true, lineStyle: { color: '#f56c6c', width: 2.5, shadowBlur: 10, shadowColor: 'rgba(245,108,108,0.3)' }, itemStyle: { color: '#f56c6c' } },
        { name: '舒张压', type: 'line', data: data.bodyTrend.diastolicValues || [], smooth: true, lineStyle: { color: '#e6a23c', width: 2.5 }, itemStyle: { color: '#e6a23c' } },
        { name: '心率', type: 'line', yAxisIndex: 1, data: data.bodyTrend.heartRateValues || [], smooth: true, lineStyle: { color: '#cbd5e1', width: 2 }, itemStyle: { color: '#cbd5e1' } }
      ]
    })
  }

  if (exChart.value && data.exerciseStats) {
    const c = echarts.init(exChart.value); charts.push(c)
    c.setOption({
      ...sciFiTheme(), tooltip: { trigger: 'axis' },
      legend: { data: ['消耗热量', '运动时长'], bottom: 0, textStyle: { color: '#94a3b8' } },
      grid: { left: 60, right: 25, top: 25, bottom: 55 },
      xAxis: { type: 'category', data: data.exerciseStats.dates || [], axisLabel: { color: '#94a3b8', rotate: 30 }, axisLine: { lineStyle: { color: '#334155' } } },
      yAxis: [{ type: 'value', name: 'kcal', nameTextStyle: { color: '#94a3b8' }, axisLabel: { color: '#94a3b8' }, splitLine: { lineStyle: { color: 'rgba(51,65,85,0.12)' } } }, { type: 'value', name: 'min' }],
      series: [
        { name: '消耗热量', type: 'bar', data: data.exerciseStats.calorieBurnedValues || [], itemStyle: { color: '#00d4ff', borderRadius: [4, 4, 0, 0], opacity: 0.8 } },
        { name: '运动时长', type: 'line', yAxisIndex: 1, data: data.exerciseStats.durationValues || [], smooth: true, lineStyle: { color: '#10b981', width: 2.5, shadowBlur: 10, shadowColor: 'rgba(16,185,129,0.3)' }, itemStyle: { color: '#10b981' } }
      ]
    })
  }

  if (dietChart.value && data.dietStats) {
    const c = echarts.init(dietChart.value); charts.push(c)
    c.setOption({
      ...sciFiTheme(), tooltip: { trigger: 'item', formatter: '{b}: {c} kcal ({d}%)' },
      legend: { orient: 'vertical', left: 'left', top: 'center', textStyle: { color: '#94a3b8' } },
      series: [{
        name: '热量分布', type: 'pie', radius: ['55%', '80%'], center: ['55%', '50%'],
        data: (data.dietStats.mealNames || []).map((name, i) => ({ name, value: data.dietStats.calorieValues?.[i] || 0 })),
        label: { color: '#cbd5e1', formatter: '{b}\n{c} kcal' },
        itemStyle: { borderColor: 'rgba(13,17,23,0.8)', borderWidth: 3, borderRadius: 4 },
        emphasis: { itemStyle: { shadowBlur: 25, shadowColor: 'rgba(0,212,255,0.35)' } }
      }]
    })
  }
}

// GSAP 动画
const initAnimations = () => {
  // 摘要卡片交错入场 + 浮动
  const cards = cardRefs.value.filter(Boolean)
  if (cards.length) {
    gsap.fromTo(cards, { opacity: 0, y: 40, scale: 0.9 }, {
      opacity: 1, y: 0, scale: 1, duration: 0.7, stagger: 0.06, ease: 'power3.out'
    })
  }

  // 图表卡片滚动触发
  const chartCards = [bodyChartCard, bsChartCard, bpChartCard, exChartCard, dietChartCard]
  chartCards.forEach((ref) => {
    if (!ref.value) return
    const st = ScrollTrigger.create({
      trigger: ref.value,
      start: 'top 85%',
      onEnter: () => gsap.fromTo(ref.value, { opacity: 0, y: 50 }, { opacity: 1, y: 0, duration: 0.8, ease: 'power2.out' })
    })
    scrollTriggers.push(st)
  })
}

onMounted(() => { window.addEventListener('resize', handleResize); loadData() })
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  charts.forEach(c => c.dispose()); charts = []
  scrollTriggers.forEach(st => st.kill()); scrollTriggers = []
})
</script>

<style scoped>
.dashboard { max-width: 1400px; }
.page-title {
  font-family: var(--font-mono);
  font-size: 24px; font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 20px;
  letter-spacing: 4px;
}

/* 统计卡片 */
.summary-row { margin-bottom: 16px; }
.stat-card {
  position: relative;
  padding: 20px 16px; margin-bottom: 14px;
  background: linear-gradient(135deg, rgba(17, 24, 39, 0.7), rgba(13, 17, 23, 0.8));
  backdrop-filter: blur(14px);
  border: 1px solid rgba(51, 65, 85, 0.35);
  border-radius: 14px;
  display: flex; align-items: center; gap: 12px;
  cursor: default;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}
.stat-card:hover {
  border-color: rgba(0, 212, 255, 0.35);
  transform: translateY(-3px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.4), 0 0 20px rgba(0, 212, 255, 0.06);
}
.stat-card::before {
  content: ''; position: absolute; top: 0; left: 0; width: 3px; height: 100%;
  border-radius: 3px 0 0 3px;
}
.card-danger::before { background: linear-gradient(180deg, #f56c6c, #e6a23c); }
.card-warning::before { background: linear-gradient(180deg, #e6a23c, #f56c6c); }
.card-success::before { background: linear-gradient(180deg, #10b981, #34d399); }
.card-info::before { background: linear-gradient(180deg, #00d4ff, #a855f7); }
.card-default::before { background: linear-gradient(180deg, #64748b, #94a3b8); }

.stat-icon { flex-shrink: 0; opacity: 0.85; }
.card-danger .stat-icon { color: #f56c6c; }
.card-warning .stat-icon { color: #e6a23c; }
.card-success .stat-icon { color: #10b981; }
.card-info .stat-icon { color: #00d4ff; }
.card-default .stat-icon { color: #94a3b8; }

.stat-body { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.stat-value {
  font-family: var(--font-mono); font-size: 18px; font-weight: 700;
  color: var(--text-primary);
}
.stat-label { font-size: 12px; color: var(--text-secondary); letter-spacing: 0.5px; }
.stat-accent { position: absolute; top: 0; right: 0; bottom: 0; width: 50px; background: radial-gradient(circle at right, rgba(0, 212, 255, 0.04), transparent); }
.stat-bg-hex {
  position: absolute; right: -20px; bottom: -20px; width: 80px; height: 80px;
  background: url("data:image/svg+xml,%3Csvg width='60' height='52' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M30 0L60 15v22L30 52 0 37V15z' fill='none' stroke='%2300d4ff' stroke-width='0.5'/%3E%3C/svg%3E");
  background-size: 40px 35px; opacity: 0.08; pointer-events: none;
}

/* 图表卡片 */
.chart-row { margin-bottom: 16px; }
.chart-row { margin-bottom: 4px; }
.chart-card {
  background: rgba(17, 24, 39, 0.5);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(51, 65, 85, 0.3);
  border-radius: 16px; padding: 22px;
  transition: border-color 0.3s;
}
.chart-card:hover { border-color: rgba(0, 212, 255, 0.25); }
.chart-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 14px; padding-bottom: 12px;
  border-bottom: 1px solid rgba(51, 65, 85, 0.2);
}
.chart-title { font-size: 15px; font-weight: 600; color: var(--text-primary); }
.chart-badge {
  font-family: var(--font-mono); font-size: 10px;
  padding: 2px 10px; border-radius: 10px;
  border: 1px solid var(--neon-blue); color: var(--neon-blue);
}
.chart-box { width: 100%; height: 380px; }
.chart-box-lg { height: 380px; }
</style>