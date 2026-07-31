<template>
  <div>
    <el-card header="鏂板杩愬姩璁板綍" style="margin-bottom:20px">
      <el-form :model="form" inline>
        <el-form-item label="杩愬姩绫诲瀷">
          <el-select v-model="form.exerciseTypeId">
            <el-option v-for="t in exerciseTypes" :key="t.id" :value="t.id" :label="t.typeName" />
          </el-select>
        </el-form-item>
        <el-form-item label="鏃堕暱(鍒嗛挓)"><el-input-number v-model="form.durationMinutes" :min="1" :max="600" /></el-form-item>
        <el-form-item label="娑堣€楃儹閲?kcal)"><el-input-number v-model="form.caloriesBurned" :precision="0" :min="0" /></el-form-item>
        <el-form-item label="骞冲潎蹇冪巼"><el-input-number v-model="form.heartRateAvg" :min="40" :max="200" /></el-form-item>
        <el-form-item label="杩愬姩鏃ユ湡"><el-date-picker v-model="form.exerciseDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item><el-button type="primary" @click="submit">鎻愪氦璁板綍</el-button></el-form-item>
      </el-form>
    </el-card>

    <el-card header="杩愬姩璁板綍" v-loading="loading">
      <el-table :data="records" stripe>
        <el-table-column prop="exerciseDate" label="鏃ユ湡" width="120" />
        <el-table-column label="杩愬姩绫诲瀷" width="120">
          <template #default="scope">{{ getTypeName(scope.row.exerciseTypeId) }}</template>
        </el-table-column>
        <el-table-column prop="durationMinutes" label="鏃堕暱(鍒嗛挓)" width="110" />
        <el-table-column prop="caloriesBurned" label="娑堣€楃儹閲?kcal)" width="130" />
        <el-table-column prop="heartRateAvg" label="骞冲潎蹇冪巼" width="100" />
        <el-table-column label="鏁版嵁鏉ユ簮" width="90">
          <template #default="scope">
            <el-tag v-if="scope.row._demo" type="info" size="small" effect="plain">绀轰緥</el-tag>
            <el-tag v-else type="success" size="small" effect="plain">鐪熷疄</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="鎿嶄綔" width="80">
          <template #default="scope">
            <el-button :type="scope.row._demo ? 'warning' : 'danger'" size="small" @click="del(scope.row)">鍒犻櫎</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { recordApi } from '../api';
import { ElMessage } from 'element-plus';
import { demoExerciseRecords, demoExerciseTypes } from '../mock/demoData';
import { getData } from '../mock/demoManager';

import { useUserStore } from '../stores/user';
const userStore = useUserStore();
const userInfo = userStore.userInfo;
const records = ref([]);
const exerciseTypes = ref([]);
const loading = ref(false);
const form = reactive({
  userId: userInfo.id, exerciseTypeId: null, durationMinutes: null,
  caloriesBurned: null, heartRateAvg: null, exerciseDate: null
});

const getTypeName = (id) => {
  const t = exerciseTypes.value.find(e => e.id === id);
  return t?.typeName || '鏈煡绫诲瀷';
};

const load = async () => {
  loading.value = true;
  try {
    const [r1, r2] = await Promise.all([
      recordApi.exerciseList(userInfo.id, {}),
      recordApi.exerciseTypes()
    ]);
    const real = (r1.data.code === 200) ? (r1.data.data || []) : [];
    const realTypes = (r2.data.code === 200) ? (r2.data.data || []) : [];
    real.forEach(r => r._demo = false);
    const demo = demoExerciseRecords.map(d => ({ ...d, _demo: true }));
    records.value = getData(demo, real);
    exerciseTypes.value = getData(demoExerciseTypes.map(d => ({ ...d, _demo: true })), realTypes);
  } catch {
    records.value = getData(demoExerciseRecords.map(d => ({ ...d, _demo: true })), []);
    exerciseTypes.value = getData(demoExerciseTypes.map(d => ({ ...d, _demo: true })), []);
  } finally {
    loading.value = false;
  }
};

const submit = async () => {
  const res = await recordApi.addExercise(form);
  if (res.data.code === 200) { ElMessage.success('璁板綍鎴愬姛'); load(); }
};

const del = async (row) => {
  if (row._demo) {
    ElMessage.warning('绀轰緥鏁版嵁浠呬緵灞曠ず锛屾棤娉曞垹闄?);
    return;
  }
  await recordApi.deleteExercise(row.id);
  ElMessage.success('宸插垹闄?);
  load();
};

onMounted(load);
</script>
