<template>
  <section class="exercise-page">
    <header class="page-heading">
      <div>
        <p class="eyebrow">CONFIG / EXERCISE</p>
        <h1>运动配置</h1>
        <p class="intro-copy">
          维护用户可选择的运动类型、热量消耗与适用人群。已有历史记录的类型请使用停用。
        </p>
      </div>
      <el-button type="primary" size="large" @click="openAdd">
        <el-icon><Plus /></el-icon>
        新增类型
      </el-button>
    </header>

    <div class="summary-strip">
      <div class="summary-item">
        <span>筛选结果</span>
        <strong>{{ formatNumber(total) }}</strong>
      </div>
      <div class="summary-item">
        <span>本页启用</span>
        <strong>{{ currentPageEnabled }}</strong>
      </div>
      <div class="summary-item">
        <span>本页高强度</span>
        <strong>{{ currentPageHighIntensity }}</strong>
      </div>
    </div>

    <section class="filter-panel">
      <div class="filter-title">
        <div>
          <p class="panel-kicker">CONFIG FILTER</p>
          <h2>筛选运动类型</h2>
        </div>
        <span class="record-count">当前第 {{ page }} 页</span>
      </div>

      <el-form class="filter-form" @submit.prevent="search">
        <el-form-item label="关键词">
          <el-input
            v-model="filters.keyword"
            clearable
            placeholder="搜索名称或适用人群"
            @keyup.enter="search"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item label="运动强度">
          <el-select v-model="filters.intensity" clearable placeholder="全部强度">
            <el-option
              v-for="option in intensityOptions"
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
          <strong>运动配置加载失败</strong>
          <p>{{ errorMessage }}</p>
        </div>
        <el-button type="primary" plain @click="loadTypes">重新加载</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="types"
        row-key="id"
        :row-class-name="rowClassName"
        class="exercise-table"
        empty-text="当前条件下没有运动类型"
      >
        <el-table-column label="运动类型" min-width="220">
          <template #default="{ row }">
            <div class="type-cell">
              <strong>{{ row.typeName }}</strong>
              <small>{{ row.suitableFor || '未填写适用人群' }}</small>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="每小时消耗" width="124">
          <template #default="{ row }">
            <span class="number-cell">{{ formatCalories(row.caloriesPerHour) }}</span>
            <small class="unit-text">kcal</small>
          </template>
        </el-table-column>

        <el-table-column label="强度" width="92">
          <template #default="{ row }">
            <el-tag
              :type="intensityTagType(row.intensity)"
              effect="plain"
              size="small"
            >
              {{ intensityLabel(row.intensity) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="适用说明" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="description-cell">{{ row.description || '暂无描述' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="94">
          <template #default="{ row }">
            <span class="state-cell" :class="{ inactive: row.status !== 1 }">
              <span class="state-dot"></span>
              {{ row.status === 1 ? '启用' : '停用' }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" width="174">
          <template #default="{ row }">
            <span class="time-cell">{{ formatDateTime(row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button size="small" text type="primary" @click="openEdit(row)">
                <el-icon><EditPen /></el-icon>
                编辑
              </el-button>
              <el-button
                size="small"
                text
                :type="row.status === 1 ? 'warning' : 'success'"
                :loading="statusBusyId === row.id"
                @click="toggleStatus(row)"
              >
                {{ row.status === 1 ? '停用' : '启用' }}
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
          @current-change="loadTypes"
          @size-change="handleSizeChange"
        />
      </div>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑运动类型' : '新增运动类型'"
      width="min(650px, 94vw)"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-position="top"
        class="exercise-form"
      >
        <el-form-item label="运动名称" prop="typeName">
          <el-input
            v-model="form.typeName"
            maxlength="50"
            show-word-limit
            placeholder="例如：室内单车"
          />
        </el-form-item>

        <div class="form-grid">
          <el-form-item label="每小时消耗" prop="caloriesPerHour">
            <el-input-number
              v-model="form.caloriesPerHour"
              :min="0"
              :max="5000"
              :precision="1"
              controls-position="right"
            />
          </el-form-item>

          <el-form-item label="运动强度" prop="intensity">
            <el-select v-model="form.intensity">
              <el-option
                v-for="option in intensityOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="启用状态" prop="status">
            <el-select v-model="form.status">
              <el-option label="启用" :value="1" />
              <el-option label="停用" :value="0" />
            </el-select>
          </el-form-item>
        </div>

        <el-form-item label="适用人群" prop="suitableFor">
          <el-input
            v-model="form.suitableFor"
            maxlength="255"
            show-word-limit
            placeholder="例如：血糖控制较稳定的患者"
          />
        </el-form-item>

        <el-form-item label="配置说明" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="5"
            maxlength="2000"
            show-word-limit
            placeholder="记录运动建议、注意事项或强度说明"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">
          {{ isEdit ? '保存修改' : '创建类型' }}
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { Delete, EditPen, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '../../api'

const types = ref([])
const loading = ref(false)
const errorMessage = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const statusBusyId = ref(null)
const formRef = ref(null)

const filters = reactive({
  keyword: '',
  intensity: '',
  status: ''
})

const intensityOptions = [
  { label: '低强度', value: 1 },
  { label: '中强度', value: 2 },
  { label: '高强度', value: 3 }
]

const createEmptyForm = () => ({
  id: null,
  typeName: '',
  caloriesPerHour: 0,
  intensity: 1,
  suitableFor: '',
  description: '',
  status: 1
})

const form = ref(createEmptyForm())

const formRules = {
  typeName: [
    { required: true, message: '请输入运动名称', trigger: 'blur' },
    { max: 50, message: '运动名称不能超过 50 个字符', trigger: 'blur' }
  ],
  caloriesPerHour: [{ required: true, message: '请输入每小时消耗', trigger: 'change' }],
  intensity: [{ required: true, message: '请选择运动强度', trigger: 'change' }],
  status: [{ required: true, message: '请选择启用状态', trigger: 'change' }]
}

const currentPageEnabled = computed(
  () => types.value.filter(type => type.status === 1).length
)

const currentPageHighIntensity = computed(
  () => types.value.filter(type => type.intensity === 3).length
)

const intensityLabel = intensity => {
  return intensityOptions.find(option => option.value === intensity)?.label || '未设置'
}

const intensityTagType = intensity => {
  if (intensity === 3) return 'danger'
  if (intensity === 2) return 'warning'
  return 'success'
}

const formatCalories = value => {
  const number = Number(value)
  if (!Number.isFinite(number)) return '—'
  return number.toFixed(1)
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

const loadTypes = async () => {
  loading.value = true
  errorMessage.value = ''

  try {
    const response = await adminApi.getExerciseTypes({
      page: page.value,
      size: pageSize.value,
      keyword: filters.keyword.trim() || undefined,
      intensity: filters.intensity === '' ? undefined : filters.intensity,
      status: filters.status === '' ? undefined : filters.status
    })

    if (response.data.code !== 200) {
      throw new Error(response.data.msg || '运动配置加载失败')
    }

    const data = response.data.data || {}
    types.value = data.records || []
    total.value = Number(data.total) || 0
  } catch (error) {
    types.value = []
    total.value = 0
    errorMessage.value =
      error?.response?.data?.msg || error?.message || '请稍后重新加载'
  } finally {
    loading.value = false
  }
}

const search = () => {
  page.value = 1
  loadTypes()
}

const resetFilters = () => {
  filters.keyword = ''
  filters.intensity = ''
  filters.status = ''
  page.value = 1
  loadTypes()
}

const handleSizeChange = () => {
  page.value = 1
  loadTypes()
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
    typeName: row.typeName || '',
    caloriesPerHour: Number(row.caloriesPerHour) || 0,
    intensity: row.intensity ?? 1,
    suitableFor: row.suitableFor || '',
    description: row.description || '',
    status: row.status ?? 1
  }
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

const buildPayload = () => ({
  typeName: form.value.typeName.trim(),
  caloriesPerHour: form.value.caloriesPerHour,
  intensity: form.value.intensity,
  suitableFor: form.value.suitableFor.trim() || null,
  description: form.value.description.trim() || null,
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
      await adminApi.updateExerciseType(form.value.id, payload)
    } else {
      await adminApi.addExerciseType(payload)
    }

    ElMessage.success(isEdit.value ? '运动类型已更新' : '运动类型已创建')
    dialogVisible.value = false
    await loadTypes()
  } catch (error) {
    if (!error?.response) {
      ElMessage.error(error?.message || '运动配置保存失败')
    }
  } finally {
    saving.value = false
  }
}

const toggleStatus = async row => {
  const nextStatus = row.status === 1 ? 0 : 1
  const action = nextStatus === 0 ? '停用' : '启用'

  try {
    await ElMessageBox.confirm(
      nextStatus === 0
        ? '停用后，该类型不再出现在用户新增运动记录的可选项中。'
        : '启用后，用户可以重新选择该运动类型。',
      `${action}「${row.typeName}」？`,
      {
        type: 'warning',
        confirmButtonText: action,
        cancelButtonText: '取消'
      }
    )

    statusBusyId.value = row.id
    await adminApi.updateExerciseTypeStatus(row.id, nextStatus)
    ElMessage.success(`运动类型已${action}`)
    await loadTypes()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    if (!error?.response) {
      ElMessage.error(error?.message || `${action}失败`)
    }
  } finally {
    statusBusyId.value = null
  }
}

const remove = async row => {
  try {
    await ElMessageBox.confirm(
      '只有未被任何运动记录使用的类型才能删除；已有历史记录时请改为停用。',
      `删除「${row.typeName}」？`,
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消'
      }
    )

    await adminApi.deleteExerciseType(row.id)
    ElMessage.success('运动类型已删除')
    await loadTypes()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    if (!error?.response) {
      ElMessage.error(error?.message || '删除失败')
    }
  }
}

onMounted(loadTypes)
</script>

<style scoped>
.exercise-page {
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
  grid-template-columns: minmax(220px, 1.3fr) repeat(2, minmax(150px, 0.7fr)) auto;
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

.exercise-table {
  width: 100%;
}

.type-cell {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 5px;
}

.type-cell strong {
  color: #dcecf8;
  font-size: 12px;
  font-weight: 500;
}

.type-cell small,
.description-cell {
  overflow: hidden;
  color: var(--text-muted);
  font-size: 9px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.number-cell,
.time-cell {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  color: #b8c8d8;
  font-size: 11px;
}

.unit-text {
  margin-left: 4px;
  color: var(--text-muted);
  font-size: 8px;
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

.form-grid :deep(.el-select),
.form-grid :deep(.el-input-number) {
  width: 100%;
}

.exercise-form :deep(.el-form-item__label) {
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
