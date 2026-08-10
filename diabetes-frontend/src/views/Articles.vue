<template>
  <div class="articles-page">
    <el-card shadow="hover" v-loading="loading">
      <template #header>
        <div class="card-header">
          <el-icon :size="20" color="#409eff"><Document /></el-icon>
          <span>健康文章</span>
        </div>
      </template>

      <el-tabs v-model="activeTab" class="article-tabs">
        <el-tab-pane label="血糖监测技巧" name="1" />
        <el-tab-pane label="控糖饮食指南" name="2" />
        <el-tab-pane label="并发症预防" name="3" />
        <el-tab-pane label="运动建议" name="4" />
      </el-tabs>

      <el-empty v-if="filteredArticles.length === 0 && !loading" description="暂无相关文章" />

      <div
        v-for="a in filteredArticles"
        :key="a.id"
        class="article-item"
        @click="openDetail(a)"
      >
        <div class="article-top">
          <h3>{{ a.title }}</h3>
          <el-tag v-if="a._demo" type="info" size="small">示例</el-tag>
        </div>
        <p class="article-summary">{{ a.summary }}</p>
        <div class="article-meta">
          <span>{{ a.author }}</span>
          <span>{{ a.createTime }}</span>
        </div>
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" :title="currentArticle?.title" width="700px" destroy-on-close>
      <div class="article-detail">{{ currentArticle?.content }}</div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Document } from '@element-plus/icons-vue'
import { userApi } from '../api'
import { demoArticles } from '../mock/demoData'
import { getData } from '../mock/demoManager'

const activeTab = ref('1')
const articles = ref([])
const loading = ref(false)
const detailVisible = ref(false)
const currentArticle = ref(null)

const filteredArticles = computed(() => articles.value.filter(a => a.category == activeTab.value))

const load = async () => {
  loading.value = true
  try {
    const res = await userApi.articleList({ page: 1, size: 100 })
    const real = (res.data.code === 200) ? (res.data.data.records || []) : []
    real.forEach(r => r._demo = false)
    const demo = demoArticles.map(d => ({ ...d, _demo: true }))
    articles.value = getData(demo, real)
  } catch {
    articles.value = getData(demoArticles.map(d => ({ ...d, _demo: true })), [])
  } finally { loading.value = false }
}

const openDetail = (a) => { currentArticle.value = a; detailVisible.value = true }

onMounted(load)
</script>

<style scoped>
.articles-page { max-width: 900px; margin: 0 auto; }
.card-header { display: flex; align-items: center; gap: 8px; font-size: 16px; font-weight: 600; }

.article-tabs { margin-bottom: 8px; }

.article-item {
  padding: 18px 0;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  transition: background 0.2s;
}
.article-item:hover { background: rgba(30,41,59,0.5); padding-left: 8px; border-radius: 6px; }
.article-item:last-child { border-bottom: none; }
.article-top { display: flex; align-items: center; justify-content: space-between; }
.article-top h3 { margin: 0 0 8px; color: #e2e8f0; font-size: 16px; }
.article-summary { color: #94a3b8; font-size: 13px; margin-bottom: 8px; line-height: 1.6; }
.article-meta { display: flex; gap: 16px; color: #64748b; font-size: 12px; }

.article-detail { white-space: pre-wrap; line-height: 1.9; font-size: 14px; color: #e2e8f0; }
</style>