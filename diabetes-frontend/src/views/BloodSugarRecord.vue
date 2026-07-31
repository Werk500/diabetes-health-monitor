<template>
  <div>
    <el-card header="鏂板琛€绯栬褰? style="margin-bottom:20px">
      <el-form :model="form" inline>
        <el-form-item label="琛€绯栧€?mmol/L)"><el-input-number v-model="form.bloodSugar" :precision="1" :min="1" :max="33" /></el-form-item>
        <el-form-item label="娴嬮噺绫诲瀷">
          <el-select v-model="form.measureType">
            <el-option :value="1" label="绌鸿吂" />
            <el-option :value="2" label="椁愬墠" />
            <el-option :value="3" label="椁愬悗2h" />
            <el-option :value="4" label="鐫″墠" />
            <el-option :value="5" label="鍑屾櫒" />
          </el-select>
        </el-form-item>
        <el-form-item label="娴嬮噺鏃堕棿"><el-date-picker v-model="form.measureTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" /></el-form-item>
        <el-form-item><el-button type="primary" @click="submit">鎻愪氦璁板綍</el-button></el-form-item>
      </el-form>
    </el-card>

    <el-card header="鍘嗗彶璁板綍" v-loading="loading">
      <el-table :data="records" stripe>
        <el-table-column prop="measureTime" label="娴嬮噺鏃堕棿" width="180" />
        <el-table-column prop="bloodSugar" label="琛€绯栧€?mmol/L)" width="140">
          <template #default="scope">
            <span :style="{color: sugarColor(scope.row.bloodSugar), fontWeight:'bold'}">{{ scope.row.bloodSugar }}</span>
          </template>
        </el-table-column>
        <el-table-column label="绫诲瀷" width="100">
          <template #default="scope">{{ ['','绌鸿吂','椁愬墠','椁愬悗2h','鐫″墠','鍑屾櫒'][scope.row.measureType] }}</template>
        </el-table-column>
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
import { demoBloodSugarRecords } from '../mock/demoData';
import { getData } from '../mock/demoManager';

import { useUserStore } from '../stores/user';
const userStore = useUserStore();
const userInfo = userStore.userInfo;
const records = ref([]);
const loading = ref(false);
const form = reactive({ userId: userInfo.id, bloodSugar: null, measureType: 1, measureTime: null });

const sugarColor = (v) => {
  if (v < 3.9) return '#e6a23c';
  if (v <= 7.0) return '#67c23a';
  if (v <= 10.0) return '#e6a23c';
  return '#f56c6c';
};

const load = async () => {
  loading.value = true;
  try {
    const res = await recordApi.bloodSugarList(userInfo.id, {});
    const real = (res.data.code === 200) ? (res.data.data || []) : [];
    // 鏍囪鐪熷疄鏁版嵁
    real.forEach(r => r._demo = false);
    // 鏍囪绀轰緥鏁版嵁
    const demo = demoBloodSugarRecords.map(d => ({ ...d, _demo: true }));
    records.value = getData(demo, real);
  } catch {
    records.value = getData(demoBloodSugarRecords.map(d => ({ ...d, _demo: true })), []);
  } finally {
    loading.value = false;
  }
};

const submit = async () => {
  const res = await recordApi.addBloodSugar(form);
  if (res.data.code === 200) { ElMessage.success('璁板綍鎴愬姛'); load(); }
};

const del = async (row) => {
  if (row._demo) {
    ElMessage.warning('绀轰緥鏁版嵁浠呬緵灞曠ず锛屾棤娉曞垹闄?);
    return;
  }
  await recordApi.deleteBloodSugar(row.id);
  ElMessage.success('宸插垹闄?);
  load();
};

onMounted(load);
</script>
