<template>
  <div class="export-page">
    <!-- 提交区 -->
    <el-card shadow="hover" class="section-card">
      <template #header>
        <div class="card-title">
          <el-icon color="#00d4ff"><Files /></el-icon>
          <span>新建导出任务</span>
          <span class="push-right muted">后台生成，可以离开本页</span>
        </div>
      </template>

      <div class="submit-row">
        <el-radio-group v-model="form.type">
          <el-radio-button value="EXCEL">Excel 明细</el-radio-button>
          <el-radio-button value="PDF">PDF 报告</el-radio-button>
        </el-radio-group>

        <el-date-picker
          v-model="form.range"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          :disabled-date="(d) => d.getTime() > Date.now()"
        />

        <el-button type="primary" :loading="submitting" @click="submit">生成导出文件</el-button>
      </div>

      <div class="muted submit-hint">
        不选日期 = 导出全部（单类最多 1000 条）；Excel 按四类记录分 Sheet，PDF 为健康报告。
      </div>
    </el-card>

    <!-- 任务列表 -->
    <el-card shadow="hover" class="section-card" v-loading="loading">
      <template #header>
        <div class="card-title">
          <el-icon color="#a855f7"><List /></el-icon>
          <span>我的导出任务</span>
          <span class="overview-meta push-right">
            共 {{ total }} 个 · 进行中 {{ activeCount }} · 失败 {{ failedCount }}
          </span>
          <el-button link type="primary" @click="loadTasks">刷新</el-button>
        </div>
      </template>

      <el-table :data="tasks" stripe style="width: 100%" empty-text="还没有导出任务">
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="row.type === 'PDF' ? 'warning' : 'primary'" effect="plain">
              {{ row.type === 'PDF' ? 'PDF 报告' : 'Excel 明细' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="状态" min-width="240">
          <template #default="{ row }">
            <div class="status-cell">
              <el-tag :type="statusType(row.status)" effect="dark" size="small">
                {{ statusText(row.status) }}
              </el-tag>
              <el-progress
                v-if="row.status === 'RUNNING'"
                class="progress"
                :percentage="row.progress || 0"
                :stroke-width="6"
                :show-text="false"
              />
              <span v-if="row.status === 'FAILED' && row.errorMsg" class="err-text" :title="row.errorMsg">
                {{ row.errorMsg }}
              </span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="文件大小" width="110">
          <template #default="{ row }">{{ row.fileSize ? formatSize(row.fileSize) : '--' }}</template>
        </el-table-column>

        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>

        <el-table-column label="耗时" width="100">
          <template #default="{ row }">{{ cost(row) }}</template>
        </el-table-column>

        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button v-if="row.status === 'SUCCESS'" link type="primary" @click="download(row)">下载</el-button>
            <el-button v-else-if="row.status === 'FAILED'" link type="warning" @click="retry(row)">重试</el-button>
            <span v-else class="muted">处理中…</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          layout="prev, pager, next, total"
          :total="total"
          :page-size="query.size"
          :current-page="query.page"
          @current-change="onPageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { Files, List } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { exportApi } from '../api'

const tasks = ref([])
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const query = reactive({ page: 1, size: 10 })
const form = reactive({ type: 'EXCEL', range: null })

let pollTimer = null

const activeCount = computed(
  () => tasks.value.filter((t) => t.status === 'PENDING' || t.status === 'RUNNING').length
)
const failedCount = computed(() => tasks.value.filter((t) => t.status === 'FAILED').length)

const statusText = (s) =>
  ({ PENDING: '排队中', RUNNING: '生成中', SUCCESS: '已完成', FAILED: '失败' }[s] || s || '--')
const statusType = (s) =>
  ({ PENDING: 'info', RUNNING: 'primary', SUCCESS: 'success', FAILED: 'danger' }[s] || 'info')

const formatSize = (bytes) => {
  if (!bytes) return '--'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

// 后端 LocalDateTime 可能是 "2026-10-19T12:00:00" 或 "2026-10-19 12:00:00"，统一成后者
const formatTime = (v) => (v ? String(v).replace('T', ' ').slice(0, 19) : '--')

const cost = (row) => {
  if (!row.startTime || !row.finishTime) return '--'
  const s = (new Date(formatTime(row.finishTime)) - new Date(formatTime(row.startTime))) / 1000
  return Number.isFinite(s) && s >= 0 ? s.toFixed(1) + ' s' : '--'
}

const loadTasks = async () => {
  loading.value = true
  try {
    const res = await exportApi.list({ page: query.page, size: query.size })
    if (res.data.code === 200) {
      tasks.value = res.data.data.records || []
      total.value = res.data.data.total || 0
    }
  } catch {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
    syncPolling()
  }
}

/** 只有还有进行中的任务时才轮询，全部到达终态就停，避免无意义请求 */
const syncPolling = () => {
  if (activeCount.value > 0 && !pollTimer) {
    pollTimer = setInterval(loadTasks, 3000)
  } else if (activeCount.value === 0 && pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

const submit = async () => {
  submitting.value = true
  try {
    const [startDate, endDate] = form.range || []
    const res = await exportApi.submit({ type: form.type, startDate, endDate })
    if (res.data.code === 200) {
      ElMessage.success('已提交，正在后台生成')
      query.page = 1
      await loadTasks()
    }
  } finally {
    submitting.value = false
  }
}

const download = async (row) => {
  try {
    const res = await exportApi.download(row.id)
    const url = URL.createObjectURL(new Blob([res.data]))
    const a = document.createElement('a')
    a.href = url
    // 优先用后端 Content-Disposition 里的文件名
    const cd = (res.headers && res.headers['content-disposition']) || ''
    const matched = /filename="?([^";]+)"?/.exec(cd)
    a.download = matched ? matched[1] : `export_${row.id}.${row.type === 'PDF' ? 'pdf' : 'xlsx'}`
    document.body.appendChild(a)
    a.click()
    a.remove()
    setTimeout(() => URL.revokeObjectURL(url), 1000)
  } catch {
    ElMessage.error('下载失败，文件可能已过期')
  }
}

const retry = async (row) => {
  const res = await exportApi.retry(row.id)
  if (res.data.code === 200) {
    ElMessage.success('已重新提交')
    await loadTasks()
  }
}

const onPageChange = (page) => {
  query.page = page
  loadTasks()
}

onMounted(loadTasks)
onUnmounted(() => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
})
</script>

<style scoped>
.export-page { max-width: 1200px; }
.section-card { margin-bottom: 16px; }

.card-title { display: flex; align-items: center; gap: 8px; font-size: 16px; font-weight: 600; }
.push-right { margin-left: auto; }

.submit-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}
.submit-hint { margin-top: 12px; }

.muted { color: #64748b; font-size: 12px; }
.overview-meta { font-size: 12px; color: #94a3b8; }

.status-cell { display: flex; align-items: center; gap: 10px; min-width: 0; }
.progress { width: 90px; flex: none; }
.err-text {
  font-size: 12px;
  color: #f56c6c;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 200px;
}

.pager { display: flex; justify-content: flex-end; margin-top: 14px; }
</style>
