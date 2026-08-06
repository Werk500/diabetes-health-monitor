<template>
  <div class="food-recognition-container">
    <el-card>
      <template #header><span>食物拍照识别</span></template>
      <div class="upload-area" v-if="!result">
        <el-upload :auto-upload="false" :show-file-list="false" :on-change="handleFile" accept="image/*" drag>
          <el-icon class="upload-icon"><CameraFilled /></el-icon>
          <div>点击或拖拽上传食物图片</div>
          <div class="upload-hint">支持 JPG/PNG，建议小于2MB</div>
        </el-upload>
        <div v-if="previewUrl" class="preview-section">
          <img :src="previewUrl" class="preview-img" />
          <el-button type="primary" @click="recognize" :loading="recognizing" size="large" style="margin-top:16px;width:100%">开始识别</el-button>
        </div>
      </div>
      <div v-if="result" class="result-section">
        <div class="result-header">
          <span style="font-size:18px;font-weight:bold">{{ result.foodName || '识别结果' }}</span>
          <el-button size="small" @click="reset">重新上传</el-button>
        </div>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="热量">{{ result.calories }} kcal</el-descriptions-item>
          <el-descriptions-item label="碳水化合物">{{ result.carbs }} g</el-descriptions-item>
          <el-descriptions-item label="蛋白质">{{ result.protein }} g</el-descriptions-item>
          <el-descriptions-item label="脂肪">{{ result.fat }} g</el-descriptions-item>
          <el-descriptions-item label="升糖指数">GI {{ result.glycemicIndex || '未知' }}</el-descriptions-item>
        </el-descriptions>
        <el-alert :title="result.suggestion || '暂无建议'" type="info" :closable="false" style="margin-top:16px" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { foodApi } from '../api/index';
import { ElMessage } from 'element-plus';

const previewUrl = ref('');
const recognizing = ref(false);
const result = ref(null);
let fileData = null;

const handleFile = (uploadFile) => {
  fileData = uploadFile.raw;
  previewUrl.value = URL.createObjectURL(fileData);
  result.value = null;
};

const recognize = async () => {
  if (!fileData) return;
  recognizing.value = true;
  try {
    const formData = new FormData();
    formData.append('file', fileData);
    const res = await foodApi.recognize(formData);
    if (res.data?.code === 200) result.value = res.data.data;
    else ElMessage.error(res.data?.msg || '识别失败');
  } catch (e) {
    ElMessage.error('识别失败，请稍后重试');
  } finally {
    recognizing.value = false;
  }
};

const reset = () => { result.value = null; previewUrl.value = ''; fileData = null; };
</script>

<style scoped>
.food-recognition-container { max-width: 600px; margin: 0 auto; }
.upload-area { text-align: center; padding: 20px 0; }
.upload-icon { font-size: 48px; color: #409eff; }
.upload-hint { font-size: 12px; color: #c0c4cc; margin-top: 8px; }
.preview-section { margin-top: 20px; }
.preview-img { max-width: 300px; max-height: 300px; border-radius: 8px; border: 1px solid #ebeef5; }
.result-section { padding: 10px 0; }
.result-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
</style>