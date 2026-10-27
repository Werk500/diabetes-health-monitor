<template>
  <section class="audit-page">
    <header class="page-heading">
      <div>
        <p class="eyebrow">GOVERNANCE / AUDIT TRAIL</p>
        <h1>操作审计</h1>
        <p class="intro-copy">
          查看管理员对账号执行的启停与删除操作。记录只读保留，用于安全追溯和问题复盘。
        </p>
      </div>
      <div class="heading-meta">
        <span><el-icon><Lock /></el-icon>只读留痕</span>
        <span><el-icon><Clock /></el-icon>时间倒序</span>
      </div>
    </header>

    <div class="summary-strip">
      <div class="summary-item">
        <span>筛选结果</span>
        <strong>{{ formatNumber(total) }}</strong>
      </div>
      <div class="summary-item">
        <span>当前页失败</span>
        <strong :class="{ danger: failedOnPage > 0 }">{{ failedOnPage }}</strong>
      </div>
      <div class="summary-item summary-wide">
        <span>最近一次记录</span>
        <strong class="time-value">{{ latestTime }}</strong>
      </div>
    </div>

    <section class="filter-panel">
      <div class="filter-title">
        <div>
          <p class="panel-kicker">FILTERS</p>
          <h2>筛选操作记录</h2>
        </div>
        <span class="record-count">当前第 {{ page }} 页</span>
      </div>

      <el-form class="filter-form" @submit.prevent="search">
        <el-form-item label="关键词">
          <el-input
            v-model="filters.keyword"
            clearable
            placeholder="操作人或目标用户 ID"
            @keyup.enter="search"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item label="操作类型">
          <el-select v-model="filters.actionCode" clearable placeholder="全部类型">
            <el-option
              v-for="option in actionOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="执行结果">
          <el-select v-model="filters.success" clearable placeholder="全部结果">
            <el-option label="执行成功" :value="1" />
            <el-option label="执行失败" :value="0" />
          </el-select>
        </el-form-item>

        <el-form-item label="时间范围" class="date-filter">
          <el-date-picker
            v-model="filters.timeRange"
            type="datetimerange"
            value-format="YYYY-MM-DDTHH:mm:ss"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            :shortcuts="dateShortcuts"
            unlink-panels
          />
        </el-form-item>

        <div class="filter-actions">
          <el-button type="primary" :loading="loading" native-type="submit">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button :disabled="loading" @click="resetFilters">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </div>
      </el-form>
    </section>

    <section class="table-panel">
      <div v-if="errorMessage" class="inline-error">
        <div>
          <strong>审计记录加载失败</strong>
          <p>{{ errorMessage }}</p>
        </div>
        <el-button type="primary" plain @click="loadAuditLogs">重新加载</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="logs"
        row-key="id"
        :row-class-name="rowClassName"
        class="audit-table"
        empty-text="当前条件下没有操作记录"
      >
        <el-table-column label="操作时间" width="176">
          <template #default="{ row }">
            <span class="time-cell">{{ formatDateTime(row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作人" min-width="150">
          <template #default="{ row }">
            <div class="operator-cell">
              <span class="operator-avatar">{{ operatorInitial(row.operatorName) }}</span>
              <span>
                <strong>{{ row.operatorName || '未知操作人' }}</strong>
                <small>ID {{ row.operatorId }}</small>
              </span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="动作" width="120">
          <template #default="{ row }">
            <el-tag
              :type="actionTagType(row.actionCode)"
              effect="plain"
              size="small"
              class="action-tag"
            >
              {{ actionLabel(row.actionCode) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="目标" width="130">
          <template #default="{ row }">
            <span class="target-cell">{{ targetLabel(row.targetType, row.targetId) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="请求" min-width="240">
          <template #default="{ row }">
            <div class="request-cell">
              <span class="method-chip">{{ row.requestMethod || '—' }}</span>
              <span class="request-path" :title="row.requestPath">{{ row.requestPath || '—' }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="requestIp" label="来源 IP" width="130">
          <template #default="{ row }">
            <span class="ip-cell">{{ row.requestIp || '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="结果" width="98">
          <template #default="{ row }">
            <span class="result-cell" :class="{ failed: row.success !== 1 }">
              <el-icon>
                <CircleCheck v-if="row.success === 1" />
                <CircleClose v-else />
              </el-icon>
              {{ row.success === 1 ? '成功' : '失败' }}
            </span>
          </template>
        </el-table-column>

        <el-table-column prop="detail" label="说明" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="detail-cell">{{ row.detail || '—' }}</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          :disabled="loading"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="loadAuditLogs"
          @size-change="handleSizeChange"
        />
      </div>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  CircleCheck,
  CircleClose,
  Clock,
  Lock,
  Refresh,
  Search
} from '@element-plus/icons-vue'
import { adminApi } from '../../api'

const logs = ref([])
const loading = ref(false)
const errorMessage = ref('')
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)

const filters = reactive({
  keyword: '',
  actionCode: '',
  success: '',
  timeRange: []
})

const actionOptions = [
  { label: '禁用用户', value: 'USER_DISABLE' },
  { label: '启用用户', value: 'USER_ENABLE' },
  { label: '删除用户', value: 'USER_DELETE' }
]

const failedOnPage = computed(
  () => logs.value.filter(log => log.success !== 1).length
)

const latestTime = computed(() => {
  if (!logs.value.length) return '暂无记录'
  return formatDateTime(logs.value[0].createTime)
})

const dateShortcuts = [
  {
    text: '最近 24 小时',
    value: () => {
      const end = new Date()
      const start = new Date(end.getTime() - 24 * 60 * 60 * 1000)
      return [start, end]
    }
  },
  {
    text: '最近 7 天',
    value: () => {
      const end = new Date()
      const start = new Date(end.getTime() - 7 * 24 * 60 * 60 * 1000)
      return [start, end]
    }
  },
  {
    text: '最近 30 天',
    value: () => {
      const end = new Date()
      const start = new Date(end.getTime() - 30 * 24 * 60 * 60 * 1000)
      return [start, end]
    }
  }
]

const actionLabel = code => {
  return actionOptions.find(option => option.value === code)?.label || code || '未知操作'
}

const actionTagType = code => {
  if (code === 'USER_DELETE') return 'danger'
  if (code === 'USER_DISABLE') return 'warning'
  return 'success'
}

const operatorInitial = name => (name || '?').charAt(0).toUpperCase()

const targetLabel = (type, id) => {
  const typeLabel = type === 'USER' ? '用户' : type || '目标'
  return `${typeLabel} #${id ?? '—'}`
}

const formatNumber = value => {
  return new Intl.NumberFormat('zh-CN').format(Number(value) || 0)
}

const formatDateTime = value => {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value).replace('T', ' ')
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  }).format(date).replace(/\//g, '-')
}

const rowClassName = ({ row }) => (row.success === 1 ? '' : 'failed-row')

const loadAuditLogs = async () => {
  loading.value = true
  errorMessage.value = ''

  try {
    const response = await adminApi.auditList({
      page: page.value,
      size: pageSize.value,
      keyword: filters.keyword.trim() || undefined,
      actionCode: filters.actionCode || undefined,
      success: filters.success === '' ? undefined : filters.success,
      startTime: filters.timeRange?.[0] || undefined,
      endTime: filters.timeRange?.[1] || undefined
    })

    if (response.data.code !== 200) {
      throw new Error(response.data.msg || '审计记录加载失败')
    }

    const data = response.data.data || {}
    logs.value = data.records || []
    total.value = Number(data.total) || 0
  } catch (error) {
    logs.value = []
    total.value = 0
    errorMessage.value =
      error?.response?.data?.msg || error?.message || '请稍后重新加载'
  } finally {
    loading.value = false
  }
}

const search = () => {
  page.value = 1
  loadAuditLogs()
}

const resetFilters = () => {
  filters.keyword = ''
  filters.actionCode = ''
  filters.success = ''
  filters.timeRange = []
  page.value = 1
  loadAuditLogs()
}

const handleSizeChange = () => {
  page.value = 1
  loadAuditLogs()
}

onMounted(loadAuditLogs)
</script>

<style scoped>
.audit-page {
  color: var(--text-primary);
}

.page-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 28px;
  margin-bottom: 22px;
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
  font-size: clamp(30px, 4vw, 44px);
  font-weight: 600;
  letter-spacing: -1.3px;
  line-height: 1.08;
}

.intro-copy {
  max-width: 640px;
  margin: 11px 0 0;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.8;
  text-wrap: pretty;
}

.heading-meta {
  display: flex;
  flex-shrink: 0;
  gap: 8px;
}

.heading-meta span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 9px;
  font-family: var(--font-mono);
  font-size: 9px;
  color: var(--text-muted);
  background: rgba(17, 24, 39, 0.56);
  border: 1px solid rgba(51, 65, 85, 0.4);
  border-radius: 6px;
}

.summary-strip {
  display: grid;
  grid-template-columns: 0.72fr 0.72fr 1.56fr;
  margin-bottom: 14px;
  overflow: hidden;
  background: linear-gradient(145deg, rgba(20, 29, 44, 0.82), rgba(13, 19, 30, 0.72));
  border: 1px solid rgba(51, 65, 85, 0.38);
  border-radius: 13px;
}

.summary-item {
  display: flex;
  min-height: 86px;
  flex-direction: column;
  justify-content: center;
  gap: 7px;
  padding: 16px 20px;
  border-right: 1px solid rgba(51, 65, 85, 0.3);
}

.summary-item:last-child {
  border-right: 0;
}

.summary-item span {
  color: var(--text-muted);
  font-size: 10px;
}

.summary-item strong {
  font-family: var(--font-mono);
  font-size: 25px;
  font-variant-numeric: tabular-nums;
  font-weight: 500;
  letter-spacing: -0.5px;
}

.summary-item strong.danger {
  color: #f59aa8;
}

.summary-item .time-value {
  font-size: 15px;
  letter-spacing: 0;
  color: #c5d4e5;
}

.filter-panel,
.table-panel {
  background: linear-gradient(145deg, rgba(20, 29, 44, 0.82), rgba(13, 19, 30, 0.72));
  border: 1px solid rgba(51, 65, 85, 0.38);
  border-radius: 14px;
}

.filter-panel {
  padding: 18px 20px 2px;
  margin-bottom: 14px;
}

.filter-title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.filter-title h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
}

.record-count {
  color: var(--text-muted);
  font-family: var(--font-mono);
  font-size: 9px;
}

.filter-form {
  display: grid;
  grid-template-columns: minmax(180px, 1.1fr) minmax(150px, 0.7fr) minmax(140px, 0.6fr) minmax(340px, 1.5fr) auto;
  align-items: end;
  gap: 10px;
  margin-top: 16px;
}

.filter-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.filter-form :deep(.el-form-item__label) {
  padding-bottom: 5px;
  color: var(--text-muted);
  font-size: 10px;
  line-height: 1.4;
}

.filter-form :deep(.el-select),
.filter-form :deep(.el-date-editor) {
  width: 100%;
}

.filter-actions {
  display: flex;
  gap: 8px;
  padding-bottom: 16px;
}

.table-panel {
  padding: 18px;
  overflow: hidden;
}

.inline-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 14px;
  padding: 13px 14px;
  background: rgba(127, 29, 29, 0.12);
  border: 1px solid rgba(248, 113, 113, 0.18);
  border-radius: 9px;
}

.inline-error strong {
  font-size: 12px;
  font-weight: 500;
}

.inline-error p {
  margin: 4px 0 0;
  color: var(--text-muted);
  font-size: 10px;
}

.audit-table {
  width: 100%;
}

.time-cell,
.ip-cell {
  font-family: var(--font-mono);
  font-size: 10px;
  font-variant-numeric: tabular-nums;
  color: #a9b8ca;
}

.operator-cell {
  display: flex;
  align-items: center;
  gap: 9px;
}

.operator-avatar {
  display: grid;
  width: 27px;
  height: 27px;
  flex-shrink: 0;
  place-items: center;
  color: #bdefff;
  font-family: var(--font-mono);
  font-size: 10px;
  background: rgba(0, 212, 255, 0.08);
  border: 1px solid rgba(0, 212, 255, 0.13);
  border-radius: 7px;
}

.operator-cell > span:last-child {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.operator-cell strong {
  font-size: 11px;
  font-weight: 500;
}

.operator-cell small {
  color: var(--text-muted);
  font-family: var(--font-mono);
  font-size: 8px;
}

.action-tag {
  font-family: var(--font-mono);
  font-size: 9px;
  letter-spacing: 0.2px;
}

.target-cell {
  color: #a9dff1;
  font-family: var(--font-mono);
  font-size: 10px;
}

.request-cell {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.method-chip {
  min-width: 44px;
  flex-shrink: 0;
  padding: 3px 5px;
  color: #8fc5d8;
  font-family: var(--font-mono);
  font-size: 8px;
  text-align: center;
  background: rgba(0, 212, 255, 0.05);
  border: 1px solid rgba(0, 212, 255, 0.11);
  border-radius: 4px;
}

.request-path {
  overflow: hidden;
  color: var(--text-muted);
  font-family: var(--font-mono);
  font-size: 9px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-cell {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: #83ddba;
  font-size: 10px;
}

.result-cell.failed {
  color: #f59aa8;
}

.detail-cell {
  color: #a9b8ca;
  font-size: 10px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

:deep(.failed-row > td.el-table__cell) {
  background: rgba(127, 29, 29, 0.055) !important;
}

@media (max-width: 1180px) {
  .filter-form {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .date-filter {
    grid-column: span 2;
  }
}

@media (max-width: 760px) {
  .page-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .summary-strip {
    grid-template-columns: repeat(2, 1fr);
  }

  .summary-wide {
    grid-column: span 2;
    border-top: 1px solid rgba(51, 65, 85, 0.3);
  }

  .filter-form {
    grid-template-columns: 1fr;
  }

  .date-filter {
    grid-column: auto;
  }

  .filter-actions {
    justify-content: flex-start;
  }

  .pagination-wrap {
    justify-content: flex-start;
    overflow-x: auto;
  }
}
</style>
