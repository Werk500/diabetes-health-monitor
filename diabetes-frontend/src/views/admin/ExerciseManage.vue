<template>
  <el-card header="运动类型管理">
    <el-button type="primary" @click="openAdd">新增类型</el-button>
    <el-table :data="types" stripe style="margin-top:12px">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="typeName" label="运动名称" width="120" />
      <el-table-column prop="caloriesPerHour" label="每小时消耗(kcal)" width="150" />
      <el-table-column label="强度" width="80">
        <template #default="s">
          <el-tag :type="['','success','warning','danger'][s.row.intensity]">{{ ['','低','中','高'][s.row.intensity] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="suitableFor" label="适用人群" min-width="150" />
      <el-table-column label="操作" width="160">
        <template #default="s">
          <el-button size="small" @click="openEdit(s.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="del(s.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑运动类型' : '新增运动类型'" width="500px">
    <el-form :model="form" label-width="120px">
      <el-form-item label="运动名称"><el-input v-model="form.typeName" /></el-form-item>
      <el-form-item label="每小时消耗(kcal)"><el-input-number v-model="form.caloriesPerHour" :precision="1" :min="0" /></el-form-item>
      <el-form-item label="强度">
        <el-select v-model="form.intensity">
          <el-option :value="1" label="低" /><el-option :value="2" label="中" /><el-option :value="3" label="高" />
        </el-select>
      </el-form-item>
      <el-form-item label="适用人群"><el-input v-model="form.suitableFor" /></el-form-item>
      <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="save">{{ isEdit ? '更新' : '保存' }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue';
import { adminApi } from '../../api';
import { ElMessage, ElMessageBox } from 'element-plus';

const types = ref([]);
const dialogVisible = ref(false);
const isEdit = ref(false);
const form = ref({ typeName:'',caloriesPerHour:null,intensity:1,suitableFor:'',description:'' });

const load = async () => {
  const res = await adminApi.getExerciseTypes();
  if (res.data.code === 200) types.value = res.data.data;
};

const openAdd = () => { isEdit.value=false; form.value={typeName:'',caloriesPerHour:null,intensity:1,suitableFor:'',description:''}; dialogVisible.value=true; };
const openEdit = (row) => { isEdit.value=true; form.value={...row}; dialogVisible.value=true; };

const save = async () => {
  if (isEdit.value) await adminApi.updateExerciseType(form.value);
  else await adminApi.addExerciseType(form.value);
  ElMessage.success(isEdit.value ? '更新成功' : '新增成功');
  dialogVisible.value = false;
  load();
};

const del = async (id) => {
  await ElMessageBox.confirm('确定删除？','提示',{type:'warning'});
  await adminApi.deleteExerciseType(id);
  ElMessage.success('删除成功');
  load();
};

load();
</script>