<template>
  <div class="record-page">
    <el-card shadow="hover" class="form-card">
      <template #header>
        <div class="card-title">
          <el-icon color="#409eff"><Monitor /></el-icon>
          <span>新增身体指标记录</span>
        </div>
      </template>
      <el-form :model="form" inline>
        <el-form-item label="体重(kg)">
          <el-input-number v-model="form.weight" :precision="1" :min="30" :max="200" controls-position="right" />
        </el-form-item>
        <el-form-item label="体脂率(%)">
          <el-input-number v-model="form.bodyFat" :precision="1" :min="1" :max="60" controls-position="right" />
        </el-form-item>
        <el-form-item label="收缩压">
          <el-input-number v-model="form.systolicPressure" :min="60" :max="250" controls-position="right" />
        </el-form-item>
        <el-form-item label="舒张压">
          <el-input-number v-model="form.diastolicPressure" :min="30" :max="150" controls-position="right" />
        </el-form-item>
        <el-form-item label="心率">
          <el-input-number v-model="form.heartRate" :min="30" :max="220" controls-position="right" />
        </el-form-item>
        <el-form-item label="腰围(cm)">
          <el-input-number v-model="form.waistline" :precision="1" :min="50" :max="150" controls-position="right" />
        </el-form-item>
        <el-form-item label="记录日期">
          <el-date-picker v-model="form.recordDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submit">提 交 记 录</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover" class="table-card" v-loading="loading">
      <template #header>
        <div class="card-title">
          <el-icon color="#409eff"><List /></el-icon>
          <span>历史记录</span>
        </div>
      </template>
      <el-table :data="records" stripe>
        <el-table-column prop="recordDate" label="日期" width="120" />
        <el-table-column prop="weight" label="体重(kg)" width="100" />
        <el-table-column label="BMI" width="80">
          <template #default="scope">
            <el-tag :type="bmiTag(scope.row.bmi)" size="small">{{ scope.row.bmi }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="bodyFat" label="体脂率(%)" width="100" />
        <el-table-column label="血压" width="130">
          <template #default="scope">{{ scope.row.systolicPressure }}/{{ scope.row.diastolicPressure }} mmHg</template>
        </el-table-column>
        <el-table-column prop="heartRate" label="心率" width="80" />
        <el-table-column prop="waistline" label="腰围(cm)" width="100" />
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
import { Monitor, List } from '@element-plus/icons-vue'
import { recordApi } from '../api'
import { ElMessage } from 'element-plus'
import { demoBodyRecords } from '../mock/demoData'
import { getData } from '../mock/demoManager'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const userInfo = userStore.userInfo
const records = ref([])
const loading = ref(false)
const form = reactive({
  userId: userInfo.id, weight: null, bodyFat: null, systolicPressure: null,
  diastolicPressure: null, heartRate: null, waistline: null, recordDate: null
})

const bmiTag = (v) => {
  if (!v) return 'info'
  if (v < 18.5) return 'warning'
  if (v <= 24) return 'success'
  if (v <= 28) return 'warning'
  return 'danger'
}

const load = async () => {
  loading.value = true
  try {
    const res = await recordApi.bodyList(userInfo.id, {})
    const real = (res.data.code === 200) ? (res.data.data || []) : []
    real.forEach(r => r._demo = false)
    const demo = demoBodyRecords.map(d => ({ ...d, _demo: true }))
    records.value = getData(demo, real)
  } catch {
    records.value = getData(demoBodyRecords.map(d => ({ ...d, _demo: true })), [])
  } finally { loading.value = false }
}

const submit = async () => {
  const res = await recordApi.addBody(form)
  if (res.data.code === 200) { ElMessage.success('记录成功'); load() }
}

const del = async (row) => {
  if (row._demo) { ElMessage.warning('示例数据仅供展示，无法删除'); return }
  await recordApi.deleteBody(row.id)
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