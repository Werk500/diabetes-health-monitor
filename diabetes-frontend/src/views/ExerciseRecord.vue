<template>
  <div class="record-page">
    <el-card shadow="hover" class="form-card">
      <template #header>
        <div class="card-title">
          <el-icon color="#67c23a"><Bicycle /></el-icon>
          <span>新增运动记录</span>
        </div>
      </template>
      <el-form :model="form" inline>
        <el-form-item label="运动类型">
          <el-select v-model="form.exerciseTypeId" placeholder="请选择">
            <el-option v-for="t in exerciseTypes" :key="t.id" :value="t.id" :label="t.typeName" />
          </el-select>
        </el-form-item>
        <el-form-item label="时长(分钟)">
          <el-input-number v-model="form.durationMinutes" :min="1" :max="600" controls-position="right" />
        </el-form-item>
        <el-form-item label="消耗热量(kcal)">
          <el-input-number v-model="form.caloriesBurned" :precision="0" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="平均心率">
          <el-input-number v-model="form.heartRateAvg" :min="40" :max="200" controls-position="right" />
        </el-form-item>
        <el-form-item label="运动日期">
          <el-date-picker v-model="form.exerciseDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item>
          <el-button type="success" @click="submit">提 交 记 录</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover" class="table-card" v-loading="loading">
      <template #header>
        <div class="card-title">
          <el-icon color="#409eff"><List /></el-icon>
          <span>运动记录</span>
        </div>
      </template>
      <el-table :data="records" stripe>
        <el-table-column prop="exerciseDate" label="日期" width="120" />
        <el-table-column label="运动类型" width="130">
          <template #default="scope">
            <el-tag effect="plain">{{ getTypeName(scope.row.exerciseTypeId) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="durationMinutes" label="时长(分钟)" width="110" />
        <el-table-column prop="caloriesBurned" label="消耗热量(kcal)" width="130" />
        <el-table-column prop="heartRateAvg" label="平均心率" width="100" />
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
import { Bicycle, List } from '@element-plus/icons-vue'
import { recordApi } from '../api'
import { ElMessage } from 'element-plus'
import { demoExerciseRecords, demoExerciseTypes } from '../mock/demoData'
import { getData } from '../mock/demoManager'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const userInfo = userStore.userInfo
const records = ref([])
const exerciseTypes = ref([])
const loading = ref(false)
const form = reactive({
  userId: userInfo.id, exerciseTypeId: null, durationMinutes: null,
  caloriesBurned: null, heartRateAvg: null, exerciseDate: null
})

const getTypeName = (id) => {
  const t = exerciseTypes.value.find(e => e.id === id)
  return t?.typeName || '未知类型'
}

const load = async () => {
  loading.value = true
  try {
    const [r1, r2] = await Promise.all([
      recordApi.exerciseList(userInfo.id, {}),
      recordApi.exerciseTypes()
    ])
    const real = (r1.data.code === 200) ? (r1.data.data || []) : []
    const realTypes = (r2.data.code === 200) ? (r2.data.data || []) : []
    real.forEach(r => r._demo = false)
    const demo = demoExerciseRecords.map(d => ({ ...d, _demo: true }))
    records.value = getData(demo, real)
    exerciseTypes.value = getData(demoExerciseTypes.map(d => ({ ...d, _demo: true })), realTypes)
  } catch {
    records.value = getData(demoExerciseRecords.map(d => ({ ...d, _demo: true })), [])
    exerciseTypes.value = getData(demoExerciseTypes.map(d => ({ ...d, _demo: true })), [])
  } finally { loading.value = false }
}

const submit = async () => {
  const res = await recordApi.addExercise(form)
  if (res.data.code === 200) { ElMessage.success('记录成功'); load() }
}

const del = async (row) => {
  if (row._demo) { ElMessage.warning('示例数据仅供展示，无法删除'); return }
  await recordApi.deleteExercise(row.id)
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