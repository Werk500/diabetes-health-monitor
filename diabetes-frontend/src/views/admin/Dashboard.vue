<template>
  <section class="admin-dashboard">
    <header class="dashboard-intro">
      <div>
        <p class="eyebrow">OPERATIONS / OVERVIEW</p>
        <h1>运营概览</h1>
        <p class="intro-copy">从用户、健康记录、AI 互动和内容触达四个方向观察系统运行状态。</p>
      </div>
      <div class="intro-meta">
        <span class="live-dot"></span>
        <span>数据实时汇总</span>
        <span class="meta-divider"></span>
        <span>{{ currentDate }}</span>
      </div>
    </header>

    <div v-if="loading" class="metrics-grid" aria-label="正在加载运营数据">
      <div v-for="index in 9" :key="index" class="metric-skeleton">
        <el-skeleton animated :rows="2" />
      </div>
    </div>

    <div v-else-if="errorMessage" class="load-error">
      <div>
        <strong>运营数据加载失败</strong>
        <p>{{ errorMessage }}</p>
      </div>
      <el-button type="primary" @click="loadDashboard">重新加载</el-button>
    </div>

    <template v-else>
      <div class="metrics-grid">
        <article v-for="metric in statCards" :key="metric.key" class="metric-card">
          <div class="metric-topline">
            <span class="metric-label">{{ metric.label }}</span>
            <span class="metric-icon" :style="{ color: metric.color, background: metric.surface }">
              <el-icon :size="17"><component :is="metric.icon" /></el-icon>
            </span>
          </div>
          <strong class="metric-value">{{ formatNumber(stats[metric.key]) }}</strong>
          <p class="metric-hint">{{ metric.hint }}</p>
        </article>
      </div>

      <div class="dashboard-grid">
        <section class="panel chart-panel">
          <div class="panel-header">
            <div>
              <p class="panel-kicker">CONTENT MIX</p>
              <h2>文章分类结构</h2>
            </div>
            <el-button text type="primary" @click="router.push('/admin/articles')">管理内容</el-button>
          </div>
          <div class="chart-wrap">
            <div ref="categoryChartEl" class="chart-box"></div>
            <div v-if="!hasArticles" class="chart-empty">
              <el-icon :size="28"><Document /></el-icon>
              <span>还没有可统计的文章</span>
            </div>
          </div>
        </section>

        <section class="panel activity-panel">
          <div class="panel-header">
            <div>
              <p class="panel-kicker">HEALTH ACTIVITY</p>
              <h2>健康数据活跃度</h2>
            </div>
            <span class="panel-note">累计记录量</span>
          </div>
          <div ref="activityChartEl" class="activity-chart"></div>
        </section>

        <section class="panel focus-panel">
          <div class="panel-header">
            <div>
              <p class="panel-kicker">NEXT ACTIONS</p>
              <h2>运营入口</h2>
            </div>
          </div>
          <div class="focus-list">
            <button type="button" class="focus-item" @click="router.push('/admin/users')">
              <span class="focus-index">01</span>
              <span class="focus-copy">
                <strong>检查用户状态</strong>
                <small>{{ formatNumber(stats.userCount) }} 个账号，优先处理异常账号</small>
              </span>
              <el-icon><ArrowRight /></el-icon>
            </button>
            <button type="button" class="focus-item" @click="router.push('/admin/articles')">
              <span class="focus-index">02</span>
              <span class="focus-copy">
                <strong>推进内容触达</strong>
                <small>{{ unpushedArticleCount }} 篇文章尚未推送</small>
              </span>
              <el-icon><ArrowRight /></el-icon>
            </button>
            <button type="button" class="focus-item" @click="router.push('/admin/exercise')">
              <span class="focus-index">03</span>
              <span class="focus-copy">
                <strong>维护运动配置</strong>
                <small>{{ formatNumber(stats.exerciseTypeCount) }} 个运动类型可供记录</small>
              </span>
              <el-icon><ArrowRight /></el-icon>
            </button>
          </div>
        </section>

        <section class="panel reach-panel">
          <div class="panel-header">
            <div>
              <p class="panel-kicker">PUBLISHING</p>
              <h2>内容发布进度</h2>
            </div>
            <strong class="reach-rate">{{ publishedRate }}%</strong>
          </div>
          <div class="progress-track" aria-label="文章推送比例">
            <span class="progress-fill" :style="{ width: `${publishedRate}%` }"></span>
          </div>
          <div class="reach-breakdown">
            <div>
              <span>已推送</span>
              <strong>{{ formatNumber(stats.pushedCount) }}</strong>
            </div>
            <div>
              <span>待推送</span>
              <strong>{{ formatNumber(unpushedArticleCount) }}</strong>
            </div>
            <div>
              <span>文章总数</span>
              <strong>{{ formatNumber(stats.articleCount) }}</strong>
            </div>
          </div>
        </section>

        <section class="panel audit-overview-panel">
          <div class="panel-header">
            <div>
              <p class="panel-kicker">GOVERNANCE FEED</p>
              <h2>最近管理员操作</h2>
            </div>
            <div class="audit-panel-actions">
              <span
                class="audit-failure-badge"
                :class="{ active: Number(failedAuditCount) > 0 }"
              >
                失败操作 {{ failedAuditCount == null ? '—' : formatNumber(failedAuditCount) }}
              </span>
              <el-button text type="primary" @click="router.push('/admin/audit')">
                查看审计
              </el-button>
            </div>
          </div>

          <div v-if="recentAudits.length" class="audit-feed">
            <button
              v-for="audit in recentAudits"
              :key="audit.id"
              type="button"
              class="audit-record"
              @click="router.push('/admin/audit')"
            >
              <span class="audit-status-dot" :class="{ failed: audit.success !== 1 }"></span>
              <span class="audit-copy">
                <strong>{{ auditActionLabel(audit.actionCode) }} · {{ auditTargetLabel(audit) }}</strong>
                <small>{{ audit.operatorName || '未知操作人' }} · {{ audit.detail || '无补充说明' }}</small>
              </span>
              <time>{{ formatAuditTime(audit.createTime) }}</time>
            </button>
          </div>
          <div v-else class="audit-empty">
            <span>暂无管理员操作记录</span>
            <small>禁用、启用或删除用户后，操作会出现在这里。</small>
          </div>
        </section>
      </div>
    </template>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import {
  ArrowRight,
  Bicycle,
  ChatDotRound,
  DataBoard,
  DishDot,
  Document,
  Orange,
  Promotion,
  Tickets,
  User
} from '@element-plus/icons-vue'
import { adminApi } from '../../api'

