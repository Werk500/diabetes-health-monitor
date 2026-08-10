<template>
  <div>
    <el-card header="新增饮食记录" style="margin-bottom:20px">
      <el-form :model="form" inline>
        <el-form-item label="食物名称"><el-input v-model="form.foodName" placeholder="如：米饭" /></el-form-item>
        <el-form-item label="餐次">
          <el-select v-model="form.mealType">
            <el-option :value="1" label="早餐" /><el-option :value="2" label="午餐" />
            <el-option :value="3" label="晚餐" /><el-option :value="4" label="加餐" />
          </el-select>
        </el-form-item>
        <el-form-item label="热量(kcal)"><el-input-number v-model="form.calories" :precision="1" :min="0" /></el-form-item>
        <el-form-item label="碳水(g)"><el-input-number v-model="form.carbs" :precision="1" :min="0" /></el-form-item>
        <el-form-item label="蛋白质(g)"><el-input-number v-model="form.protein" :precision="1" :min="0" /></el-form-item>
        <el-form-item label="脂肪(g)"><el-input-number v-model="form.fat" :precision="1" :min="0" /></el-form-item>
        <el-form-item label="份量(g)"><el-input-number v-model="form.portion" :precision="0" :min="0" /></el-form-item>
        <el-form-item label="时间"><el-date-picker v-model="form.eatTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" /></el-form-item>
        <el-form-item><el-button type="primary" @click="submit">提交记录</el-button></el-form-item>
      </el-form>
    </el-card>

    <el-card header="饮食记录" v-loading="loading">
      <el-table :data="records" stripe>
        <el-table-column prop="eatTime" label="时间" width="170" />
        <el-table-column prop="foodName" label="食物" width="140" />
        <el-table-column label="餐次" width="80">
          <template #default="scope">{{ ['','早餐','午餐','晚餐','加餐'][scope.row.mealType] }}</template>
        </el-table-column>
        <el-table-column prop="calories" label="热量(kcal)" width="100" />
        <el-table-column prop="carbs" label="碳水(g)" width="90" />
        <el-table-column prop="protein" label="蛋白质(g)" width="100" />
        <el-table-column prop="fat" label="脂肪(g)" width="80" />
        <el-table-column prop="portion" label="份量(g)" width="80" />
        <el-table-column label="数据来源" width="90">
          <template #default="scope">
            <el-tag v-if="scope.row._demo" type="info" size="small" effect="plain">示例</el-tag>
            <el-tag v-else type="success" size="small" effect="plain">真实</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="scope">
            <el-button :type="scope.row._demo ? 'warning' : 'danger'" size="small" @click="del(scope.row)">删除</el-button>
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
  if (res.data.code === 200) { ElMessage.success('记录成功'); load(); }
};

const del = async (row) => {
  if (row._demo) {
    ElMessage.warning('示例数据仅供展示，无法删除');
    return;
  }
  await recordApi.deleteDiet(row.id);
  ElMessage.success('已删除');
  load();
};

onMounted(load);
</script>