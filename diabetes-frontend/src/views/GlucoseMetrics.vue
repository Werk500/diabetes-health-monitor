<template>
  <div class="metrics-page">
    <!-- 概览 + 指标卡片 -->
    <el-card shadow="hover" class="overview-card" v-loading="loading">
      <template #header>
        <div class="card-title">
          <el-icon color="#00d4ff"><Odometer /></el-icon>
          <span>血糖指标概览</span>
          <el-tag v-if="!hasMetrics" type="info" effect="plain" size="small" class="push-right">暂无数据</el-tag>
          <el-tag v-else-if="!metrics.enough" type="info" effect="plain" size="small" class="push-right">样本不足，仅供参考</el-tag>
          <el-tag v-else-if="metrics.tirAchieved" type="success" effect="dark" size="small" class="push-right">TIR 达标</el-tag>
          <el-tag v-else type="warning" effect="dark" size="small" class="push-right">TIR 未达标</el-tag>
        </div>
      </template>

      <div class="overview-meta">
        <span>统计窗口：{{ windowText }}</span>
        <span>有效读数：{{ metrics.sampleCount || 0 }} 条</span>
        <span v-if="metrics.droppedCount">已忽略异常值：{{ metrics.droppedCount }} 条</span>
        <span class="muted">指标全部由规则引擎计算，与模型回答无关</span>
      </div>

      <div class="stat-grid">
        <div v-for="c in metricCards" :key="c.label" class="stat-card" :class="'tone-' + c.tone">
          <div class="stat-label">{{ c.label }}</div>
          <div class="stat-value">
            {{ c.value }}<small v-if="c.unit">{{ c.unit }}</small>
          </div>
          <div class="stat-hint">{{ c.hint }}</div>
        </div>
      </div>
    </el-card>

    <el-row :gutter="16">
      <!-- TIR 环形图 -->
      <el-col :xs="24" :md="9">
        <el-card shadow="hover" class="chart-card" v-loading="loading">
          <template #header>
            <div class="card-title">
              <el-icon color="#10b981"><PieChart /></el-icon>
              <span>目标范围内占比</span>
            </div>
          </template>
          <div ref="tirChart" class="chart-box"></div>
          <div class="chart-foot muted">
            范围内 3.9 ~ 10.0 mmol/L；TIR ≥ 70% 视为控制达标
          </div>
        </el-card>
      </el-col>

      <!-- 日聚合趋势 -->
      <el-col :xs="24" :md="15">
        <el-card shadow="hover" class="chart-card" v-loading="loading">
          <template #header>
            <div class="card-title">
              <el-icon color="#a855f7"><TrendCharts /></el-icon>
              <span>日聚合趋势</span>
              <el-radio-group v-model="trendDays" size="small" class="push-right" @change="loadTrend">
                <el-radio-button :value="7">7 天</el-radio-button>
                <el-radio-button :value="14">14 天</el-radio-button>
                <el-radio-button :value="30">30 天</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="trendChart" class="chart-box chart-box-lg"></div>
          <div class="chart-foot">
            <span class="muted">红线区域 = 当天有低血糖事件；虚线 = TIR 达标线 70%；断点 = 当天没有记录</span>
            <el-button
              link
              type="primary"
              size="small"
              :loading="backfilling"
              @click="doBackfill"
            >补齐历史聚合（最近 7 天）</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { Odometer, PieChart, TrendCharts } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { recordApi } from '../api'
import { demoGlucoseMetrics, demoDailyGlucoseStats } from '../mock/demoData'
import { isEmpty } from '../mock/demoManager'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const userInfo = userStore.userInfo

const loading = ref(false)
const backfilling = ref(false)
const trendDays = ref(14)

const metrics = reactive({})
const dailyStats = ref([])

const tirChart = ref(null)
const trendChart = ref(null)
let charts = []

const handleResize = () => charts.forEach(c => c.resize())

const hasMetrics = computed(() => !isEmpty(metrics))

const windowText = computed(() => {
  if (!metrics.windowFrom || !metrics.windowTo) return '--'
  const fmt = (s) => String(s).replace('T', ' ').slice(0, 16)
  return `${fmt(metrics.windowFrom)} ~ ${fmt(metrics.windowTo)}`
})

const fmtNum = (v, unit = '', digits = 1) =>
  (v === null || v === undefined) ? '--' : Number(v).toFixed(digits) + unit

