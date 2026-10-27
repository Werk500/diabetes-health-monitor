package com.diabetes.monitor.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diabetes.monitor.common.cache.AuthRedisKeys;
import com.diabetes.monitor.entity.HealthRecordBloodSugar;
import com.diabetes.monitor.entity.SysUser;
import com.diabetes.monitor.service.HealthRecordBloodSugarService;
import com.diabetes.monitor.service.SysUserService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@DisplayName("管理员用户治理")
class AdminUserGovernanceIntegrationTest extends BaseIntegrationTest {

    @Resource
    private SysUserService sysUserService;

    @Resource
    private HealthRecordBloodSugarService bloodSugarService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("启用状态接口幂等，重复禁用不重复吊销会话")
    void updateStatus_isIdempotent() throws Exception {
        SysUser target = createUser(0);
        String adminToken = getAdminToken();

        updateStatus(target.getId(), 0, adminToken)
                .andExpect(status().isOk());

        long versionAfterFirstDisable = tokenVersion(target.getId());
        assertThat(versionAfterFirstDisable).isGreaterThan(0);

        updateStatus(target.getId(), 0, adminToken)
                .andExpect(status().isOk());

        assertThat(tokenVersion(target.getId()))
                .isEqualTo(versionAfterFirstDisable);
    }

    @Test
    @DisplayName("非法状态返回 400，普通用户不能调用管理员状态接口")
    void updateStatus_validatesInputAndRequiresAdmin() throws Exception {
        SysUser target = createUser(0);

        updateStatus(target.getId(), 2, getAdminToken())
                .andExpect(status().isBadRequest());

        updateStatus(target.getId(), 0, getTestUserToken())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("当前管理员和其他管理员都不能被启停或删除")
    void adminAccountsAreProtected() throws Exception {
        String adminToken = getAdminToken();
        SysUser currentAdmin = sysUserService.getOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, "admin")
        );
        SysUser anotherAdmin = createUser(1);

        assertThat(currentAdmin).isNotNull();

        updateStatus(currentAdmin.getId(), 0, adminToken)
                .andExpect(status().isBadRequest());

        updateStatus(anotherAdmin.getId(), 0, adminToken)
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/api/admin/user/" + anotherAdmin.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("禁用和逻辑删除都会吊销会话，删除后健康记录仍然保留")
    void disableAndDeleteInvalidateTokensAndPreserveHealthRecords() throws Exception {
        SysUser target = createUser(0);
        String adminToken = getAdminToken();

        HealthRecordBloodSugar record = new HealthRecordBloodSugar();
        record.setUserId(target.getId());
        record.setBloodSugar(6.2);
        record.setMeasureType(1);
        record.setMeasureTime(LocalDateTime.now());
        bloodSugarService.save(record);

        String firstAccessToken = loginAndGetToken(target.getUsername(), "123456");

        updateStatus(target.getId(), 0, adminToken)
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/user/info/" + target.getId())
                        .header("Authorization", "Bearer " + firstAccessToken))
                .andExpect(status().isUnauthorized());

        updateStatus(target.getId(), 1, adminToken)
                .andExpect(status().isOk());

        String renewedAccessToken = loginAndGetToken(target.getUsername(), "123456");

        mockMvc.perform(delete("/api/admin/user/" + target.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/user/info/" + target.getId())
                        .header("Authorization", "Bearer " + renewedAccessToken))
                .andExpect(status().isUnauthorized());

        Integer deleted = jdbcTemplate.queryForObject(
                "SELECT deleted FROM sys_user WHERE id = ?",
                Integer.class,
                target.getId()
        );
        Integer recordCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM health_record_blood_sugar WHERE user_id = ?",
                Integer.class,
                target.getId()
        );

        assertThat(deleted).isEqualTo(1);
        assertThat(recordCount).isEqualTo(1);
        assertThat(sysUserService.getById(target.getId())).isNull();
    }

    private SysUser createUser(int role) {
        SysUser user = new SysUser();
        user.setUsername("governance_test_" + System.nanoTime());
        user.setPassword("123456");
        user.setRealName("治理测试用户");
        user.setRole(role);
        user.setStatus(1);
        return sysUserService.register(user);
    }

    private org.springframework.test.web.servlet.ResultActions updateStatus(
            Integer targetId,
            int targetStatus,
            String token
    ) throws Exception {
        return mockMvc.perform(put("/api/admin/user/" + targetId + "/status")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", targetStatus))));
    }

    private long tokenVersion(Integer userId) {
        String value = stringRedisTemplate.opsForValue()
                .get(AuthRedisKeys.userVersion(userId));
        return value == null ? 0L : Long.parseLong(value);
    }
}
