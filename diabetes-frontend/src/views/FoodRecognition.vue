<template>
  <div class="food-page">
    <div class="food-card">
      <div class="card-header">
        <div class="header-title">
          <el-icon :size="20"><Camera /></el-icon>
          <span>食物拍照识别</span>
        </div>
        <el-tag type="info" size="small" class="ai-tag">AI 视觉识别</el-tag>
      </div>

      <!-- 上传区域 -->
      <div v-if="!result" class="upload-section">
        <div class="upload-zone">
          <el-upload
            :auto-upload="false"
            :show-file-list="false"
            :on-change="handleFile"
            accept="image/*"
            drag
            class="uploader"
          >
            <div v-if="!previewUrl" class="upload-placeholder">
              <div class="upload-icon-ring">
                <el-icon :size="40"><CameraFilled /></el-icon>
              </div>
              <div class="upload-text">点击或拖拽上传食物图片</div>
              <div class="upload-hint">支持 JPG / PNG，建议小于 2MB</div>
            </div>
            <img v-else :src="previewUrl" class="preview-img" />
          </el-upload>

          <div v-if="previewUrl" class="action-bar">
            <el-button @click="reset" class="btn-outline">重新选择</el-button>
            <el-button type="primary" @click="recognize" :loading="recognizing" size="large" class="recognize-btn">
              <el-icon><Search /></el-icon>
              {{ recognizing ? '识别中...' : '开始识别' }}
            </el-button>
          </div>
        </div>
      </div>

      <!-- 识别结果 -->
      <div v-if="result" class="result-section">
        <div class="result-header">
          <div>
            <h3 class="result-title">{{ result.foodName || '识别结果' }}</h3>
            <span class="result-gi" v-if="result.glycemicIndex">
              血糖生成指数：{{ result.glycemicIndex }}
            </span>
          </div>
          <el-button @click="reset" class="btn-outline">重新上传</el-button>
        </div>

        <el-row :gutter="12" class="nutrient-grid">
          <el-col :span="6">
            <div class="nutrient-item calories">
              <div class="nutrient-value">{{ result.calories || '--' }}</div>
              <div class="nutrient-unit">kcal</div>
              <div class="nutrient-label">热量</div>
              <div class="nutrient-ring"></div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="nutrient-item carbs">
              <div class="nutrient-value">{{ result.carbs || '--' }}</div>
              <div class="nutrient-unit">g</div>
              <div class="nutrient-label">碳水</div>
              <div class="nutrient-ring"></div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="nutrient-item protein">
              <div class="nutrient-value">{{ result.protein || '--' }}</div>
              <div class="nutrient-unit">g</div>
              <div class="nutrient-label">蛋白质</div>
              <div class="nutrient-ring"></div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="nutrient-item fat">
              <div class="nutrient-value">{{ result.fat || '--' }}</div>
              <div class="nutrient-unit">g</div>
              <div class="nutrient-label">脂肪</div>
              <div class="nutrient-ring"></div>
            </div>
          </el-col>
        </el-row>

        <div v-if="result.suggestion" class="suggestion-box">
          <div class="suggestion-accent"></div>
          <div class="suggestion-text">{{ result.suggestion }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Camera, CameraFilled, Search } from '@element-plus/icons-vue'
import { foodApi } from '../api'
import { ElMessage } from 'element-plus'

const previewUrl = ref('')
const recognizing = ref(false)
const result = ref(null)
let fileData = null

const handleFile = (uploadFile) => {
  fileData = uploadFile.raw
  previewUrl.value = URL.createObjectURL(fileData)
  result.value = null
}
const recognize = async () => {
  if (!fileData) return
  recognizing.value = true
  try {
    const formData = new FormData()
    formData.append('file', fileData)
    const res = await foodApi.recognize(formData)
    if (res.data?.code === 200) result.value = res.data.data
    else ElMessage.error(res.data?.msg || '识别失败')
  } catch { ElMessage.error('识别失败，请稍后重试') }
  finally { recognizing.value = false }
}
const reset = () => { result.value = null; previewUrl.value = ''; fileData = null }
</script>

