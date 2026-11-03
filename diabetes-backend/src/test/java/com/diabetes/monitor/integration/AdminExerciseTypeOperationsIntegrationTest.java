package com.diabetes.monitor.integration;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diabetes.monitor.entity.HealthRecordExercise;
import com.diabetes.monitor.entity.SysExerciseType;
import com.diabetes.monitor.entity.SysUser;
import com.diabetes.monitor.service.HealthRecordExerciseService;
import com.diabetes.monitor.service.SysExerciseTypeService;
import com.diabetes.monitor.service.SysUserService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@DisplayName("管理员运动配置")
public class AdminExerciseTypeOperationsIntegrationTest extends BaseIntegrationTest {

    @Resource
    private SysExerciseTypeService exerciseTypeService;

    @Resource
    private HealthRecordExerciseService exerciseRecordService;

    @Resource
    private SysUserService sysUserService;

    @Test
    @DisplayName("普通用户不能访问运动配置管理接口")
    void exerciseTypeAdminApis_requireAdmin() throws  Exception{
        mockMvc.perform(get("/api/admin/exerciseType/list")
                .header("Authorization", "Bearer " + getTestUserToken()))
                .andExpect(status().isForbidden());


    }


    @Test
    @DisplayName("关键词、强度和状态筛选正确")
    void exerciseTypeList_filterByKeywordIntensityAndStatus()  throws  Exception{
        String prefix = "exercise_query_" + System.nanoTime();

        SysExerciseType first = createType(prefix + "_散步", 1, 1);
        SysExerciseType second = createType(
                "关节友好运动_" + System.nanoTime(),
                2,
                0
        );

        second.setSuitableFor(prefix+"_适合关节不适人群");
        exerciseTypeService.updateById(second);

        MvcResult keywordResult = mockMvc.perform(
                        get("/api/admin/exerciseType/list")
                                .header("Authorization", "Bearer " + getAdminToken())
                                .param("keyword", prefix)
                                .param("page", "1")
                                .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> keywordData = getData(keywordResult);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> keywordRecords =
                (List<Map<String, Object>>) keywordData.get("records");

        assertThat(((Number)keywordData.get("total")).intValue()).isEqualTo(2);
        assertThat(keywordRecords).hasSize(2);

        MvcResult filteredResult = mockMvc.perform(
                        get("/api/admin/exerciseType/list")
                                .header("Authorization", "Bearer " + getAdminToken())
                                .param("keyword", prefix)
                                .param("intensity", "1")
                                .param("status", "1")
                                .param("page", "1")
                                .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> filteredData = getData(filteredResult);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> filteredRecords =
                (List<Map<String, Object>>) filteredData.get("records");

        assertThat(((Number) filteredData.get("total")).intValue()).isEqualTo(1);
        assertThat(filteredRecords.get(0).get("id")).isEqualTo(first.getId());

    }

    @Test
    @DisplayName("非法运动配置返回400，同名类型不能重复创建")
    void createExerciseType_validatesInputAndDuplicateName() throws Exception {
        Map<String, Object> invalidBody = Map.of(
                "typeName", "",
                "caloriesPerHour", -1,
                "intensity", 99,
                "suitableFor", "测试",
                "description", "测试",
                "status", 1
        );

        mockMvc.perform(post("/api/admin/exerciseType")
                        .header("Authorization", "Bearer " + getAdminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidBody)))
                .andExpect(status().isBadRequest());

        String name = "重复类型_" + System.nanoTime();
        createType(name, 1, 1);

        mockMvc.perform(post("/api/admin/exerciseType")
                        .header("Authorization", "Bearer " + getAdminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                createPayload(name, 2, 1)
                        )))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("更新和重复设置状态可以正确落库")
    void updateAndUpdateStatus_persistChanges() throws Exception{
        SysExerciseType type = createType("更新前_" + System.nanoTime(), 1, 1);

        String newName = "更新后_" + System.nanoTime();
        mockMvc.perform(put("/api/admin/exerciseType/"+type.getId())
                .header("Authorization","Bearer "+getAdminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        createPayload(newName, 2, 1)))
        ).andExpect(status().isOk());

        SysExerciseType updated = exerciseTypeService.getById(type.getId());
        assertThat(updated.getTypeName()).isEqualTo(newName);
        assertThat(updated.getIntensity()).isEqualTo(2);

        mockMvc.perform(put("/api/admin/exerciseType/" + type.getId() + "/status")
                        .header("Authorization", "Bearer " + getAdminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody(0)))
                .andExpect(status().isOk());

        //再次设为 0（幂等验证）
        mockMvc.perform(put("/api/admin/exerciseType/" + type.getId() + "/status")
                        .header("Authorization", "Bearer " + getAdminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody(0)))
                .andExpect(status().isOk());

        assertThat(exerciseTypeService.getById(type.getId()).getStatus()).isZero();

        mockMvc.perform(put("/api/admin/exerciseType/" + type.getId() + "/status")
                        .header("Authorization", "Bearer " + getAdminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody(2)))
                .andExpect(status().isBadRequest());

    }

    @Test
    @DisplayName("被运动记录引用的类型不能删除，未引用类型可以删除")
    void deleteExerciseType_checksReferences() throws Exception {
        SysExerciseType referenced = createType(
                "被引用_" + System.nanoTime(),
                1,
                1
        );

        SysUser testUser = sysUserService.getOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, "test001")
        );
        assertThat(testUser).isNotNull();

        HealthRecordExercise record = new HealthRecordExercise();
        record.setUserId(testUser.getId());
        record.setExerciseTypeId(referenced.getId());
        record.setDurationMinutes(30);
        record.setCaloriesBurned(150.0);
        record.setExerciseDate(LocalDate.now());
        exerciseRecordService.save(record);

        mockMvc.perform(delete("/api/admin/exerciseType/" + referenced.getId())
                        .header("Authorization", "Bearer " + getAdminToken()))
                .andExpect(status().isBadRequest());

        assertThat(exerciseTypeService.getById(referenced.getId())).isNotNull();

        SysExerciseType unused = createType(
                "未引用_" + System.nanoTime(),

                1,
                1
        );

        mockMvc.perform(delete("/api/admin/exerciseType/" + unused.getId())
                        .header("Authorization", "Bearer " + getAdminToken()))
                .andExpect(status().isOk());

        assertThat(exerciseTypeService.getById(unused.getId())).isNull();
    }


    private SysExerciseType createType(
            String name,
            int intensity,
            int typeStatus
    ) {
        SysExerciseType type = new SysExerciseType();
        type.setTypeName(name);
        type.setCaloriesPerHour(200.0);
        type.setIntensity(intensity);
        type.setSuitableFor("测试人群");
        type.setDescription("测试运动类型");
        type.setStatus(typeStatus);
        exerciseTypeService.save(type);
        return type;
    }

    private Map<String, Object> createPayload(
            String name,
            int intensity,
            int typeStatus
    ) {
        return Map.of(
                "typeName", name,
                "caloriesPerHour", 300.0,
                "intensity", intensity,
                "suitableFor", "测试适用人群",
                "description", "测试描述",
                "status", typeStatus
        );
    }

    private String statusBody(int newStatus) throws Exception {
        return objectMapper.writeValueAsString(Map.of("status", newStatus));
    }
}
