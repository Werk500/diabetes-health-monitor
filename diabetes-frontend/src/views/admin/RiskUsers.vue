<template>
  <section class="risk-users">
    <header class="page-heading">
      <div>
        <p class="eyebrow">CARE / RISK MONITORING</p>
        <h1>风险用户</h1>
        <p>结合近期血糖异常与记录依从性生成运营预警，帮助管理员优先跟进需要关注的用户。</p>
      </div>
      <span class="total-chip">待跟进 {{ total }} 人</span>
    </header>

    <div class="risk-note">
      <el-icon><InfoFilled /></el-icon>
      <span>评分仅用于健康管理运营提示，依据近 7 天血糖记录与最近测量时间计算，不构成医疗诊断。</span>
    </div>

    <section class="overview-grid" aria-label="风险用户概览">
      <article
        v-for="item in overviewCards"
        :key="item.key"
        class="overview-card"
        :class="item.className"
      >
        <div class="overview-icon">
          <el-icon><component :is="item.icon" /></el-icon>
        </div>
        <div>
          <span>{{ item.label }}</span>
          <strong>{{ overview[item.key] ?? 0 }}</strong>
          <small>{{ item.hint }}</small>
        </div>
      </article>
    </section>

    <section class="table-panel">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          clearable
          placeholder="搜索用户名、姓名或手机号"
          class="search-input"
          @clear="search"
          @keyup.enter="search"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-select v-model="riskLevel" class="level-select" placeholder="全部风险等级" clearable>
          <el-option label="高风险" :value="1" />
          <el-option label="中风险" :value="2" />
          <el-option label="持续观察" :value="3" />
        </el-select>

        <el-button type="primary" :loading="loading" @click="search">筛选</el-button>
        <el-button :loading="overviewLoading" @click="refreshAll">刷新评估</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="users"
        row-key="userId"
        class="risk-table"
        empty-text="当前没有需要跟进的风险用户"
      >
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="reason-panel">
              <div class="reason-title">
                <span>评估依据</span>
                <small>综合风险分 {{ row.riskScore }} / 100</small>
              </div>
              <ul v-if="reasonList(row).length">
                <li v-for="reason in reasonList(row)" :key="reason">{{ reason }}</li>
              </ul>
              <p v-else>暂无可展示的风险依据，请结合用户健康记录人工复核。</p>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="用户" min-width="190">
          <template #default="{ row }">
            <div class="user-cell">
              <span class="user-avatar">{{ userInitial(row) }}</span>
              <div>
                <strong>{{ row.realName || row.username }}</strong>
                <small>{{ row.username }}<template v-if="row.phone"> · {{ row.phone }}</template></small>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="风险等级" width="118">
          <template #default="{ row }">
            <span class="risk-badge" :class="levelMeta(row.riskLevel).className">
              <span class="risk-dot"></span>
              {{ levelMeta(row.riskLevel).label }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="风险分" width="150">
          <template #default="{ row }">
            <div class="score-cell">
              <el-progress
                :percentage="normalizeScore(row.riskScore)"
                :stroke-width="5"
                :show-text="false"
                :color="scoreColor(row.riskLevel)"
              />
              <span>{{ normalizeScore(row.riskScore) }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="血糖记录" min-width="165">
          <template #default="{ row }">
            <div class="metric-cell">
              <strong>{{ row.recentRecordCount ?? 0 }} 次 <small>近 7 天</small></strong>
              <span :class="{ warning: Number(row.abnormalCount) > 0 }">
                异常 {{ row.abnormalCount ?? 0 }} 次
              </span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="最近测量" min-width="165">
          <template #default="{ row }">
            <div class="metric-cell">
              <strong>{{ row.lastMeasureTime || '暂无记录' }}</strong>
              <span :class="{ warning: Number(row.inactiveDays) >= 7 }">
                {{ inactiveText(row.inactiveDays) }}
              </span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="最高血糖" width="125" align="right">
          <template #default="{ row }">
            <strong :class="['glucose-value', { warning: Number(row.highestBloodSugar) >= 11.1 }]">
              {{ row.highestBloodSugar != null ? `${row.highestBloodSugar} mmol/L` : '—' }}
            </strong>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          :total="total"
          :page-size="pageSize"
          :disabled="loading"
          layout="prev, pager, next"
          background
          @current-change="load"
        />
      </div>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import {
  Bell,
  Clock,
  InfoFilled,
  Search,
  TrendCharts,
  WarningFilled
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '../../api'

const users = ref([])
const keyword = ref('')
const riskLevel = ref(null)
const page = ref(1)
const pageSize = 10
const total = ref(0)
const loading = ref(false)
const overviewLoading = ref(false)
const overview = ref({
  highRiskUsers: 0,
  mediumRiskUsers: 0,
  observationUsers: 0,
  inactiveUsers: 0
})

const overviewCards = computed(() => [
  {
    key: 'highRiskUsers',
    label: '高风险用户',
    hint: '优先人工跟进',
    icon: WarningFilled,
    className: 'danger'
  },
  {
    key: 'mediumRiskUsers',
    label: '中风险用户',
    hint: '近期异常或漏记',
    icon: TrendCharts,
    className: 'warning'
  },
  {
    key: 'observationUsers',
    label: '持续观察',
    hint: '需要继续关注',
    icon: Bell,
    className: 'observing'
  },
  {
    key: 'inactiveUsers',
    label: '长期未记录',
    hint: '超过 7 天无血糖',
    icon: Clock,
    className: 'muted'
  }
])

const requestData = response => {
  if (response.data.code !== 200) {
    throw new Error(response.data.msg || '风险用户数据加载失败')
  }
  return response.data.data
}

const loadOverview = async () => {
  overviewLoading.value = true
  try {
    const data = requestData(await adminApi.riskUserOverview())
    overview.value = {
      highRiskUsers: Number(data?.highRiskUsers) || 0,
      mediumRiskUsers: Number(data?.mediumRiskUsers) || 0,
      observationUsers: Number(data?.observationUsers) || 0,
      inactiveUsers: Number(data?.inactiveUsers) || 0
    }
  } catch (error) {
    ElMessage.error(error?.response?.data?.msg || error?.message || '风险概览加载失败')
  } finally {
    overviewLoading.value = false
  }
}

const load = async () => {
  loading.value = true
  try {
    const params = {
      page: page.value,
      size: pageSize,
      keyword: keyword.value.trim()
    }
    if (riskLevel.value !== null) params.riskLevel = riskLevel.value

    const data = requestData(await adminApi.riskUserList(params))
    users.value = data.records || []
    total.value = Number(data.total) || 0
  } catch (error) {
    users.value = []
    total.value = 0
    ElMessage.error(error?.response?.data?.msg || error?.message || '风险用户加载失败')
  } finally {
    loading.value = false
  }
}

const search = () => {
  page.value = 1
  load()
}

const refreshAll = async () => {
  await Promise.all([loadOverview(), load()])
}

const normalizeScore = value => Math.min(100, Math.max(0, Number(value) || 0))

const levelMeta = level => {
  const map = {
    1: { label: '高风险', className: 'high' },
    2: { label: '中风险', className: 'medium' },
    3: { label: '持续观察', className: 'observe' }
  }
  return map[level] || map[3]
}

const scoreColor = level => {
  if (level === 1) return '#fb7185'
  if (level === 2) return '#fbbf24'
  return '#38bdf8'
}

const userInitial = row => (row.realName || row.username || 'U').charAt(0).toUpperCase()

const inactiveText = days => {
  const value = Number(days)
  if (!Number.isFinite(value)) return '暂无测量时间'
  if (value === 0) return '今天已测量'
  return `${value} 天未测量`
}

const reasonList = row => {
  if (Array.isArray(row.riskReasons)) return row.riskReasons
  if (typeof row.riskReasons === 'string' && row.riskReasons.trim()) {
    return row.riskReasons.split(/[；;|]/).map(item => item.trim()).filter(Boolean)
  }
  return []
}

onMounted(refreshAll)
</script>

<style scoped>
.risk-users {
  color: var(--text-primary);
}

.page-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 18px;
}

.eyebrow {
  margin: 0 0 8px;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 1.8px;
  color: var(--neon-blue);
}

h1 {
  margin: 0;
  font-size: clamp(28px, 4vw, 42px);
  font-weight: 600;
  letter-spacing: -1.2px;
  line-height: 1.1;
}

.page-heading p:not(.eyebrow) {
  max-width: 720px;
  margin: 10px 0 0;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.7;
}

.total-chip {
  flex-shrink: 0;
  padding: 7px 10px;
  font-family: var(--font-mono);
  font-size: 10px;
  color: var(--text-secondary);
  background: rgba(17, 24, 39, 0.65);
  border: 1px solid rgba(51, 65, 85, 0.42);
  border-radius: 7px;
}

.risk-note {
  display: flex;
  align-items: flex-start;
  gap: 9px;
  margin-bottom: 16px;
  padding: 11px 14px;
  color: #9fb0c5;
  font-size: 11px;
  line-height: 1.65;
  background: rgba(14, 26, 42, 0.64);
  border: 1px solid rgba(56, 189, 248, 0.16);
  border-radius: 10px;
}

.risk-note .el-icon {
  flex-shrink: 0;
  margin-top: 2px;
  color: #38bdf8;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.overview-card {
  display: flex;
  align-items: center;
  gap: 13px;
  min-height: 96px;
  padding: 16px;
  overflow: hidden;
  background: linear-gradient(145deg, rgba(20, 29, 44, 0.84), rgba(13, 19, 30, 0.72));
  border: 1px solid rgba(51, 65, 85, 0.38);
  border-radius: 13px;
}

.overview-card.danger {
  border-color: rgba(251, 113, 133, 0.25);
}

.overview-card.warning {
  border-color: rgba(251, 191, 36, 0.22);
}

.overview-icon {
  display: grid;
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  place-items: center;
  color: #38bdf8;
  background: rgba(56, 189, 248, 0.08);
  border: 1px solid rgba(56, 189, 248, 0.14);
  border-radius: 10px;
}

.danger .overview-icon {
  color: #fb7185;
  background: rgba(251, 113, 133, 0.08);
  border-color: rgba(251, 113, 133, 0.16);
}

.warning .overview-icon {
  color: #fbbf24;
  background: rgba(251, 191, 36, 0.08);
  border-color: rgba(251, 191, 36, 0.16);
}

.muted .overview-icon {
  color: #94a3b8;
  background: rgba(148, 163, 184, 0.07);
  border-color: rgba(148, 163, 184, 0.13);
}

.overview-card > div:last-child {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.overview-card span {
  color: var(--text-secondary);
  font-size: 11px;
}

.overview-card strong {
  font-family: var(--font-mono);
  font-size: 25px;
  font-weight: 500;
  line-height: 1.3;
}

.overview-card small {
  overflow: hidden;
  color: var(--text-muted);
  font-size: 9px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.table-panel {
  padding: 18px;
  background: linear-gradient(145deg, rgba(20, 29, 44, 0.82), rgba(13, 19, 30, 0.72));
  border: 1px solid rgba(51, 65, 85, 0.38);
  border-radius: 14px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.search-input {
  width: min(100%, 310px);
}

.level-select {
  width: 155px;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-avatar {
  display: grid;
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  place-items: center;
  color: #bae6fd;
  font-family: var(--font-mono);
  font-size: 12px;
  background: linear-gradient(145deg, rgba(14, 165, 233, 0.2), rgba(99, 102, 241, 0.12));
  border: 1px solid rgba(56, 189, 248, 0.2);
  border-radius: 9px;
}

.user-cell > div {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 3px;
}

.user-cell strong {
  overflow: hidden;
  font-size: 12px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-cell small {
  color: var(--text-muted);
  font-family: var(--font-mono);
  font-size: 9px;
}

.risk-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  font-size: 10px;
  border: 1px solid currentColor;
  border-radius: 999px;
}

.risk-badge.high {
  color: #fb7185;
  background: rgba(251, 113, 133, 0.07);
}

.risk-badge.medium {
  color: #fbbf24;
  background: rgba(251, 191, 36, 0.07);
}

.risk-badge.observe {
  color: #38bdf8;
  background: rgba(56, 189, 248, 0.07);
}

.risk-dot {
  width: 5px;
  height: 5px;
  background: currentColor;
  border-radius: 50%;
  box-shadow: 0 0 7px currentColor;
}

.score-cell {
  display: flex;
  align-items: center;
  gap: 9px;
}

.score-cell :deep(.el-progress) {
  flex: 1;
}

.score-cell > span {
  width: 22px;
  color: var(--text-secondary);
  font-family: var(--font-mono);
  font-size: 10px;
  text-align: right;
}

.metric-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.metric-cell strong {
  font-size: 11px;
  font-weight: 500;
}

.metric-cell strong small {
  color: var(--text-muted);
  font-size: 9px;
  font-weight: 400;
}

.metric-cell span {
  color: var(--text-muted);
  font-size: 9px;
}

.metric-cell span.warning {
  color: #fbbf24;
}

.glucose-value {
  color: var(--text-secondary);
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 500;
}

.glucose-value.warning {
  color: #fb7185;
}

.reason-panel {
  margin: 4px 18px 10px 54px;
  padding: 14px 16px;
  background: rgba(8, 14, 24, 0.5);
  border: 1px solid rgba(51, 65, 85, 0.3);
  border-radius: 10px;
}

.reason-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 9px;
}

.reason-title span {
  font-size: 11px;
  font-weight: 500;
}

.reason-title small {
  color: var(--text-muted);
  font-family: var(--font-mono);
  font-size: 9px;
}

.reason-panel ul {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.reason-panel li {
  padding: 5px 8px;
  color: #a9bdd3;
  font-size: 10px;
  background: rgba(30, 41, 59, 0.48);
  border: 1px solid rgba(71, 85, 105, 0.34);
  border-radius: 6px;
}

.reason-panel p {
  margin: 0;
  color: var(--text-muted);
  font-size: 10px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

@media (max-width: 1080px) {
  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .page-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .overview-grid {
    grid-template-columns: 1fr;
  }

  .toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .search-input,
  .level-select {
    width: 100%;
  }

  .reason-panel {
    margin-left: 12px;
  }
}
</style>
