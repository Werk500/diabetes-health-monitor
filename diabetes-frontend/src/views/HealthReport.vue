<template>
  <div class="report-container">
    <el-card>
      <template #header><span>健康报告</span></template>
      <div class="report-content">
        <el-icon class="report-icon"><Document /></el-icon>
        <h3>糖尿病健康管理报告</h3>
        <p>报告包含：基本信息、最新体征、近7天血糖记录及统计、饮食运动汇总</p>
        <el-button type="primary" size="large" @click="downloadReport" :loading="downloading">
          <el-icon><Download /></el-icon> 下载 PDF 报告
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { reportApi } from '../api/index';
import { ElMessage } from 'element-plus';

const downloading = ref(false);

const downloadReport = async () => {
  downloading.value = true;
  try {
    const res = await reportApi.download();
    const blob = new Blob([res.data], { type: 'application/pdf' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = '健康报告.pdf';
    a.click();
    window.URL.revokeObjectURL(url);
    ElMessage.success('下载成功');
  } catch (e) {
    ElMessage.error('下载失败');
  } finally {
    downloading.value = false;
  }
};
</script>

<style scoped>
.report-container { max-width: 600px; margin: 0 auto; }
.report-content { text-align: center; padding: 40px 0; }
.report-icon { font-size: 64px; color: #409eff; }
.report-content h3 { margin: 16px 0 8px; }
.report-content p { color: #909399; margin-bottom: 24px; }
</style>