<style scoped>
.food-page { max-width: 650px; margin: 0 auto; }
.food-card {
  background: rgba(17, 24, 39, 0.5);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(51, 65, 85, 0.35);
  border-radius: 14px;
  padding: 24px;
}
.card-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 20px; padding-bottom: 16px;
  border-bottom: 1px solid rgba(51, 65, 85, 0.25);
}
.header-title { display: flex; align-items: center; gap: 8px; font-size: 16px; font-weight: 600; color: var(--text-primary); }
.ai-tag { background: rgba(0, 212, 255, 0.1) !important; border-color: rgba(0, 212, 255, 0.3) !important; color: var(--neon-blue) !important; }

.upload-zone { text-align: center; }
.uploader :deep(.el-upload-dragger) {
  background: rgba(17, 24, 39, 0.6) !important;
  border: 2px dashed rgba(51, 65, 85, 0.5) !important;
  border-radius: 14px !important;
  transition: all 0.3s !important;
}
.uploader :deep(.el-upload-dragger:hover) {
  border-color: rgba(0, 212, 255, 0.4) !important;
  background: rgba(17, 24, 39, 0.8) !important;
}
.upload-placeholder { padding: 48px 20px; }
.upload-icon-ring {
  width: 72px; height: 72px; border-radius: 50%;
  border: 1.5px solid rgba(0, 212, 255, 0.2);
  display: inline-flex; align-items: center; justify-content: center;
  margin-bottom: 14px; color: var(--neon-blue);
}
.upload-text { font-size: 15px; color: var(--text-secondary); margin-top: 8px; }
.upload-hint { font-size: 12px; color: var(--text-muted); margin-top: 6px; }
.preview-img { max-width: 100%; max-height: 350px; border-radius: 10px; object-fit: contain; }

.action-bar { margin-top: 16px; display: flex; justify-content: center; gap: 12px; }
.btn-outline {
  background: transparent !important; border: 1px solid rgba(51, 65, 85, 0.5) !important;
  color: var(--text-secondary) !important;
}
.btn-outline:hover { border-color: var(--neon-blue) !important; color: var(--neon-blue) !important; }
.recognize-btn { min-width: 160px; }

.result-header {
  display: flex; justify-content: space-between; align-items: flex-start;
  margin-bottom: 22px;
}
.result-title { font-size: 20px; margin: 0 0 4px; color: var(--text-primary); }
.result-gi { font-size: 13px; color: var(--text-muted); }

.nutrient-grid { margin-bottom: 16px; }
.nutrient-item {
  text-align: center; padding: 20px 8px; border-radius: 12px;
  background: rgba(17, 24, 39, 0.4);
  border: 1px solid rgba(51, 65, 85, 0.25);
  position: relative; overflow: hidden;
}
.nutrient-item .nutrient-ring {
  position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%);
  width: 70px; height: 70px; border-radius: 50%; border: 1px solid rgba(51, 65, 85, 0.2);
}
.calories .nutrient-ring { border-color: rgba(245, 108, 108, 0.15); }
.carbs .nutrient-ring { border-color: rgba(230, 162, 60, 0.15); }
.protein .nutrient-ring { border-color: rgba(16, 185, 129, 0.15); }
.fat .nutrient-ring { border-color: rgba(0, 212, 255, 0.15); }
.nutrient-value {
  font-family: var(--font-mono); font-size: 22px; font-weight: 700;
  color: var(--text-primary); position: relative; z-index: 1;
}
.nutrient-unit { font-size: 12px; color: var(--text-muted); position: relative; z-index: 1; }
.nutrient-label { font-size: 12px; color: var(--text-muted); margin-top: 6px; position: relative; z-index: 1; }

.suggestion-box {
  display: flex; gap: 12px; align-items: flex-start;
  padding: 14px 16px; border-radius: 10px;
  background: rgba(0, 212, 255, 0.05);
  border: 1px solid rgba(0, 212, 255, 0.15);
  margin-top: 16px;
}
.suggestion-accent { width: 3px; height: 40px; background: var(--neon-blue); border-radius: 2px; flex-shrink: 0; }
.suggestion-text { color: var(--text-secondary); font-size: 14px; line-height: 1.6; }
</style>