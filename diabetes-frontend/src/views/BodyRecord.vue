<template>
  <div>
    <el-card header="鏂板韬綋鎸囨爣璁板綍" style="margin-bottom:20px">
      <el-form :model="form" inline>
        <el-form-item label="浣撻噸(kg)"><el-input-number v-model="form.weight" :precision="1" :min="30" :max="200" /></el-form-item>
        <el-form-item label="浣撹剛鐜?%)"><el-input-number v-model="form.bodyFat" :precision="1" :min="1" :max="60" /></el-form-item>
        <el-form-item label="鏀剁缉鍘?><el-input-number v-model="form.systolicPressure" :min="60" :max="250" /></el-form-item>
        <el-form-item label="鑸掑紶鍘?><el-input-number v-model="form.diastolicPressure" :min="30" :max="150" /></el-form-item>
        <el-form-item label="蹇冪巼"><el-input-number v-model="form.heartRate" :min="30" :max="220" /></el-form-item>
        <el-form-item label="鑵板洿(cm)"><el-input-number v-model="form.waistline" :precision="1" :min="50" :max="150" /></el-form-item>
        <el-form-item label="璁板綍鏃ユ湡"><el-date-picker v-model="form.recordDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item><el-button type="primary" @click="submit">鎻愪氦璁板綍</el-button></el-form-item>
      </el-form>
    </el-card>

    <el-card header="鍘嗗彶璁板綍" v-loading="loading">
      <el-table :data="records" stripe>
        <el-table-column prop="recordDate" label="鏃ユ湡" width="120" />
        <el-table-column prop="weight" label="浣撻噸(kg)" width="100" />
        <el-table-column prop="bmi" label="BMI" width="80" />
        <el-table-column prop="bodyFat" label="浣撹剛鐜?%)" width="100" />
        <el-table-column label="琛€鍘? width="120">
          <template #default="scope">{{ scope.row.systolicPressure }}/{{ scope.row.diastolicPressure }}</template>
        </el-table-column>
        <el-table-column prop="heartRate" label="蹇冪巼" width="80" />
        <el-table-column prop="waistline" label="鑵板洿(cm)" width="100" />
        <el-table-column label="鏁版嵁鏉ユ簮" width="90">
          <template #default="scope">
            <el-tag v-if="scope.row._demo" type="info" size="small" effect="plain">绀轰緥</el-tag>
            <el-tag v-else type="success" size="small" effect="plain">鐪熷疄</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="鎿嶄綔" width="100">
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
import { demoBodyRecords } from '../mock/demoData';
import { getData } from '../mock/demoManager';

import { useUserStore } from '../stores/user';
const userStore = useUserStore();
const userInfo = userStore.userInfo;
const records = ref([]);
const loading = ref(false);
const form = reactive({
  userId: userInfo.id, weight: null, bodyFat: null, systolicPressure: null,
  diastolicPressure: null, heartRate: null, waistline: null, recordDate: null
});

const load = async () => {
  loading.value = true;
  try {
    const res = await recordApi.bodyList(userInfo.id, {});
    const real = (res.data.code === 200) ? (res.data.data || []) : [];
    real.forEach(r => r._demo = false);
    const demo = demoBodyRecords.map(d => ({ ...d, _demo: true }));
    records.value = getData(demo, real);
  } catch {
    records.value = getData(demoBodyRecords.map(d => ({ ...d, _demo: true })), []);
  } finally {
    loading.value = false;
  }
};

const submit = async () => {
  const res = await recordApi.addBody(form);
  if (res.data.code === 200) { ElMessage.success('璁板綍鎴愬姛'); load(); }
};

const del = async (row) => {
  if (row._demo) {
    ElMessage.warning('绀轰緥鏁版嵁浠呬緵灞曠ず锛屾棤娉曞垹闄?);
    return;
  }
  await recordApi.deleteBody(row.id);
  ElMessage.success('宸插垹闄?);
  load();
};

onMounted(load);
</script>