const router = useRouter()
const categoryChartEl = ref(null)
const activityChartEl = ref(null)
const stats = ref({})
const categoryStats = ref({})
const recentAudits = ref([])
const failedAuditCount = ref(null)
const loading = ref(true)
const errorMessage = ref('')

let categoryChart = null
let activityChart = null

const currentDate = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit'
}).format(new Date())

const statCards = [
  {
    key: 'userCount',
    label: '注册用户',
    hint: '平台累计账号数',
    icon: User,
    color: '#72ddff',
    surface: 'rgba(0, 212, 255, 0.09)'
  },
  {
    key: 'bloodSugarCount',
    label: '血糖记录',
    hint: '核心健康数据量',
    icon: DataBoard,
    color: '#75e6c4',
    surface: 'rgba(16, 185, 129, 0.09)'
  },
  {
    key: 'bodyCount',
    label: '身体记录',
    hint: '体重与身体指标',
    icon: Orange,
    color: '#f5b86b',
    surface: 'rgba(245, 158, 11, 0.09)'
  },
  {
    key: 'dietCount',
    label: '饮食记录',
    hint: '饮食行为数据量',
    icon: DishDot,
    color: '#d7a4ff',
    surface: 'rgba(168, 85, 247, 0.09)'
  },
  {
    key: 'exerciseCount',
    label: '运动记录',
    hint: '运动行为数据量',
    icon: Bicycle,
    color: '#7ee3a7',
    surface: 'rgba(34, 197, 94, 0.09)'
  },
  {
    key: 'aiChatCount',
    label: 'AI 互动',
    hint: '累计对话消息数',
    icon: ChatDotRound,
    color: '#84d4ff',
    surface: 'rgba(14, 165, 233, 0.09)'
  },
  {
    key: 'articleCount',
    label: '健康文章',
    hint: '内容库文章总量',
    icon: Document,
    color: '#a8d7f7',
    surface: 'rgba(56, 189, 248, 0.08)'
  },
  {
    key: 'pushedCount',
    label: '已推送',
    hint: '完成用户触达的内容',
    icon: Promotion,
    color: '#f7c86a',
    surface: 'rgba(234, 179, 8, 0.08)'
  },
  {
    key: 'exerciseTypeCount',
    label: '运动类型',
    hint: '可选的运动配置数',
    icon: Tickets,
    color: '#f39ab5',
    surface: 'rgba(236, 72, 153, 0.08)'
  }
]

