<template>
  <div class="ai-analysis-container">
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header><span>血糖智能分析</span></template>
          <p style="color:#909399;margin-bottom:12px">分析近7天血糖数据，获取趋势和控糖建议</p>
          <el-input-number v-model="sugarDays" :min="3" :max="30" size="small" /> <span style="margin-left:8px;color:#909399">天数据</span>
          <el-button type="primary" @click="analyzeBloodSugar" :loading="sugarLoading" style="margin-top:12px;width:100%">开始分析</el-button>
          <div v-if="sugarResult" class="analysis-result">{{ sugarResult }}</div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span>饮食智能分析</span></template>
          <p style="color:#909399;margin-bottom:12px">分析近7天饮食，获取营养评估和改善建议</p>
          <el-input-number v-model="dietDays" :min="3" :max="30" size="small" /> <span style="margin-left:8px;color:#909399">天数据</span>
          <el-button type="primary" @click="analyzeDiet" :loading="dietLoading" style="margin-top:12px;width:100%">开始分析</el-button>
          <div v-if="dietResult" class="analysis-result">{{ dietResult }}</div>
        </el-card>
      </el-col>
    </el-row>
    <el-card style="margin-top:20px">
      <template #header><span>每日综合健康小结</span></template>
      <p style="color:#909399;margin-bottom:12px">聚合今日血糖、饮食、运动、体征数据，AI 综合分析</p>
      <el-button type="success" @click="analyzeDaily" :loading="dailyLoading" style="width:100%">生成今日小结</el-button>
      <div v-if="dailyResult" class="analysis-result">{{ dailyResult }}</div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { ElMessage } from 'element-plus';

const sugarDays = ref(7);
const dietDays = ref(7);
const sugarLoading = ref(false);
const dietLoading = ref(false);
const dailyLoading = ref(false);
const sugarResult = ref('');
const dietResult = ref('');
const dailyResult = ref('');

const token = () => localStorage.getItem('token');

const streamFetch = async (url, body, resultRef) => {
  resultRef.value = '';
  const response = await fetch(url, {
    method: 'POST', headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token() }, body: JSON.stringify(body || {})
  });
  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    const chunk = decoder.decode(value);
    const lines = chunk.split('\n');
    for (const line of lines) {
      if (line.startsWith('data:')) {
        try {
          const json = JSON.parse(line.substring(5).trim());
          const delta = json?.output?.choices?.[0]?.message?.content;
          if (Array.isArray(delta) && delta.length > 0) resultRef.value += delta[0].text || '';
        } catch (e) {}
      }
    }
  }
};

const analyzeBloodSugar = async () => { sugarLoading.value = true; try { await streamFetch('/api/ai/analysis/blood-sugar', { days: sugarDays.value }, sugarResult); } catch (e) { ElMessage.error('分析失败'); } finally { sugarLoading.value = false; } };
const analyzeDiet = async () => { dietLoading.value = true; try { await streamFetch('/api/ai/analysis/diet', { days: dietDays.value }, dietResult); } catch (e) { ElMessage.error('分析失败'); } finally { dietLoading.value = false; } };
const analyzeDaily = async () => { dailyLoading.value = true; try { await streamFetch('/api/ai/analysis/daily-report', null, dailyResult); } catch (e) { ElMessage.error('分析失败'); } finally { dailyLoading.value = false; } };
</script>

<style scoped>
.ai-analysis-container { max-width: 900px; margin: 0 auto; }
.analysis-result { margin-top: 16px; padding: 16px; background: #f5f7fa; border-radius: 8px; line-height: 1.8; white-space: pre-wrap; max-height: 500px; overflow-y: auto; }
</style>