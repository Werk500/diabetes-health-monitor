<template>
  <div>
    <div class="summary-cards">
      <el-card><div class="card-label">最新体重(kg)</div><div class="card-value">{{ data.latestWeight || '--' }}</div></el-card>
      <el-card><div class="card-label">BMI</div><div class="card-value">{{ data.latestBmi || '--' }}</div></el-card>
      <el-card><div class="card-label">体脂率(%)</div><div class="card-value">{{ data.latestBodyFat || '--' }}</div></el-card>
      <el-card><div class="card-label">血压(mmHg)</div><div class="card-value">{{ data.latestSystolic || '--' }}/{{ data.latestDiastolic || '--' }}</div></el-card>
      <el-card><div class="card-label">心率(次/min)</div><div class="card-value">{{ data.latestHeartRate || '--' }}</div></el-card>
      <el-card><div class="card-label">最新血糖(mmol/L)</div><div class="card-value" :style="{color: bloodSugarColor}">{{ data.latestBloodSugar || '--' }}</div></el-card>
      <el-card><div class="card-label">今日摄入(kcal)</div><div class="card-value">{{ data.todayCalories?.toFixed(0) || '--' }}</div></el-card>
      <el-card><div class="card-label">今日运动消耗(kcal)</div><div class="card-value">{{ data.todayExerciseCalories?.toFixed(0) || '--' }}</div></el-card>
    </div>

    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="12">
        <el-card header="体重/BMI 变化趋势" v-loading="loading">
          <div ref="bodyChart" style="width:100%;height:300px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card header="血压/心率趋势" v-loading="loading">
          <div ref="bpChart" style="width:100%;height:300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="12">
        <el-card header="血糖变化趋势" v-loading="loading">
          <div ref="bsChart" style="width:100%;height:300px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card header="运动消耗趋势" v-loading="loading">
          <div ref="exChart" style="width:100%;height:300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="24">
        <el-card header="今日餐饮营养分布" v-loading="loading">
          <div ref="dietChart" style="width:100%;height:300px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue';
import { dashboardApi } from '../api';
import * as echarts from 'echarts';
import { demoDashboardData } from '../mock/demoData';
import { getData, isEmpty } from '../mock/demoManager';
import { useUserStore } from '../stores/user';

const userStore = useUserStore();
const userInfo = userStore.userInfo;
const data = reactive({});
const loading = ref(false);
const bodyChart = ref(null);
const bpChart = ref(null);
const bsChart = ref(null);
const exChart = ref(null);
const dietChart = ref(null);

let charts = [];

const handleResize = () => {
  charts.forEach(c => c.resize());
};

const bloodSugarColor = computed(() => {
  const v = data.latestBloodSugar;
  if (!v) return '#909399';
  if (v < 3.9) return '#e6a23c';
  if (v <= 7.0) return '#67c23a';
  if (v <= 10.0) return '#e6a23c';
  return '#f56c6c';
});

const loadData = async () => {
  loading.value = true;
  try {
    const res = await dashboardApi.get(userInfo.id, {});
    const realData = (res.data.code === 200) ? res.data.data : null;
    if (isEmpty(realData) || !realData.bodyTrend || (!realData.bodyTrend.dates || realData.bodyTrend.dates.length === 0)) {
      Object.assign(data, demoDashboardData);
    } else {
      Object.assign(data, realData);
    }
  } catch {
    Object.assign(data, demoDashboardData);
  } finally {
    loading.value = false;
    await nextTick();
    renderCharts();
  }
};

