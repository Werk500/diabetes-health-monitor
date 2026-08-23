<template>
  <div class="analysis-page">
    <el-row :gutter="16">
      <!-- 血糖智能分析 -->
      <el-col :xs="24" :md="12">
        <el-card shadow="hover" class="analysis-card">
          <template #header>
            <div class="card-header">
              <el-icon :size="20" color="#f56c6c"><Sugar /></el-icon>
              <span>血糖智能分析</span>
            </div>
          </template>
          <p class="card-desc">分析近 N 天血糖数据，获取趋势和控糖建议</p>
          <div class="card-controls">
            <el-input-number v-model="sugarDays" :min="3" :max="30" size="small" />
            <span class="unit-label">天数据</span>
          </div>
          <el-button type="primary" @click="analyzeBloodSugar" :loading="sugarLoading" class="analyze-btn">
            <el-icon><TrendCharts /></el-icon> 开始分析
          </el-button>
          <div v-if="sugarResult" class="result-box">
            <div class="result-content">{{ sugarResult }}</div>
          </div>
        </el-card>
      </el-col>

      <!-- 饮食智能分析 -->
      <el-col :xs="24" :md="12">
        <el-card shadow="hover" class="analysis-card">
          <template #header>
            <div class="card-header">
              <el-icon :size="20" color="#e6a23c"><DishDot /></el-icon>
              <span>饮食智能分析</span>
            </div>
          </template>
          <p class="card-desc">分析近 N 天饮食，获取营养评估和改善建议</p>
          <div class="card-controls">
            <el-input-number v-model="dietDays" :min="3" :max="30" size="small" />
            <span class="unit-label">天数据</span>
          </div>
          <el-button type="warning" @click="analyzeDiet" :loading="dietLoading" class="analyze-btn">
            <el-icon><TrendCharts /></el-icon> 开始分析
          </el-button>
          <div v-if="dietResult" class="result-box">
            <div class="result-content">{{ dietResult }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 每日综合小结 -->
    <el-card shadow="hover" class="daily-card">
      <template #header>
        <div class="card-header">
          <el-icon :size="20" color="#409eff"><Document /></el-icon>
          <span>每日综合健康小结</span>
        </div>
      </template>
      <p class="card-desc">聚合今日血糖、饮食、运动、体征数据，AI 综合分析生成健康小结</p>
      <el-button type="success" @click="analyzeDaily" :loading="dailyLoading" class="analyze-btn daily-btn">
        <el-icon><TrendCharts /></el-icon> 生成今日小结
      </el-button>
      <div v-if="dailyResult" class="result-box">
        <div class="result-content">{{ dailyResult }}</div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Watermelon, DishDot, Document, TrendCharts } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const sugarDays = ref(7)
const dietDays = ref(7)
const sugarLoading = ref(false)
const dietLoading = ref(false)
const dailyLoading = ref(false)
const sugarResult = ref('')
const dietResult = ref('')
const dailyResult = ref('')

const token = () => localStorage.getItem('token')

const streamFetch = async (url, body, resultRef) => {
  resultRef.value = ''
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token() },
    body: JSON.stringify(body || {})
  })

  if (!response.ok) {
    let msg = '分析失败'
    try {
      const data = await response.json()
      msg = data?.msg || msg
    } catch (_) {}
    throw new Error(msg)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const lines = buffer.split('\n')
    buffer = lines.pop() || ''
    let currentEvent = 'message'
    let dataLines = []
    for (const line of lines) {
      if (line === '') {
        if (dataLines.length > 0) {
          const payload = dataLines.join('\n')
          dataLines = []
          if (currentEvent === 'error') {
            let msg = '分析失败'
            try {
              msg = JSON.parse(payload).msg || msg
            } catch (_) {}
            throw new Error(msg)
          }
          resultRef.value += payload
        }
        currentEvent = 'message'
        continue
      }
      if (line.startsWith('event:')) {
        currentEvent = line.substring(6).trim()
      } else if (line.startsWith('data:')) {
        dataLines.push(line.substring(5).replace(/^ /, ''))
      }
    }
  }
}

const analyzeBloodSugar = async () => {
  sugarLoading.value = true
  try { await streamFetch('/api/ai/analysis/blood-sugar', { days: sugarDays.value }, sugarResult) }
  catch (e) { ElMessage.error(e.message || '分析失败') }
  finally { sugarLoading.value = false }
}

const analyzeDiet = async () => {
  dietLoading.value = true
  try { await streamFetch('/api/ai/analysis/diet', { days: dietDays.value }, dietResult) }
  catch (e) { ElMessage.error(e.message || '分析失败') }
  finally { dietLoading.value = false }
}

const analyzeDaily = async () => {
  dailyLoading.value = true
  try { await streamFetch('/api/ai/analysis/daily-report', null, dailyResult) }
  catch (e) { ElMessage.error(e.message || '分析失败') }
  finally { dailyLoading.value = false }
}
</script>

<style scoped>
.analysis-page { max-width: 1100px; margin: 0 auto; }

.analysis-card { margin-bottom: 16px; }
.daily-card { margin-top: 0; }

.card-header { display: flex; align-items: center; gap: 8px; font-size: 16px; font-weight: 600; }
.card-desc { color: #94a3b8; font-size: 13px; margin-bottom: 14px; }
.card-controls { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.unit-label { color: #94a3b8; font-size: 13px; }

.analyze-btn { width: 100%; height: 38px; font-size: 14px; }
.daily-btn { max-width: 400px; }

.result-box { margin-top: 16px; background: rgba(17,24,39,0.5); border-radius: 10px; padding: 16px; max-height: 400px; overflow-y: auto; }
.result-content { line-height: 1.8; white-space: pre-wrap; font-size: 14px; color: #e2e8f0; }
</style>
