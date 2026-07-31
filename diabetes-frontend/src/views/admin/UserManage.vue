<template>
  <el-card header="用户管理">
    <el-input v-model="keyword" placeholder="搜索用户名或姓名" style="width:240px;margin-bottom:16px" clearable @clear="load" @keyup.enter="load" />
    <el-button type="primary" style="margin-left:12px" @click="load">搜索</el-button>
    <el-table :data="users" stripe style="margin-top:12px">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="realName" label="姓名" width="100" />
      <el-table-column label="性别" width="60">
        <template #default="s">{{ ['未知','男','女'][s.row.gender] || '未知' }}</template>
      </el-table-column>
      <el-table-column prop="age" label="年龄" width="60" />
      <el-table-column label="角色" width="80">
        <template #default="s">{{ s.row.role === 1 ? '管理员' : '用户' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="s">
          <el-tag :type="s.row.status === 1 ? 'success' : 'danger'">{{ s.row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="注册时间" width="170" />
      <el-table-column label="操作" width="140">
        <template #default="s">
          <el-button v-if="s.row.role !== 1" type="danger" size="small" @click="del(s.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page" :total="total" :page-size="10" layout="prev,pager,next" @current-change="load" style="margin-top:16px" />
  </el-card>
</template>

<script setup>
import { ref } from 'vue';
import { adminApi } from '../../api';
import { ElMessage, ElMessageBox } from 'element-plus';

const users = ref([]);
const keyword = ref('');
const page = ref(1);
const total = ref(0);

const load = async () => {
  const res = await adminApi.userList({ page: page.value, size: 10, keyword: keyword.value });
  if (res.data.code === 200) {
    users.value = res.data.data.records;
    total.value = res.data.data.total;
  }
};

const del = async (id) => {
  await ElMessageBox.confirm('确定删除该用户？', '提示', { type: 'warning' });
  await adminApi.deleteUser(id);
  ElMessage.success('删除成功');
  load();
};

load();
</script>