const unpushedArticleCount = computed(() => {
  const total = Number(stats.value.articleCount) || 0
  const pushed = Number(stats.value.pushedCount) || 0
  return Math.max(total - pushed, 0)
})

const publishedRate = computed(() => {
  const total = Number(stats.value.articleCount) || 0
  if (!total) return 0
  return Math.round(((Number(stats.value.pushedCount) || 0) / total) * 100)
})

const hasArticles = computed(() => {
  return Object.values(categoryStats.value).some(value => Number(value) > 0)
})

const formatNumber = value => {
  return new Intl.NumberFormat('zh-CN').format(Number(value) || 0)
}

const auditActionLabel = actionCode => {
  return {
    USER_ENABLE: '启用用户',
    USER_DISABLE: '禁用用户',
    USER_DELETE: '删除用户'
  }[actionCode] || actionCode || '管理操作'
}

const auditTargetLabel = audit => {
  const type = audit.targetType === 'USER' ? '用户' : audit.targetType || '目标'
  return `${type} #${audit.targetId ?? '—'}`
}

const formatAuditTime = value => {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value).replace('T', ' ')
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  }).format(date).replace(/\//g, '-')
}

const disposeCharts = () => {
  categoryChart?.dispose()
  activityChart?.dispose()
  categoryChart = null
  activityChart = null
}