const renderCharts = () => {
  charts.forEach(c => c.dispose());
  charts = [];

  // Body trend
  if (bodyChart.value && data.bodyTrend) {
    const c = echarts.init(bodyChart.value);
    charts.push(c);
    c.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['体重', 'BMI'], bottom: 0 },
      grid: { left: 50, right: 20, top: 20, bottom: 30 },
      xAxis: { type: 'category', data: data.bodyTrend.dates || [] },
      yAxis: [{ type: 'value', name: 'kg' }, { type: 'value', name: 'BMI' }],
      series: [
        { name: '体重', type: 'line', data: data.bodyTrend.weightValues || [], smooth: true, itemStyle: { color: '#409eff' } },
        { name: 'BMI', type: 'line', yAxisIndex: 1, data: data.bodyTrend.bmiValues || [], smooth: true, itemStyle: { color: '#67c23a' } }
      ]
    });
  }

  // BP + HR
  if (bpChart.value && data.bodyTrend) {
    const c = echarts.init(bpChart.value);
    charts.push(c);
    c.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['收缩压', '舒张压', '心率'], bottom: 0 },
      grid: { left: 50, right: 20, top: 20, bottom: 30 },
      xAxis: { type: 'category', data: data.bodyTrend.dates || [] },
      yAxis: [{ type: 'value', name: 'mmHg' }, { type: 'value', name: 'bpm' }],
      series: [
        { name: '收缩压', type: 'line', data: data.bodyTrend.systolicValues || [], smooth: true, itemStyle: { color: '#f56c6c' } },
        { name: '舒张压', type: 'line', data: data.bodyTrend.diastolicValues || [], smooth: true, itemStyle: { color: '#e6a23c' } },
        { name: '心率', type: 'line', yAxisIndex: 1, data: data.bodyTrend.heartRateValues || [], smooth: true, itemStyle: { color: '#909399' } }
      ]
    });
  }

  // Blood sugar
  if (bsChart.value && data.bloodSugarTrend) {
    const c = echarts.init(bsChart.value);
    charts.push(c);
    c.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['空腹', '餐前', '餐后', '睡前'], bottom: 0 },
      grid: { left: 50, right: 20, top: 20, bottom: 30 },
      xAxis: { type: 'category', data: data.bloodSugarTrend.dates || [] },
      yAxis: { type: 'value', name: 'mmol/L' },
      series: [
        { name: '空腹', type: 'scatter', data: data.bloodSugarTrend.fastingValues || [], itemStyle: { color: '#409eff' } },
        { name: '餐前', type: 'scatter', data: data.bloodSugarTrend.beforeMealValues || [], itemStyle: { color: '#67c23a' } },
        { name: '餐后', type: 'scatter', data: data.bloodSugarTrend.afterMealValues || [], itemStyle: { color: '#e6a23c' } },
        { name: '睡前', type: 'scatter', data: data.bloodSugarTrend.bedtimeValues || [], itemStyle: { color: '#f56c6c' } }
      ]
    });
  }

  // Exercise
  if (exChart.value && data.exerciseStats) {
    const c = echarts.init(exChart.value);
    charts.push(c);
    c.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['消耗热量', '运动时长'], bottom: 0 },
      grid: { left: 50, right: 20, top: 20, bottom: 30 },
      xAxis: { type: 'category', data: data.exerciseStats.dates || [] },
      yAxis: [{ type: 'value', name: 'kcal' }, { type: 'value', name: 'min' }],
      series: [
        { name: '消耗热量', type: 'bar', data: data.exerciseStats.calorieBurnedValues || [], itemStyle: { color: '#409eff' } },
        { name: '运动时长', type: 'line', yAxisIndex: 1, data: data.exerciseStats.durationValues || [], itemStyle: { color: '#67c23a' } }
      ]
    });
  }

  // Diet pie
  if (dietChart.value && data.dietStats) {
    const c = echarts.init(dietChart.value);
    charts.push(c);
    c.setOption({
      tooltip: { trigger: 'item' },
      legend: { orient: 'vertical', left: 'left' },
      series: [{
        name: '热量分布',
        type: 'pie',
        radius: ['40%', '70%'],
        data: (data.dietStats.mealNames || []).map((name, i) => ({
          name: name,
          value: data.dietStats.calorieValues?.[i] || 0
        })),
        label: { formatter: '{b}: {c} kcal' }
      }]
    });
  }
};

onMounted(() => {
  window.addEventListener('resize', handleResize);
  loadData();
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  charts.forEach(c => c.dispose());
  charts = [];
});
</script>

<style scoped>
.summary-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.card-label { font-size: 13px; color: #909399; margin-bottom: 8px; }
.card-value { font-size: 24px; font-weight: bold; color: #303133; }
</style>