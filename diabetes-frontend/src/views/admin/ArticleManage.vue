<template>
  <div>
    <el-card header="文章管理">
      <el-button type="primary" @click="openAdd">新增文章</el-button>
      <el-table :data="articles" stripe style="margin-top:12px">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column label="分类" width="120">
          <template #default="s">{{ ['','血糖监测','控糖饮食','并发症预防','运动建议'][s.row.category] }}</template>
        </el-table-column>
        <el-table-column prop="author" label="作者" width="100" />
        <el-table-column label="推送状态" width="100">
          <template #default="s">
            <el-tag :type="s.row.pushStatus === 1 ? 'success' : 'info'">{{ s.row.pushStatus === 1 ? '已推送' : '未推送' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览" width="70" />
        <el-table-column label="操作" width="220">
          <template #default="s">
            <el-button size="small" @click="openEdit(s.row)">编辑</el-button>
            <el-button v-if="s.row.pushStatus !== 1" size="small" type="success" @click="push(s.row.id)">推送</el-button>
            <el-button size="small" type="danger" @click="del(s.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" :total="total" :page-size="10" layout="prev,pager,next" @current-change="load" style="margin-top:16px" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑文章' : '新增文章'" width="600px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="摘要"><el-input v-model="form.summary" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category">
            <el-option :value="1" label="血糖监测技巧" /><el-option :value="2" label="控糖饮食指南" />
            <el-option :value="3" label="并发症预防" /><el-option :value="4" label="运动建议" />
          </el-select>
        </el-form-item>
        <el-form-item label="作者"><el-input v-model="form.author" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="form.content" type="textarea" :rows="6" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">{{ isEdit ? '更新' : '保存' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { adminApi } from '../../api';
import { ElMessage, ElMessageBox } from 'element-plus';

const articles = ref([]);
const page = ref(1);
const total = ref(0);
const dialogVisible = ref(false);
const isEdit = ref(false);
const form = ref({ title: '', summary: '', category: 1, author: '', content: '' });

const load = async () => {
  const res = await adminApi.articleList({ page: page.value, size: 10 });
  if (res.data.code === 200) { articles.value = res.data.data.records; total.value = res.data.data.total; }
};

const openAdd = () => { isEdit.value = false; form.value = { title:'',summary:'',category:1,author:'',content:''}; dialogVisible.value = true; };
const openEdit = (row) => { isEdit.value = true; form.value = {...row}; dialogVisible.value = true; };

const save = async () => {
  if (isEdit.value) await adminApi.updateArticle(form.value);
  else await adminApi.addArticle(form.value);
  ElMessage.success(isEdit.value ? '更新成功' : '新增成功');
  dialogVisible.value = false;
  load();
};

const push = async (id) => {
  await adminApi.pushArticle(id);
  ElMessage.success('推送成功');
  load();
};

const del = async (id) => {
  await ElMessageBox.confirm('确定删除？','提示',{type:'warning'});
  await adminApi.deleteArticle(id);
  ElMessage.success('删除成功');
  load();
};

load();
</script>