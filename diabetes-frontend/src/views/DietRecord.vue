<template>
  <div>
    <el-card header="鏂板楗璁板綍" style="margin-bottom:20px">
      <el-form :model="form" inline>
        <el-form-item label="椋熺墿鍚嶇О"><el-input v-model="form.foodName" placeholder="濡傦細绫抽キ" /></el-form-item>
        <el-form-item label="椁愭">
          <el-select v-model="form.mealType">
            <el-option :value="1" label="鏃╅" /><el-option :value="2" label="鍗堥" />
            <el-option :value="3" label="鏅氶" /><el-option :value="4" label="鍔犻" />
          </el-select>
        </el-form-item>
        <el-form-item label="鐑噺(kcal)"><el-input-number v-model="form.calories" :precision="1" :min="0" /></el-form-item>
        <el-form-item label="纰虫按(g)"><el-input-number v-model="form.carbs" :precision="1" :min="0" /></el-form-item>
        <el-form-item label="铔嬬櫧璐?g)"><el-input-number v-model="form.protein" :precision="1" :min="0" /></el-form-item>
        <el-form-item label="鑴傝偑(g)"><el-input-number v-model="form.fat" :precision="1" :min="0" /></el-form-item>
        <el-form-item label="浠介噺(g)"><el-input-number v-model="form.portion" :precision="0" :min="0" /></el-form-item>
        <el-form-item label="鏃堕棿"><el-date-picker v-model="form.eatTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" /></el-form-item>
        <el-form-item><el-button type="primary" @click="submit">鎻愪氦璁板綍</el-button></el-form-item>
      </el-form>
    </el-card>

    <el-card header="楗璁板綍" v-loading="loading">
      <el-table :data="records" stripe>
        <el-table-column prop="eatTime" label="鏃堕棿" width="170" />
        <el-table-column prop="foodName" label="椋熺墿" width="140" />
        <el-table-column label="椁愭" width="80">
          <template #default="scope">{{ ['','鏃╅','鍗堥','鏅氶','鍔犻'][scope.row.mealType] }}</template>
        </el-table-column>
        <el-table-column prop="calories" label="鐑噺(kcal)" width="100" />
        <el-table-column prop="carbs" label="纰虫按(g)" width="90" />
        <el-table-column prop="protein" label="铔嬬櫧璐?g)" width="100" />
        <el-table-column prop="fat" label="鑴傝偑(g)" width="80" />
        <el-table-column prop="portion" label="浠介噺(g)" width="80" />
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
import { demoDietRecords } from '../mock/demoData';
import { getData } from '../mock/demoManager';

import { useUserStore } from '../stores/user';
const userStore = useUserStore();
const userInfo = userStore.userInfo;
const records = ref([]);
const loading = ref(false);
const form = reactive({
  userId: userInfo.id, foodName: '', mealType: 1, calories: null,
  carbs: null, protein: null, fat: null, portion: null, eatTime: null
});

const load = async () => {
  loading.value = true;
  try {
    const res = await recordApi.dietList(userInfo.id, {});
    const real = (res.data.code === 200) ? (res.data.data || []) : [];
    real.forEach(r => r._demo = false);
    const demo = demoDietRecords.map(d => ({ ...d, _demo: true }));
    records.value = getData(demo, real);
  } catch {
    records.value = getData(demoDietRecords.map(d => ({ ...d, _demo: true })), []);
  } finally {
    loading.value = false;
  }
};

const submit = async () => {
  const res = await recordApi.addDiet(form);
  if (res.data.code === 200) { ElMessage.success('璁板綍鎴愬姛'); load(); }
};

const del = async (row) => {
  if (row._demo) {
    ElMessage.warning('绀轰緥鏁版嵁浠呬緵灞曠ず锛屾棤娉曞垹闄?);
    return;
  }
  await recordApi.deleteDiet(row.id);
  ElMessage.success('宸插垹闄?);
  load();
};

onMounted(load);
</script>
