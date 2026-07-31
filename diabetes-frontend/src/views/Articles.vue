<template>
  <div>
    <el-card header="健康文章" style="margin-bottom:20px">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="血糖监测技巧" name="1" />
        <el-tab-pane label="控糖饮食指南" name="2" />
        <el-tab-pane label="并发症预防" name="3" />
        <el-tab-pane label="运动建议" name="4" />
      </el-tabs>
      <div v-if="filteredArticles.length === 0 && !loading" style="text-align:center;padding:40px;color:#909399">
        <el-icon :size="48"><Document /></el-icon>
        <p style="margin-top:12px">暂无相关文章</p>
      </div>
      <div v-loading="loading" v-for="a in filteredArticles" :key="a.id" style="border-bottom:1px solid #ebeef5;padding:16px 0;cursor:pointer" @click="openDetail(a)">
        <div style="display:flex;align-items:center;justify-content:space-between">
          <h3 style="color:#303133;margin-bottom:8px;margin-top:0">{{ a.title }}</h3>
          <el-tag v-if="a._demo" type="info" size="small" effect="plain">示例</el-tag>
        </div>
        <p style="color:#909399;font-size:13px;margin-bottom:6px">{{ a.summary }}</p>
        <span style="color:#c0c4cc;font-size:12px">{{ a.author }} · {{ a.createTime }}</span>
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" :title="currentArticle?.title" width="700px">
      <div style="white-space:pre-wrap;line-height:1.8">{{ currentArticle?.content }}</div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { adminApi } from '../api';
import { demoArticles } from '../mock/demoData';
import { getData } from '../mock/demoManager';

const activeTab = ref('1');
const articles = ref([]);
const loading = ref(false);
const detailVisible = ref(false);
const currentArticle = ref(null);

const filteredArticles = computed(() => articles.value.filter(a => a.category == activeTab.value));

const load = async () => {
  loading.value = true;
  try {
    const res = await adminApi.articleList({ page: 1, size: 100 });
    const real = (res.data.code === 200) ? (res.data.data.records || []) : [];
    real.forEach(r => r._demo = false);
    const demo = demoArticles.map(d => ({ ...d, _demo: true }));
    articles.value = getData(demo, real);
  } catch {
    articles.value = getData(demoArticles.map(d => ({ ...d, _demo: true })), []);
  } finally {
    loading.value = false;
  }
};

const openDetail = (a) => { currentArticle.value = a; detailVisible.value = true; };

onMounted(load);
</script>
