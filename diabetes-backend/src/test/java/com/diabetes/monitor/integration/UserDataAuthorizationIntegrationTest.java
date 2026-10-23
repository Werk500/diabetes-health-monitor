package com.diabetes.monitor.integration;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diabetes.monitor.entity.HealthRecordBloodSugar;
import com.diabetes.monitor.service.HealthRecordBloodSugarService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Map;

@Transactional
@DisplayName("用户健康数据权限")
public class UserDataAuthorizationIntegrationTest extends BaseIntegrationTest{
    private static final Integer CURRENT_USER_ID = 2;

    @Resource
    private HealthRecordBloodSugarService bloodSugarService;

    @Test
    @DisplayName("用户不能读取其他用户的血糖和仪表盘")
    void userCannotReadOtherUsersData() throws Exception{
        String token = getTestUserToken();

        mockMvc.perform(// ① 执行一次模拟请求
                get("/api/record/bloodSugar/list/1")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());//断言响应状态码 = 403

        mockMvc.perform(get("/api/dashboard/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/user/info/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

    }

    @Test
    @DisplayName("普通用户不能查看用户列表")
    void normalUserCannotListUsers() throws Exception {
        String token = getTestUserToken();

        mockMvc.perform(get("/api/user/list")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("新增记录时伪造 userId 会被当前用户覆盖")
    void createRecordIgnoresForgedUserId() throws Exception {
        String token = getTestUserToken();
        LocalDateTime measureTime =
                LocalDateTime.of(2099, 12, 31, 23, 59, 59);

        String body = objectMapper.writeValueAsString(Map.of(
                "userId", 1, // ← 伪造的归属
                "bloodSugar", 12.34,
                "measureType", 1,
                "measureTime", "2099-12-31 23:59:59"
        ));


        //发请求
        mockMvc.perform(post("/api/record/bloodSugar")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        long otherUserCount = bloodSugarService.count(
                new LambdaQueryWrapper<HealthRecordBloodSugar>()
                        .eq(HealthRecordBloodSugar::getUserId, 1)
                        .eq(HealthRecordBloodSugar::getMeasureTime, measureTime));

        long currentUserCount = bloodSugarService.count(
                new LambdaQueryWrapper<HealthRecordBloodSugar>()
                        .eq(HealthRecordBloodSugar::getUserId, CURRENT_USER_ID)
                        .eq(HealthRecordBloodSugar::getMeasureTime, measureTime));

        assertThat(otherUserCount).isZero();// 伪造目标下没有记录
        assertThat(currentUserCount).isEqualTo(1);//真实用户下恰好一条
    }
}
