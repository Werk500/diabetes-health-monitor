<template>
  <div class="record-page">
    <!-- 表单卡片 -->
    <el-card shadow="hover" class="form-card">
      <template #header>
        <div class="card-title">
          <el-icon color="#f56c6c"><Sugar /></el-icon>
          <span>新增血糖记录</span>
        </div>
      </template>
      <el-form :model="form" inline>
        <el-form-item label="血糖值(mmol/L)">
          <el-input-number v-model="form.bloodSugar" :precision="1" :min="1" :max="33" controls-position="right" />
        </el-form-item>
        <el-form-item label="测量类型">
          <el-select v-model="form.measureType">
            <el-option :value="1" label="空腹" /><el-option :value="2" label="餐前" />
            <el-option :value="3" label="餐后2h" /><el-option :value="4" label="睡前" />
            <el-option :value="5" label="凌晨" />
          </el-select>
        </el-form-item>
        <el-form-item label="测量时间">
          <el-date-picker v-model="form.measureTime" type="datetime" placeholder="选择时间" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item>
          <el-button type="danger" @click="submit">提 交 记 录</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 历史记录表格 -->
    <el-card shadow="hover" class="table-card" v-loading="loading">
      <template #header>
        <div class="card-title">
          <el-icon color="#409eff"><List /></el-icon>
          <span>历史记录</span>
        </div>
      </template>
      <el-table :data="records" stripe style="width:100%">
        <el-table-column prop="measureTime" label="测量时间" width="180" />
        <el-table-column prop="bloodSugar" label="血糖值(mmol/L)" width="150">
          <template #default="scope">
            <el-tag :type="sugarTagType(scope.row.bloodSugar)" effect="dark" size="large">
              {{ scope.row.bloodSugar }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="100">
          <template #default="scope">{{ ['','空腹','餐前','餐后2h','睡前','凌晨'][scope.row.measureType] }}</template>
        </el-table-column>
        <el-table-column label="备注" min-width="120">
          <template #default="scope">{{ scope.row.note || '--' }}</template>
        </el-table-column>
        <el-table-column label="数据来源" width="100">
          <template #default="scope">
            <el-tag v-if="scope.row._demo" type="info" size="small">示例</el-tag>
            <el-tag v-else type="success" size="small">真实</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="scope">
            <el-button :type="scope.row._demo ? 'warning' : 'danger'" size="small" @click="del(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Watermelon, List } from '@element-plus/icons-vue'
import { recordApi } from '../api'
import { ElMessage } from 'element-plus'
import { demoBloodSugarRecords } from '../mock/demoData'
import { getData } from '../mock/demoManager'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const userInfo = userStore.userInfo
const records = ref([])
const loading = ref(false)
const form = reactive({ userId: userInfo.id, bloodSugar: null, measureType: 1, measureTime: null })

const sugarTagType = (v) => {
  if (v < 3.9) return 'warning'
  if (v <= 7.0) return 'success'
  if (v <= 10.0) return 'warning'
  return 'danger'
}

const load = async () => {
  loading.value = true
  try {
    const res = await recordApi.bloodSugarList(userInfo.id, {})
    const real = (res.data.code === 200) ? (res.data.data || []) : []
    real.forEach(r => r._demo = false)
    const demo = demoBloodSugarRecords.map(d => ({ ...d, _demo: true }))
    records.value = getData(demo, real)
  } catch {
    records.value = getData(demoBloodSugarRecords.map(d => ({ ...d, _demo: true })), [])
  } finally { loading.value = false }
}

const submit = async () => {
  const res = await recordApi.addBloodSugar(form)
  if (res.data.code === 200) { ElMessage.success('记录成功'); load() }
}

const del = async (row) => {
  if (row._demo) { ElMessage.warning('示例数据仅供展示，无法删除'); return }
  await recordApi.deleteBloodSugar(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.record-page { max-width: 1200px; }
.form-card { margin-bottom: 16px; }
.table-card { margin-bottom: 16px; }
.card-title { display: flex; align-items: center; gap: 8px; font-size: 16px; font-weight: 600; }
</style>