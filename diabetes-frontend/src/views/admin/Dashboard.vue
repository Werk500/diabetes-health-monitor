<template>
  <div>
    <el-row :gutter="20">
      <el-col :span="6" v-for="s in stats" :key="s.label">
        <el-card>
          <div style="text-align:center">
            <div style="color:#909399;font-size:14px">{{ s.label }}</div>
            <div style="font-size:28px;font-weight:bold;color:#409eff;margin:8px 0">{{ s.value }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-card header="文章分类统计" style="margin-top:20px">
      <div ref="chart" style="width:100%;height:350px"></div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue';
import { adminApi, userApi } from '../../api';
import * as echarts from 'echarts';

const chart = ref(null);
const stats = ref([
  { label: '总用户数', value: 0 },
  { label: '文章总数', value: 0 },
  { label: '已推送文章', value: 0 },
  { label: '运动类型', value: 0 }
]);

onMounted(async () => {
  const [users, articles, exTypes] = await Promise.all([
    userApi.list(), adminApi.articleList({page:1,size:100}), adminApi.getExerciseTypes()
  ]);
  if (users.data.code === 200) stats.value[0].value = users.data.data.length;
  const arts = articles.data.data?.records || [];
  stats.value[1].value = arts.length;
  stats.value[2].value = arts.filter(a => a.pushStatus === 1).length;
  if (exTypes.data.code === 200) stats.value[3].value = exTypes.data.data.length;

  const catRes = await adminApi.categoryStats();
  await nextTick();
  if (chart.value && catRes.data.code === 200) {
    const c = echarts.init(chart.value);
    const d = catRes.data.data;
    c.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [{
        type: 'pie', radius: ['40%', '70%'],
        data: [
          { name: '血糖监测', value: d.bloodSugar || 0 },
          { name: '控糖饮食', value: d.dietControl || 0 },
          { name: '并发症预防', value: d.complication || 0 },
          { name: '运动建议', value: d.exerciseSuggestion || 0 }
        ]
      }]
    });
  }
});
</script>