const metricCards = computed(() => [
  {
    label: 'TIR 目标范围内占比',
    value: fmtNum(metrics.tirPercent, '%'),
    tone: metrics.tirPercent >= 70 ? 'success' : 'warning',
    hint: '达标线 70%'
  },
  {
    label: '平均血糖',
    value: fmtNum(metrics.meanGlucose, ' mmol/L'),
    tone: 'primary',
    hint: `空腹 ${fmtNum(metrics.fastingAvg)} / 餐后 ${fmtNum(metrics.postprandialAvg)}`
  },
  {
    label: '变异系数 CV',
    value: fmtNum(metrics.cv, '%'),
    tone: metrics.cv !== null && metrics.cv !== undefined && metrics.cv <= 36 ? 'success' : 'warning',
    hint: `标准差 ${fmtNum(metrics.sd)}，≤36% 视为平稳`
  },
  {
    label: '估算糖化 GMI',
    value: fmtNum(metrics.gmiPercent, '%'),
    tone: 'info',
    hint: '由平均血糖推算，非实测'
  },
  {
    label: '低血糖事件',
    value: metrics.hypoEventCount ?? '--',
    tone: (metrics.hypoEventCount > 0) ? 'danger' : 'success',
    hint: `严重低血糖 ${metrics.severeHypoCount ?? 0} 次`
  },
  {
    label: '血糖极值',
    value: `${fmtNum(metrics.minGlucose)} ~ ${fmtNum(metrics.maxGlucose)}`,
    tone: 'default',
    hint: `偏高 ${metrics.hyperCount ?? 0} 次`
  }
])

const loadMetrics = async () => {
  try {
    const res = await recordApi.bloodSugarMetrics(userInfo.id, { days: 14 })
    const real = (res.data.code === 200) ? res.data.data : null
    Object.assign(metrics, isEmpty(real) ? demoGlucoseMetrics : real)
  } catch {
    Object.assign(metrics, demoGlucoseMetrics)
  }
}

const loadTrend = async () => {
  try {
    const res = await recordApi.bloodSugarDaily(userInfo.id, { days: trendDays.value })
    const real = (res.data.code === 200) ? (res.data.data || []) : []
    dailyStats.value = real.length ? real : demoDailyGlucoseStats.slice(-trendDays.value)
  } catch {
    dailyStats.value = demoDailyGlucoseStats.slice(-trendDays.value)
  }
  await nextTick()
  renderTrendChart()
}

const load = async () => {
  loading.value = true
  try {
    await Promise.all([loadMetrics(), loadTrend()])
    await nextTick()
    renderTirChart()
  } finally {
    loading.value = false
  }
}

const doBackfill = async () => {
  backfilling.value = true
  try {
    const res = await recordApi.backfillDaily(userInfo.id, {})
    if (res.data.code === 200) {
      // 后端返回的是受影响行数之和，不是天数，所以这里不报具体数字
      ElMessage.success('历史聚合已补齐')
      await loadTrend()
    }
  } finally {
    backfilling.value = false
  }
}

const sciFiTheme = () => ({
  textStyle: { color: '#94a3b8' },
  legend: { textStyle: { color: '#94a3b8' } }
})