const renderCharts = async () => {
  await nextTick()
  disposeCharts()

  if (categoryChartEl.value && hasArticles.value) {
    categoryChart = echarts.init(categoryChartEl.value)
    categoryChart.setOption({
      animationDuration: 700,
      animationEasing: 'cubicOut',
      color: ['#00c8f0', '#55d6a7', '#f0ad4e', '#cc8cff'],
      tooltip: {
        trigger: 'item',
        formatter: '{b}<br/>{c} 篇 · {d}%',
        backgroundColor: 'rgba(13, 17, 23, 0.96)',
        borderColor: 'rgba(0, 212, 255, 0.22)',
        textStyle: { color: '#dbeafe' }
      },
      legend: {
        bottom: 0,
        icon: 'circle',
        itemWidth: 7,
        itemHeight: 7,
        itemGap: 18,
        textStyle: {
          color: '#8291a7',
          fontSize: 11
        }
      },
      series: [
        {
          type: 'pie',
          radius: ['52%', '74%'],
          center: ['50%', '43%'],
          avoidLabelOverlap: true,
          itemStyle: {
            borderColor: '#0d1117',
            borderWidth: 4,
            borderRadius: 5
          },
          label: { show: false },
          emphasis: {
            scaleSize: 7,
            label: {
              show: true,
              color: '#e2e8f0',
              fontSize: 12,
              formatter: '{b}\n{d}%'
            }
          },
          data: [
            { name: '血糖监测', value: categoryStats.value.bloodSugar || 0 },
            { name: '控糖饮食', value: categoryStats.value.dietControl || 0 },
            { name: '并发症预防', value: categoryStats.value.complication || 0 },
            { name: '运动建议', value: categoryStats.value.exerciseSuggestion || 0 }
          ]
        }
      ]
    })
  }

  if (activityChartEl.value) {
    const activity = [
      { label: '血糖', value: Number(stats.value.bloodSugarCount) || 0 },
      { label: '身体', value: Number(stats.value.bodyCount) || 0 },
      { label: '饮食', value: Number(stats.value.dietCount) || 0 },
      { label: '运动', value: Number(stats.value.exerciseCount) || 0 },
      { label: 'AI', value: Number(stats.value.aiChatCount) || 0 }
    ]

    activityChart = echarts.init(activityChartEl.value)
    activityChart.setOption({
      animationDuration: 750,
      grid: { top: 8, right: 16, bottom: 6, left: 44, containLabel: false },
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        backgroundColor: 'rgba(13, 17, 23, 0.96)',
        borderColor: 'rgba(0, 212, 255, 0.22)',
        textStyle: { color: '#dbeafe' }
      },
      xAxis: {
        type: 'value',
        splitLine: { lineStyle: { color: 'rgba(71, 85, 105, 0.16)' } },
        axisLabel: { color: '#64748b', fontSize: 10 }
      },
      yAxis: {
        type: 'category',
        data: activity.map(item => item.label),
        axisTick: { show: false },
        axisLine: { show: false },
        axisLabel: { color: '#9aa9bc', fontSize: 11, margin: 12 }
      },
      series: [
        {
          type: 'bar',
          data: activity.map(item => item.value),
          barWidth: 10,
          showBackground: true,
          backgroundStyle: {
            color: 'rgba(51, 65, 85, 0.2)',
            borderRadius: 8
          },
          itemStyle: {
            borderRadius: 8,
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: 'rgba(0, 212, 255, 0.38)' },
              { offset: 1, color: '#00d4ff' }
            ])
          }
        }
      ]
    })
  }
}

const loadDashboard = async () => {
  loading.value = true
  errorMessage.value = ''
  disposeCharts()

  try {
    const auditPromise = Promise.all([
      adminApi.auditList({ page: 1, size: 5 }),
      adminApi.auditList({ page: 1, size: 1, success: 0 })
    ]).catch(() => null);

    const [statsResponse, categoryResponse] = await Promise.all([
      adminApi.stats(),
      adminApi.categoryStats()
    ])

    if (statsResponse.data.code !== 200 || categoryResponse.data.code !== 200) {
      throw new Error('服务端未能返回完整的运营数据')
    }

    stats.value = statsResponse.data.data || {}
    categoryStats.value = categoryResponse.data.data || {}

    const auditResponses = await auditPromise
    if (auditResponses) {
      const [recentResponse, failedResponse] = auditResponses
      recentAudits.value = recentResponse.data?.data?.records || []
      failedAuditCount.value = Number(failedResponse.data?.data?.total) || 0
    }
  } catch (error) {
    errorMessage.value = error?.response?.data?.msg || error?.message || '请稍后重试'
    return
  } finally {
    loading.value = false
  }

  await renderCharts()
}

const handleResize = () => {
  categoryChart?.resize()
  activityChart?.resize()
}

onMounted(() => {
  loadDashboard()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  disposeCharts()
})
</script>

<style scoped>
.admin-dashboard {
  color: var(--text-primary);
}

.dashboard-intro {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 28px;
  margin-bottom: 24px;
}

.eyebrow,
.panel-kicker {
  margin: 0 0 8px;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 1.8px;
  color: var(--neon-blue);
}

h1 {
  margin: 0;
  font-size: clamp(30px, 4vw, 46px);
  font-weight: 600;
  letter-spacing: -1.4px;
  line-height: 1.08;
  text-wrap: balance;
}

.intro-copy {
  max-width: 620px;
  margin: 12px 0 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.8;
  text-wrap: pretty;
}

.intro-meta {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 8px;
  padding: 9px 12px;
  font-family: var(--font-mono);
  font-size: 10px;
  color: var(--text-muted);
  background: rgba(17, 24, 39, 0.62);
  border: 1px solid rgba(51, 65, 85, 0.42);
  border-radius: 8px;
}

