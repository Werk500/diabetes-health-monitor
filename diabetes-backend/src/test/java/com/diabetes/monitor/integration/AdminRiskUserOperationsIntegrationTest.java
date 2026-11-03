package com.diabetes.monitor.integration;


import com.diabetes.monitor.entity.HealthRecordBloodSugar;
import com.diabetes.monitor.entity.SysUser;
import com.diabetes.monitor.service.HealthRecordBloodSugarService;
import com.diabetes.monitor.service.SysUserService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@DisplayName("管理员风险用户分析")
public class AdminRiskUserOperationsIntegrationTest extends BaseIntegrationTest {

    @Resource
    private SysUserService sysUserService;
    @Resource
    private HealthRecordBloodSugarService bloodSugarService;

    @Test
    @DisplayName("普通用户不能访问风险用户接口")
    void riskUserApis_requireAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/risk-user/overview")
                        .header("Authorization", "Bearer " + getTestUserToken()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/risk-user/list")
                        .header("Authorization", "Bearer " + getTestUserToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("三次近期异常血糖进入高风险列表")
    void highRiskUser_isReturnedWithReasons() throws Exception{

        SysUser user = createUser();

        addBloodSugar(user.getId(), 2.9, 1, LocalDateTime.now().minusHours(3));
        addBloodSugar(user.getId(), 12.0, 3, LocalDateTime.now().minusHours(2));
        addBloodSugar(user.getId(), 14.2, 3, LocalDateTime.now().minusHours(1));

        MvcResult result = mockMvc.perform(get("/api/admin/risk-user/list")
                        .header("Authorization", "Bearer " + getAdminToken())
                        .param("keyword", user.getUsername()))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> data = getData(result);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records =
                (List<Map<String, Object>>) data.get("records");

        assertThat(((Number) data.get("total")).intValue()).isEqualTo(1);
        assertThat(records).hasSize(1);

        Map<String, Object> row = records.get(0);
        assertThat(((Number) row.get("riskLevel")).intValue()).isEqualTo(1);
        assertThat(((Number) row.get("riskScore")).intValue()).isGreaterThanOrEqualTo(60);
        assertThat(((Number) row.get("abnormalCount")).intValue()).isEqualTo(3);
        assertThat((String) row.get("riskReasons"))
                .contains("异常血糖")
                .contains("严重高血糖")
                .contains("低血糖");



    }

    @Test
    @DisplayName("从未记录用户进入持续观察")
    void neverMeasuredUser_isObservation() throws Exception {
        SysUser user = createUser();

        MvcResult result = mockMvc.perform(get("/api/admin/risk-user/list")
                        .header("Authorization", "Bearer " + getAdminToken())
                        .param("keyword", user.getUsername()))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> data = getData(result);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records =
                (List<Map<String, Object>>) data.get("records");

        assertThat(records).hasSize(1);

        Map<String, Object> row = records.get(0);
        assertThat(((Number) row.get("riskLevel")).intValue()).isEqualTo(3);
        assertThat(((Number) row.get("riskScore")).intValue()).isEqualTo(20);
        assertThat(row.get("lastMeasureTime")).isNull();
        assertThat((String) row.get("riskReasons"))
                .contains("从未记录血糖");
    }

    @Test
    @DisplayName("正常活跃用户不进入风险列表")
    void normalActiveUser_isExcluded() throws Exception {
        SysUser user = createUser();
        addBloodSugar(user.getId(), 5.5, 1, LocalDateTime.now());

        MvcResult result = mockMvc.perform(get("/api/admin/risk-user/list")
                        .header("Authorization", "Bearer " + getAdminToken())
                        .param("keyword", user.getUsername()))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> data = getData(result);

        assertThat(((Number) data.get("total")).intValue()).isZero();
    }

    @Test
    @DisplayName("风险概览与等级筛选使用同一评分口径")
    void overviewAndRiskLevelFilter_areConsistent() throws Exception {
        Map<String, Object> before = getOverview();

        SysUser highRiskUser = createUser();
        addBloodSugar(highRiskUser.getId(), 2.9, 1, LocalDateTime.now().minusHours(3));
        addBloodSugar(highRiskUser.getId(), 12.0, 3, LocalDateTime.now().minusHours(2));
        addBloodSugar(highRiskUser.getId(), 14.2, 3, LocalDateTime.now().minusHours(1));

        SysUser mediumRiskUser = createUser();
        addBloodSugar(mediumRiskUser.getId(), 7.5, 1, LocalDateTime.now().minusHours(2));
        addBloodSugar(mediumRiskUser.getId(), 12.0, 3, LocalDateTime.now().minusHours(1));

        createUser();

        Map<String, Object> after = getOverview();

        assertThat(number(after, "highRiskUsers"))
                .isEqualTo(number(before, "highRiskUsers") + 1);
        assertThat(number(after, "mediumRiskUsers"))
                .isEqualTo(number(before, "mediumRiskUsers") + 1);
        assertThat(number(after, "observationUsers"))
                .isEqualTo(number(before, "observationUsers") + 1);
        assertThat(number(after, "inactiveUsers"))
                .isEqualTo(number(before, "inactiveUsers") + 1);

        MvcResult result = mockMvc.perform(get("/api/admin/risk-user/list")
                        .header("Authorization", "Bearer " + getAdminToken())
                        .param("keyword", mediumRiskUser.getUsername())
                        .param("riskLevel", "2"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> data = getData(result);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records =
                (List<Map<String, Object>>) data.get("records");

        assertThat(((Number) data.get("total")).intValue()).isEqualTo(1);
        assertThat(records).hasSize(1);
        assertThat(((Number) records.get(0).get("riskLevel")).intValue()).isEqualTo(2);
        assertThat(((Number) records.get(0).get("riskScore")).intValue()).isEqualTo(40);
    }

    private Map<String, Object> getOverview() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/risk-user/overview")
                        .header("Authorization", "Bearer " + getAdminToken()))
                .andExpect(status().isOk())
                .andReturn();
        return getData(result);
    }

    private long number(Map<String, Object> data, String key) {
        return ((Number) data.get(key)).longValue();
    }

    private SysUser createUser() {
        SysUser user = new SysUser();
        user.setUsername("risk_test_" + System.nanoTime());
        user.setPassword("123456");
        user.setRealName("风险测试用户");
        user.setPhone("13800000000");
        user.setRole(0);
        user.setStatus(1);
        return sysUserService.register(user);
    }

    private void addBloodSugar(
            Integer userId,
            double value,
            int measureType,
            LocalDateTime measureTime
    ) {
        HealthRecordBloodSugar record = new HealthRecordBloodSugar();
        record.setUserId(userId);
        record.setBloodSugar(value);
        record.setMeasureType(measureType);
        record.setMeasureTime(measureTime);
        bloodSugarService.save(record);
    }
}