const renderTirChart = () => {
  charts.forEach(c => c.dispose())
  charts = []
  if (!tirChart.value) return

  const tir = Number(metrics.tirPercent ?? 0)
  const tar = Number(metrics.tarPercent ?? 0)
  const tbr = Number(metrics.tbrPercent ?? 0)

  const c = echarts.init(tirChart.value)
  charts.push(c)
  c.setOption({
    ...sciFiTheme(),
    tooltip: { trigger: 'item', formatter: '{b}：{c}%' },
    legend: { bottom: 0, textStyle: { color: '#94a3b8' } },
    series: [{
      type: 'pie',
      radius: ['58%', '82%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: false,
      itemStyle: { borderColor: '#0b1220', borderWidth: 3 },
      label: {
        show: true,
        position: 'center',
        formatter: () => (hasMetrics.value ? `TIR\n${tir}%` : '--'),
        color: '#e2e8f0',
        fontSize: 20,
        fontWeight: 'bold',
        lineHeight: 26
      },
      emphasis: { label: { show: true } },
      labelLine: { show: false },
      data: [
        { value: tir, name: 'TIR 范围内', itemStyle: { color: '#10b981' } },
        { value: tar, name: 'TAR 偏高', itemStyle: { color: '#e6a23c' } },
        { value: tbr, name: 'TBR 偏低', itemStyle: { color: '#f56c6c' } }
      ]
    }]
  })
  if (!hasMetrics.value) c.clear()
}

const renderTrendChart = () => {
  if (!trendChart.value) return

  const rows = dailyStats.value || []
  const dates = rows.map(r => r.statDate)
  const avgSeries = rows.map(r => (r.sampleCount ? r.avgGlucose : null))
  const tirSeries = rows.map(r => (r.sampleCount ? r.tirPercent : null))
  const fastingSeries = rows.map(r => (r.sampleCount ? r.fastingAvg : null))
  const postSeries = rows.map(r => (r.sampleCount ? r.postprandialAvg : null))

  // 有低血糖事件的日子，用红色区域标出来
  const hypoAreas = rows
    .filter(r => r.hypoEventCount > 0)
    .map(r => [
      { xAxis: r.statDate, itemStyle: { color: 'rgba(245,108,108,0.16)' } },
      { xAxis: r.statDate }
    ])

  const c = echarts.init(trendChart.value)
  charts.push(c)
  c.setOption({
    ...sciFiTheme(),
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        const idx = params[0]?.dataIndex ?? 0
        const row = rows[idx] || {}
        if (!row.sampleCount) return `${row.statDate}<br/>当天没有记录`
        return [
          row.statDate,
          `平均血糖：${row.avgGlucose ?? '--'} mmol/L`,
          `空腹：${row.fastingAvg ?? '--'} mmol/L`,
          `餐后2h：${row.postprandialAvg ?? '--'} mmol/L`,
          `TIR：${row.tirPercent ?? '--'}%`,
          `低血糖事件：${row.hypoEventCount ?? 0}`,
          `记录条数：${row.sampleCount}`
        ].join('<br/>')
      }
    },
    legend: { data: ['平均血糖', '空腹', '餐后2h', 'TIR'], bottom: 0, textStyle: { color: '#94a3b8' } },
    grid: { left: 55, right: 60, top: 25, bottom: 55 },
    xAxis: {
      type: 'category',
      data: dates,
      axisLabel: { color: '#94a3b8', rotate: 30, formatter: (v) => String(v).slice(5) },
      axisLine: { lineStyle: { color: '#334155' } }
    },
    yAxis: [
      {
        type: 'value', name: 'mmol/L', min: 3, max: 14,
        nameTextStyle: { color: '#94a3b8' },
        axisLabel: { color: '#94a3b8' },
        splitLine: { lineStyle: { color: 'rgba(51,65,85,0.12)' } }
      },
      {
        type: 'value', name: 'TIR %', min: 0, max: 100,
        nameTextStyle: { color: '#94a3b8' },
        axisLabel: { color: '#94a3b8' },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '平均血糖',
        type: 'line',
        data: avgSeries,
        smooth: true,
        connectNulls: false,
        symbolSize: 6,
        lineStyle: { color: '#00d4ff', width: 2.5, shadowBlur: 10, shadowColor: 'rgba(0,212,255,0.3)' },
        itemStyle: { color: '#00d4ff' },
        markArea: { silent: true, data: hypoAreas }
      },
      {
        name: '空腹',
        type: 'line',
        data: fastingSeries,
        smooth: true,
        connectNulls: false,
        symbolSize: 4,
        lineStyle: { color: '#10b981', width: 1.5, type: 'dashed' },
        itemStyle: { color: '#10b981' }
      },
      {
        name: '餐后2h',
        type: 'line',
        data: postSeries,
        smooth: true,
        connectNulls: false,
        symbolSize: 4,
        lineStyle: { color: '#e6a23c', width: 1.5, type: 'dashed' },
        itemStyle: { color: '#e6a23c' }
      },
      {
        name: 'TIR',
        type: 'line',
        yAxisIndex: 1,
        data: tirSeries,
        smooth: true,
        connectNulls: false,
        symbolSize: 5,
        lineStyle: { color: '#a855f7', width: 2 },
        itemStyle: { color: '#a855f7' },
        markLine: {
          silent: true,
          symbol: 'none',
          label: { formatter: '达标 70%', color: '#94a3b8' },
          lineStyle: { color: 'rgba(168,85,247,0.6)', type: 'dashed' },
          data: [{ yAxis: 70 }]
        }
      }
    ]
  })
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
  load()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  charts.forEach(c => c.dispose())
  charts = []
})
</script>

<style scoped>
.metrics-page { max-width: 1200px; }
.overview-card { margin-bottom: 16px; }
.chart-card { margin-bottom: 16px; }
.card-title { display: flex; align-items: center; gap: 8px; font-size: 16px; font-weight: 600; }
.push-right { margin-left: auto; }

.overview-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  font-size: 13px;
  color: #94a3b8;
  margin-bottom: 14px;
}
.muted { color: #64748b; font-size: 12px; }

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 12px;
}
.stat-card {
  border: 1px solid rgba(51, 65, 85, 0.6);
  border-radius: 10px;
  padding: 14px 16px;
  background: rgba(15, 23, 42, 0.55);
  transition: transform .2s ease, border-color .2s ease;
}
.stat-card:hover { transform: translateY(-2px); }
.stat-label { font-size: 13px; color: #94a3b8; margin-bottom: 6px; }
.stat-value { font-size: 24px; font-weight: 700; color: #e2e8f0; line-height: 1.2; }
.stat-value small { font-size: 13px; font-weight: 400; color: #94a3b8; margin-left: 4px; }
.stat-hint { font-size: 12px; color: #64748b; margin-top: 6px; }

.tone-success { border-color: rgba(16, 185, 129, .55); }
.tone-success .stat-value { color: #10b981; }
.tone-warning { border-color: rgba(230, 162, 60, .55); }
.tone-warning .stat-value { color: #e6a23c; }
.tone-danger { border-color: rgba(245, 108, 108, .55); }
.tone-danger .stat-value { color: #f56c6c; }
.tone-primary .stat-value { color: #00d4ff; }
.tone-info .stat-value { color: #a855f7; }

.chart-box { height: 260px; width: 100%; }
.chart-box-lg { height: 320px; }
.chart-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 6px;
}
</style>
