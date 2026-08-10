<template>
  <div class="admin-dashboard">
    <el-row :gutter="16" class="stats-row">
      <el-col :xs="12" :sm="8" :md="6" :lg="4" v-for="(s, i) in statCards" :key="i">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-inner">
            <el-icon :size="30" :color="s.color">
              <component :is="s.icon" />
            </el-icon>
            <div class="stat-body">
              <div class="stat-value">{{ s.value }}</div>
              <div class="stat-label">{{ s.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover" class="chart-card">
      <template #header>
        <div class="card-header">
          <el-icon :size="18" color="#409eff"><TrendCharts /></el-icon>
          <span>文章分类统计</span>
        </div>
      </template>
      <div ref="chart" class="chart-box"></div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { User, Document, Check, Bicycle, TrendCharts, DataBoard, Orange } from '@element-plus/icons-vue'
import { adminApi } from '../../api'
import * as echarts from 'echarts'

const chart = ref(null)

const statCards = reactive([
  { label: '用户总数', value: 0, icon: User, color: '#409eff' },
  { label: '文章总数', value: 0, icon: Document, color: '#67c23a' },
  { label: '已推送', value: 0, icon: Check, color: '#e6a23c' },
  { label: '运动类型', value: 0, icon: Bicycle, color: '#f56c6c' },
  { label: '血糖记录', value: 0, icon: DataBoard, color: '#00d4ff' },
  { label: '身体记录', value: 0, icon: Orange, color: '#a855f7' },
])

onMounted(async () => {
  try {
    const res = await adminApi.stats()
    if (res.data.code === 200) {
      const d = res.data.data
      statCards[0].value = d.userCount || 0
      statCards[1].value = d.articleCount || 0
      statCards[2].value = d.pushedCount || 0
      statCards[3].value = d.exerciseTypeCount || 0
      statCards[4].value = d.bloodSugarCount || 0
      statCards[5].value = d.bodyCount || 0
    }
  } catch (e) {
    console.error('获取统计数据失败', e)
  }

  const catRes = await adminApi.categoryStats()
  await nextTick()
  if (chart.value && catRes.data.code === 200) {
    const c = echarts.init(chart.value)
    const d = catRes.data.data
    c.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} 篇 ({d}%)' },
      legend: { bottom: 0, textStyle: { color: '#94a3b8' } },
      series: [{
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['50%', '45%'],
        data: [
          { name: '血糖监测', value: d.bloodSugar || 0, itemStyle: { color: '#409eff' } },
          { name: '控糖饮食', value: d.dietControl || 0, itemStyle: { color: '#67c23a' } },
          { name: '并发症预防', value: d.complication || 0, itemStyle: { color: '#e6a23c' } },
          { name: '运动建议', value: d.exerciseSuggestion || 0, itemStyle: { color: '#f56c6c' } }
        ],
        label: { formatter: '{b}\n{d}%', color: '#cbd5e1' }
      }]
    })
  }
})
</script>

<style scoped>
.admin-dashboard { max-width: 1200px; }
.stats-row { margin-bottom: 16px; }
.stat-card {
  margin-bottom: 14px;
  background: rgba(17, 24, 39, 0.6);
  border: 1px solid rgba(51, 65, 85, 0.35);
  border-radius: 12px;
  transition: all 0.3s;
}
.stat-card:hover {
  border-color: rgba(0, 212, 255, 0.35);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
}
.stat-inner { display: flex; align-items: center; gap: 12px; }
.stat-body { flex: 1; min-width: 0; }
.stat-value { font-size: 24px; font-weight: 700; color: #e2e8f0; line-height: 1.3; }
.stat-label { font-size: 12px; color: #94a3b8; margin-top: 4px; }

.card-header { display: flex; align-items: center; gap: 8px; font-size: 16px; font-weight: 600; }
.chart-card { margin-top: 16px; background: rgba(17, 24, 39, 0.6); border: 1px solid rgba(51, 65, 85, 0.3); }
.chart-box { width: 100%; height: 350px; }
</style>