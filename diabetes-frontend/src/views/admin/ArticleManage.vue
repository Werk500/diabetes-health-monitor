<template>
  <section class="article-page">
    <header class="page-heading">
      <div>
        <p class="eyebrow">CONTENT / OPERATIONS</p>
        <h1>内容运营</h1>
        <p class="intro-copy">
          管理健康文章内容、发布状态与用户触达。重复推送只会补充尚未收到文章的用户。
        </p>
      </div>
      <el-button type="primary" size="large" @click="openAdd">
        <el-icon><Plus /></el-icon>
        新增文章
      </el-button>
    </header>

    <div class="summary-strip">
      <div class="summary-item">
        <span>筛选结果</span>
        <strong>{{ formatNumber(total) }}</strong>
      </div>
      <div class="summary-item">
        <span>本页已推送</span>
        <strong>{{ currentPagePushed }}</strong>
      </div>
      <div class="summary-item">
        <span>本页启用</span>
        <strong>{{ currentPageEnabled }}</strong>
      </div>
    </div>

    <section class="filter-panel">
      <div class="filter-title">
        <div>
          <p class="panel-kicker">ARTICLE FILTER</p>
          <h2>筛选文章</h2>
        </div>
        <span class="record-count">当前第 {{ page }} 页</span>
      </div>

      <el-form class="filter-form" @submit.prevent="search">
        <el-form-item label="关键词">
          <el-input
            v-model="filters.keyword"
            clearable
            placeholder="搜索标题、摘要或作者"
            @keyup.enter="search"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item label="文章分类">
          <el-select v-model="filters.category" clearable placeholder="全部分类">
            <el-option
              v-for="option in categoryOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="启用状态">
          <el-select v-model="filters.status" clearable placeholder="全部状态">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>

        <el-form-item label="推送状态">
          <el-select v-model="filters.pushStatus" clearable placeholder="全部状态">
            <el-option label="已推送" :value="1" />
            <el-option label="未推送" :value="0" />
          </el-select>
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
          <strong>文章列表加载失败</strong>
          <p>{{ errorMessage }}</p>
        </div>
        <el-button type="primary" plain @click="loadArticles">重新加载</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="articles"
        row-key="id"
        :row-class-name="rowClassName"
        class="article-table"
        empty-text="当前条件下没有文章"
      >
        <el-table-column label="文章" min-width="280">
          <template #default="{ row }">
            <div class="article-cell">
              <strong>{{ row.title }}</strong>
              <small>{{ row.summary || '暂无摘要' }}</small>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="分类" width="118">
          <template #default="{ row }">
            <el-tag type="info" effect="plain" size="small">
              {{ categoryLabel(row.category) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="author" label="作者" width="110">
          <template #default="{ row }">{{ row.author || '未填写' }}</template>
        </el-table-column>

        <el-table-column label="启用" width="86">
          <template #default="{ row }">
            <span class="state-cell" :class="{ inactive: row.status !== 1 }">
              <span class="state-dot"></span>
              {{ row.status === 1 ? '启用' : '停用' }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="推送" width="96">
          <template #default="{ row }">
            <span class="push-state" :class="{ pushed: row.pushStatus === 1 }">
              {{ row.pushStatus === 1 ? '已推送' : '未推送' }}
            </span>
          </template>
        </el-table-column>

        <el-table-column prop="viewCount" label="浏览" width="80">
          <template #default="{ row }">
            <span class="number-cell">{{ formatNumber(row.viewCount) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="更新时间" width="174">
          <template #default="{ row }">
            <span class="time-cell">{{ formatDateTime(row.updateTime || row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="258" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button size="small" text type="primary" @click="openEdit(row)">
                <el-icon><EditPen /></el-icon>
                编辑
              </el-button>
              <el-button
                size="small"
                text
                type="success"
                :loading="pushLoadingId === row.id"
                :disabled="row.status !== 1"
                :title="row.status !== 1 ? '停用文章不能推送' : ''"
                @click="push(row)"
              >
                <el-icon><Promotion /></el-icon>
                {{ row.pushStatus === 1 ? '再次推送' : '推送' }}
              </el-button>
              <el-button size="small" text type="danger" @click="remove(row)">
                <el-icon><Delete /></el-icon>
                删除
              </el-button>
            </div>
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
          @current-change="loadArticles"
          @size-change="handleSizeChange"
        />
      </div>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑文章' : '新增文章'"
      width="min(720px, 94vw)"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-position="top"
        class="article-form"
      >
        <el-form-item label="标题" prop="title">
          <el-input
            v-model="form.title"
            maxlength="100"
            show-word-limit
            placeholder="输入文章标题"
          />
        </el-form-item>

        <div class="form-grid">
          <el-form-item label="分类" prop="category">
            <el-select v-model="form.category" placeholder="选择分类">
              <el-option
                v-for="option in categoryOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="作者" prop="author">
            <el-input v-model="form.author" maxlength="50" placeholder="营养师、医师或编辑" />
          </el-form-item>

          <el-form-item label="启用状态" prop="status">
            <el-select v-model="form.status">
              <el-option label="启用" :value="1" />
              <el-option label="停用" :value="0" />
            </el-select>
          </el-form-item>
        </div>

        <el-form-item label="摘要" prop="summary">
          <el-input
            v-model="form.summary"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="用于文章列表展示的简短说明"
          />
        </el-form-item>

        <el-form-item label="封面图片" prop="coverImage">
          <el-input v-model="form.coverImage" placeholder="可选，填写图片 URL" />
        </el-form-item>

        <el-form-item label="正文" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="9"
            placeholder="输入文章正文"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">
          {{ isEdit ? '保存修改' : '创建文章' }}
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import {
  Delete,
  EditPen,
  Plus,
  Promotion,
  Refresh,
  Search
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '../../api'

const articles = ref([])
const loading = ref(false)
const errorMessage = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const pushLoadingId = ref(null)
const formRef = ref(null)

const filters = reactive({
  keyword: '',
  category: '',
  status: '',
  pushStatus: ''
})

const createEmptyForm = () => ({
  id: null,
  title: '',
  summary: '',
  category: 1,
  coverImage: '',
  author: '',
  status: 1,
  content: ''
})

const form = ref(createEmptyForm())

const categoryOptions = [
  { label: '血糖监测', value: 1 },
  { label: '控糖饮食', value: 2 },
  { label: '并发症预防', value: 3 },
  { label: '运动建议', value: 4 }
]

const formRules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { max: 100, message: '标题不能超过 100 个字符', trigger: 'blur' }
  ],
  summary: [
    { required: true, message: '请输入摘要', trigger: 'blur' },
    { max: 500, message: '摘要不能超过 500 个字符', trigger: 'blur' }
  ],
  content: [{ required: true, message: '请输入文章正文', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  author: [
    { required: true, message: '请输入作者', trigger: 'blur' },
    { max: 50, message: '作者不能超过 50 个字符', trigger: 'blur' }
  ],
  status: [{ required: true, message: '请选择启用状态', trigger: 'change' }]
}

const currentPagePushed = computed(
  () => articles.value.filter(article => article.pushStatus === 1).length
)

const currentPageEnabled = computed(
  () => articles.value.filter(article => article.status === 1).length
)

const categoryLabel = category => {
  return categoryOptions.find(option => option.value === category)?.label || '未分类'
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
    hour12: false
  }).format(date).replace(/\//g, '-')
}

const rowClassName = ({ row }) => (row.status === 1 ? '' : 'inactive-row')

const loadArticles = async () => {
  loading.value = true
  errorMessage.value = ''

  try {
    const response = await adminApi.articleList({
      page: page.value,
      size: pageSize.value,
      keyword: filters.keyword.trim() || undefined,
      category: filters.category === '' ? undefined : filters.category,
      status: filters.status === '' ? undefined : filters.status,
      pushStatus: filters.pushStatus === '' ? undefined : filters.pushStatus
    })

    if (response.data.code !== 200) {
      throw new Error(response.data.msg || '文章列表加载失败')
    }

    const data = response.data.data || {}
    articles.value = data.records || []
    total.value = Number(data.total) || 0
  } catch (error) {
    articles.value = []
    total.value = 0
    errorMessage.value =
      error?.response?.data?.msg || error?.message || '请稍后重新加载'
  } finally {
    loading.value = false
  }
}

const search = () => {
  page.value = 1
  loadArticles()
}

const resetFilters = () => {
  filters.keyword = ''
  filters.category = ''
  filters.status = ''
  filters.pushStatus = ''
  page.value = 1
  loadArticles()
}

const handleSizeChange = () => {
  page.value = 1
  loadArticles()
}

const openAdd = async () => {
  isEdit.value = false
  form.value = createEmptyForm()
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

const openEdit = async row => {
  isEdit.value = true
  form.value = {
    id: row.id,
    title: row.title || '',
    summary: row.summary || '',
    category: row.category ?? 1,
    coverImage: row.coverImage || '',
    author: row.author || '',
    status: row.status ?? 1,
    content: row.content || ''
  }
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

const buildPayload = () => ({
  title: form.value.title.trim(),
  summary: form.value.summary.trim(),
  content: form.value.content.trim(),
  category: form.value.category,
  coverImage: form.value.coverImage.trim() || null,
  author: form.value.author.trim(),
  status: form.value.status
})

const save = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  saving.value = true

  try {
    const payload = buildPayload()
    if (isEdit.value) {
      await adminApi.updateArticle(form.value.id, payload)
    } else {
      await adminApi.addArticle(payload)
    }

    ElMessage.success(isEdit.value ? '文章已更新' : '文章已创建')
    dialogVisible.value = false
    await loadArticles()
  } catch (error) {
    ElMessage.error(
      error?.response?.data?.msg ||
        error?.message ||
        (isEdit.value ? '文章更新失败' : '文章创建失败')
    )
  } finally {
    saving.value = false
  }
}

const push = async row => {
  const repush = row.pushStatus === 1
  const description = repush
    ? '已收到文章的用户会自动跳过，仅补充新增的启用用户。'
    : '文章会发送给所有启用的普通用户，管理员和禁用用户不会收到。'

  try {
    await ElMessageBox.confirm(
      description,
      `${repush ? '再次推送' : '推送'}「${row.title}」？`,
      {
        type: 'warning',
        confirmButtonText: repush ? '再次推送' : '确认推送',
        cancelButtonText: '取消'
      }
    )

    pushLoadingId.value = row.id
    const response = await adminApi.pushArticle(row.id)
    if (response.data.code !== 200) {
      throw new Error(response.data.msg || '推送失败')
    }

    const result = response.data.data || {}
    ElMessage.success(
      `推送完成：覆盖 ${formatNumber(result.eligibleCount)} 人，新增 ${formatNumber(result.insertedCount)}，跳过 ${formatNumber(result.skippedCount)}`
    )
    await loadArticles()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(error?.response?.data?.msg || error?.message || '推送失败')
  } finally {
    pushLoadingId.value = null
  }
}

const remove = async row => {
  try {
    await ElMessageBox.confirm(
      '文章和用户推送关系会一起删除，此操作不可恢复。',
      `删除「${row.title}」？`,
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消'
      }
    )

    await adminApi.deleteArticle(row.id)
    ElMessage.success('文章已删除')
    await loadArticles()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(error?.response?.data?.msg || error?.message || '删除失败')
  }
}

onMounted(loadArticles)
</script>

<style scoped>
.article-page {
  color: var(--text-primary);
}

.page-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
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
  font-size: clamp(30px, 4vw, 44px);
  font-weight: 600;
  letter-spacing: -1.3px;
  line-height: 1.08;
}

.intro-copy {
  max-width: 650px;
  margin: 11px 0 0;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.8;
  text-wrap: pretty;
}

.summary-strip {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  margin-bottom: 14px;
  overflow: hidden;
  background: linear-gradient(145deg, rgba(20, 29, 44, 0.82), rgba(13, 19, 30, 0.72));
  border: 1px solid rgba(51, 65, 85, 0.38);
  border-radius: 13px;
}

.summary-item {
  display: flex;
  min-height: 82px;
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
  font-size: 24px;
  font-variant-numeric: tabular-nums;
  font-weight: 500;
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
  grid-template-columns: minmax(210px, 1.3fr) repeat(3, minmax(145px, 0.7fr)) auto;
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

.filter-form :deep(.el-select) {
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

.article-table {
  width: 100%;
}

.article-cell {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 5px;
}

.article-cell strong,
.article-cell small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.article-cell strong {
  color: #dcecf8;
  font-size: 12px;
  font-weight: 500;
}

.article-cell small {
  color: var(--text-muted);
  font-size: 9px;
}

.state-cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #83ddba;
  font-size: 10px;
}

.state-cell.inactive {
  color: #8590a1;
}

.state-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  box-shadow: 0 0 7px currentColor;
}

.push-state {
  color: var(--text-muted);
  font-size: 10px;
}

.push-state.pushed {
  color: var(--neon-blue);
}

.number-cell,
.time-cell {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  color: #a9b8ca;
  font-size: 10px;
}

.row-actions {
  display: flex;
  align-items: center;
  gap: 2px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.form-grid :deep(.el-select) {
  width: 100%;
}

.article-form :deep(.el-form-item__label) {
  color: var(--text-secondary);
  font-size: 11px;
}

:deep(.inactive-row > td.el-table__cell) {
  opacity: 0.62;
}

@media (max-width: 1180px) {
  .filter-form {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .page-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .summary-strip,
  .filter-form,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .summary-item {
    border-right: 0;
    border-bottom: 1px solid rgba(51, 65, 85, 0.3);
  }

  .summary-item:last-child {
    border-bottom: 0;
  }

  .pagination-wrap {
    justify-content: flex-start;
    overflow-x: auto;
  }
}
</style>
