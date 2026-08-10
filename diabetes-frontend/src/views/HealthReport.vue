<template>
  <div class="report-page">
    <el-card shadow="hover" class="report-card">
      <div class="report-content">
        <div class="report-icon-bg">
          <el-icon :size="72" color="#409eff"><Document /></el-icon>
        </div>
        <h2>糖尿病健康管理报告</h2>
        <div class="report-features">
          <div class="feature-item">
            <el-icon color="#67c23a"><CircleCheck /></el-icon>
            <span>基本信息与最新体征</span>
          </div>
          <div class="feature-item">
            <el-icon color="#67c23a"><CircleCheck /></el-icon>
            <span>近7天血糖记录及趋势统计</span>
          </div>
          <div class="feature-item">
            <el-icon color="#67c23a"><CircleCheck /></el-icon>
            <span>饮食与运动数据汇总分析</span>
          </div>
          <div class="feature-item">
            <el-icon color="#67c23a"><CircleCheck /></el-icon>
            <span>AI 智能健康建议</span>
          </div>
        </div>
        <el-button type="primary" size="large" @click="downloadReport" :loading="downloading" class="download-btn">
          <el-icon><Download /></el-icon>
          <span>下载 PDF 报告</span>
        </el-button>
        <p class="report-note">报告为 PDF 格式，可保存或打印</p>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Document, Download, CircleCheck } from '@element-plus/icons-vue'
import { reportApi } from '../api'
import { ElMessage } from 'element-plus'

const downloading = ref(false)

const downloadReport = async () => {
  downloading.value = true
  try {
    const res = await reportApi.download()
    const blob = new Blob([res.data], { type: 'application/pdf' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '健康报告.pdf'
    a.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('下载成功')
  } catch (e) {
    ElMessage.error('下载失败')
  } finally { downloading.value = false }
}
</script>

<style scoped>
.report-page { max-width: 580px; margin: 0 auto; }
.report-content { text-align: center; padding: 48px 20px; }
.report-icon-bg { margin-bottom: 20px; }
.report-content h2 { font-size: 24px; color: #e2e8f0; margin: 0 0 24px; }

.report-features { display: inline-block; text-align: left; margin-bottom: 32px; }
.feature-item { display: flex; align-items: center; gap: 10px; padding: 8px 0; font-size: 14px; color: #94a3b8; }

.download-btn {
  height: 48px; font-size: 16px; padding: 0 40px; border-radius: 12px;
  display: inline-flex; align-items: center; gap: 8px;
}
.report-note { font-size: 12px; color: #64748b; margin-top: 14px; }
</style>