.live-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--neon-green);
  box-shadow: 0 0 8px rgba(16, 185, 129, 0.6);
}

.meta-divider {
  width: 1px;
  height: 10px;
  background: var(--border-subtle);
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 14px;
}

.metric-card,
.panel,
.metric-skeleton {
  position: relative;
  overflow: hidden;
  background:
    linear-gradient(145deg, rgba(20, 29, 44, 0.82), rgba(13, 19, 30, 0.72));
  border: 1px solid rgba(51, 65, 85, 0.38);
  border-radius: 14px;
}

.metric-card {
  min-height: 148px;
  padding: 17px 18px 18px;
  transition:
    transform 0.24s ease,
    border-color 0.24s ease,
    box-shadow 0.24s ease;
}

.metric-card::after {
  content: '';
  position: absolute;
  right: -30px;
  bottom: -46px;
  width: 110px;
  height: 110px;
  background: radial-gradient(circle, rgba(0, 212, 255, 0.08), transparent 65%);
  pointer-events: none;
}

.metric-card:hover {
  border-color: rgba(0, 212, 255, 0.27);
  box-shadow: 0 14px 34px rgba(2, 8, 20, 0.3);
  transform: translateY(-2px);
}

.metric-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.metric-label {
  font-size: 12px;
  color: var(--text-secondary);
}

.metric-icon {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  border-radius: 9px;
}

.metric-value {
  display: block;
  margin-top: 20px;
  font-family: var(--font-mono);
  font-size: 31px;
  font-variant-numeric: tabular-nums;
  font-weight: 600;
  letter-spacing: -1px;
  line-height: 1;
}

.metric-hint {
  margin: 9px 0 0;
  color: var(--text-muted);
  font-size: 10px;
}

.metric-skeleton {
  min-height: 148px;
  padding: 24px 18px;
}

.load-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 18px 20px;
  background: rgba(127, 29, 29, 0.12);
  border: 1px solid rgba(248, 113, 113, 0.2);
  border-radius: 12px;
}

.load-error strong {
  font-size: 13px;
  font-weight: 500;
}

.load-error p {
  margin: 5px 0 0;
  color: var(--text-muted);
  font-size: 11px;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(340px, 0.85fr);
  gap: 14px;
}

.panel {
  padding: 20px;
}

.chart-panel,
.activity-panel {
  min-height: 360px;
}

.focus-panel,
.reach-panel {
  min-height: 292px;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.panel-header h2 {
  margin: 0;
  font-size: 17px;
  font-weight: 500;
  letter-spacing: -0.25px;
}

.panel-note {
  color: var(--text-muted);
  font-size: 10px;
}

.chart-wrap,
.chart-box,
.activity-chart {
  height: 285px;
}

.chart-wrap {
  position: relative;
}

.chart-box,
.activity-chart {
  width: 100%;
}

.chart-empty {
  position: absolute;
  inset: 40px 0 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: var(--text-muted);
  font-size: 12px;
}

.focus-list {
  display: grid;
  gap: 8px;
  margin-top: 20px;
}

.focus-item {
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 12px 13px;
  color: var(--text-primary);
  text-align: left;
  background: rgba(15, 23, 36, 0.58);
  border: 1px solid rgba(51, 65, 85, 0.28);
  border-radius: 10px;
  cursor: pointer;
  transition:
    background-color 0.2s ease,
    border-color 0.2s ease,
    transform 0.2s ease;
}

.focus-item:hover {
  background: rgba(0, 212, 255, 0.05);
  border-color: rgba(0, 212, 255, 0.22);
  transform: translateX(2px);
}

.focus-item:focus-visible {
  outline: 2px solid var(--neon-blue);
  outline-offset: 2px;
}

.focus-index {
  font-family: var(--font-mono);
  font-size: 10px;
  color: var(--neon-blue);
}

.focus-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 3px;
}

.focus-copy strong {
  font-size: 12px;
  font-weight: 500;
}

.focus-copy small {
  overflow: hidden;
  color: var(--text-muted);
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.reach-rate {
  font-family: var(--font-mono);
  font-size: 28px;
  font-weight: 500;
  color: var(--neon-blue);
}

.progress-track {
  height: 8px;
  margin-top: 34px;
  overflow: hidden;
  background: rgba(51, 65, 85, 0.3);
  border-radius: 999px;
}

.progress-fill {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, rgba(0, 212, 255, 0.55), var(--neon-blue));
  border-radius: inherit;
  box-shadow: 0 0 14px rgba(0, 212, 255, 0.28);
  transition: width 0.7s cubic-bezier(0.16, 1, 0.3, 1);
}

.reach-breakdown {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 34px;
}

.reach-breakdown div {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  background: rgba(15, 23, 36, 0.5);
  border-radius: 9px;
}

.reach-breakdown span {
  color: var(--text-muted);
  font-size: 10px;
}

.reach-breakdown strong {
  font-family: var(--font-mono);
  font-size: 18px;
  font-variant-numeric: tabular-nums;
  font-weight: 500;
}

.audit-overview-panel {
  grid-column: 1 / -1;
}

.audit-panel-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.audit-failure-badge {
  padding: 5px 8px;
  font-family: var(--font-mono);
  font-size: 9px;
  color: var(--text-muted);
  background: rgba(51, 65, 85, 0.18);
  border: 1px solid rgba(51, 65, 85, 0.28);
  border-radius: 5px;
}

.audit-failure-badge.active {
  color: #f3a3ae;
  background: rgba(127, 29, 29, 0.1);
  border-color: rgba(248, 113, 113, 0.16);
}

.audit-feed {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-top: 18px;
}

.audit-record {
  display: grid;
  grid-template-columns: 8px minmax(0, 1fr) auto;
  align-items: center;
  gap: 11px;
  min-width: 0;
  padding: 12px 13px;
  color: var(--text-primary);
  text-align: left;
  background: rgba(15, 23, 36, 0.5);
  border: 1px solid rgba(51, 65, 85, 0.26);
  border-radius: 9px;
  transition:
    border-color 0.2s ease,
    background-color 0.2s ease,
    transform 0.2s ease;
}

.audit-record:hover {
  background: rgba(0, 212, 255, 0.045);
  border-color: rgba(0, 212, 255, 0.18);
  transform: translateY(-1px);
}

.audit-record:focus-visible {
  outline: 2px solid var(--neon-blue);
  outline-offset: 2px;
}

.audit-status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #5fd6bd;
  box-shadow: 0 0 8px rgba(16, 185, 129, 0.4);
}

.audit-status-dot.failed {
  background: #ef8d9b;
  box-shadow: 0 0 8px rgba(244, 63, 94, 0.35);
}

.audit-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.audit-copy strong,
.audit-copy small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audit-copy strong {
  font-size: 11px;
  font-weight: 500;
}

.audit-copy small {
  color: var(--text-muted);
  font-size: 9px;
}

.audit-record time {
  color: var(--text-muted);
  font-family: var(--font-mono);
  font-size: 8px;
  white-space: nowrap;
}

.audit-empty {
  display: flex;
  min-height: 92px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 5px;
  margin-top: 14px;
  color: var(--text-secondary);
  font-size: 11px;
  border: 1px dashed rgba(51, 65, 85, 0.28);
  border-radius: 9px;
}

.audit-empty small {
  color: var(--text-muted);
  font-size: 9px;
}

@media (max-width: 1100px) {
  .metrics-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .audit-feed {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .dashboard-intro {
    align-items: flex-start;
    flex-direction: column;
  }

  .metrics-grid {
    grid-template-columns: 1fr;
  }

  .intro-meta {
    flex-wrap: wrap;
  }

  .panel {
    padding: 17px;
  }

  .audit-panel-actions {
    align-items: flex-end;
    flex-direction: column;
    gap: 4px;
  }
}
</style>
