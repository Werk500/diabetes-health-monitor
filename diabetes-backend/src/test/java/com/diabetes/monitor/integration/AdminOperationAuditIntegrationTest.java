package com.diabetes.monitor.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diabetes.monitor.entity.AdminOperationLog;
import com.diabetes.monitor.entity.SysUser;
import com.diabetes.monitor.mapper.AdminOperationLogMapper;
import com.diabetes.monitor.service.AdminOperationLogService;
import com.diabetes.monitor.service.SysUserService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@DisplayName("管理员操作审计")
class AdminOperationAuditIntegrationTest extends BaseIntegrationTest {

    @Resource
    private AdminOperationLogMapper adminOperationLogMapper;

    @Resource
    private SysUserService sysUserService;

    @Resource
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("成功启用、禁用和幂等请求都会写入完整审计信息")
    void statusOperations_writeSuccessAudit() throws Exception {
        SysUser target = createUser(0);
        String adminToken = getAdminToken();

        updateStatus(target.getId(), 0, adminToken)
                .andExpect(status().isOk());
        updateStatus(target.getId(), 0, adminToken)
                .andExpect(status().isOk());

        List<AdminOperationLog> logs = adminOperationLogMapper.selectList(
                new LambdaQueryWrapper<AdminOperationLog>()
                        .eq(AdminOperationLog::getActionCode,
                                AdminOperationLogService.USER_DISABLE)
                        .eq(AdminOperationLog::getTargetId,
                                String.valueOf(target.getId()))
                        .orderByDesc(AdminOperationLog::getId)
        );

        assertThat(logs).hasSize(2);
        assertThat(logs).allSatisfy(log -> {
            assertThat(log.getSuccess()).isEqualTo(1);
            assertThat(log.getOperatorName()).isEqualTo("admin");
            assertThat(log.getTargetType()).isEqualTo("USER");
            assertThat(log.getRequestMethod()).isEqualTo("PUT");
            assertThat(log.getRequestPath())
                    .isEqualTo("/api/admin/user/" + target.getId() + "/status");
            assertThat(log.getRequestIp()).isNotBlank();
            assertThat(log.getCreateTime()).isNotNull();
        });
        assertThat(logs.get(0).getDetail()).contains("状态未变化");
        assertThat(logs.get(1).getDetail()).contains("状态由 1 变更为 0");
    }

    @Test
    @DisplayName("被保护管理员的失败操作会独立写入失败审计")
    void protectedAdminOperation_writesFailureAudit() throws Exception {
        SysUser anotherAdmin = createUser(1);

        updateStatus(anotherAdmin.getId(), 0, getAdminToken())
                .andExpect(status().isBadRequest());

        AdminOperationLog log = inNewTransaction(status -> {
            LambdaQueryWrapper<AdminOperationLog> wrapper =
                    new LambdaQueryWrapper<AdminOperationLog>()
                            .eq(AdminOperationLog::getActionCode,
                                    AdminOperationLogService.USER_DISABLE)
                            .eq(AdminOperationLog::getTargetId,
                                    String.valueOf(anotherAdmin.getId()))
                            .orderByDesc(AdminOperationLog::getId)
                            .last("LIMIT 1");
            return adminOperationLogMapper.selectOne(wrapper);
        });

        assertThat(log).isNotNull();
        assertThat(log.getSuccess()).isZero();
        assertThat(log.getOperatorName()).isEqualTo("admin");
        assertThat(log.getDetail()).contains("管理员账号受保护");
    }

    @Test
    @DisplayName("逻辑删除用户会记录删除审计")
    void deleteUser_writesSuccessAudit() throws Exception {
        SysUser target = createUser(0);
        String adminToken = getAdminToken();

        mockMvc.perform(delete("/api/admin/user/" + target.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        AdminOperationLog log = adminOperationLogMapper.selectOne(
                new LambdaQueryWrapper<AdminOperationLog>()
                        .eq(AdminOperationLog::getActionCode,
                                AdminOperationLogService.USER_DELETE)
                        .eq(AdminOperationLog::getTargetId,
                                String.valueOf(target.getId()))
                        .orderByDesc(AdminOperationLog::getId)
                        .last("LIMIT 1")
        );

        assertThat(log).isNotNull();
        assertThat(log.getSuccess()).isEqualTo(1);
        assertThat(log.getRequestMethod()).isEqualTo("DELETE");
        assertThat(log.getRequestPath())
                .isEqualTo("/api/admin/user/" + target.getId());
        assertThat(log.getDetail()).contains("逻辑删除");
    }

    @Test
    @DisplayName("审计列表支持组合筛选、时间范围和稳定倒序")
    void auditList_filtersAndOrdersRecords() throws Exception {
        String prefix = "audit_query_" + System.nanoTime();
        LocalDateTime now = LocalDateTime.now().withNano(0);

        insertAuditLog(
                AdminOperationLogService.USER_DISABLE,
                0,
                "operator-old",
                prefix + "-old",
                now.minusMinutes(120)
        );
        insertAuditLog(
                AdminOperationLogService.USER_ENABLE,
                1,
                "operator-mid",
                prefix + "-mid",
                now.minusMinutes(90)
        );
        insertAuditLog(
                AdminOperationLogService.USER_DISABLE,
                0,
                "operator-new",
                prefix + "-new",
                now.minusMinutes(30)
        );

        Map<String, Object> allData = getData(
                mockMvc.perform(get("/api/admin/audit/list")
                                .header("Authorization", "Bearer " + getAdminToken())
                                .param("keyword", prefix)
                                .param("page", "1")
                                .param("size", "10"))
                        .andExpect(status().isOk())
                        .andReturn()
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> allRecords =
                (List<Map<String, Object>>) allData.get("records");

        assertThat(allRecords).extracting(row -> row.get("targetId"))
                .containsExactly(prefix + "-new", prefix + "-mid", prefix + "-old");

        Map<String, Object> filteredData = getData(
                mockMvc.perform(get("/api/admin/audit/list")
                                .header("Authorization", "Bearer " + getAdminToken())
                                .param("keyword", prefix)
                                .param("actionCode", AdminOperationLogService.USER_DISABLE)
                                .param("success", "0")
                                .param("page", "1")
                                .param("size", "10"))
                        .andExpect(status().isOk())
                        .andReturn()
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> filteredRecords =
                (List<Map<String, Object>>) filteredData.get("records");
        assertThat(filteredRecords).hasSize(2);
        assertThat(filteredRecords).allSatisfy(row -> {
            assertThat(row.get("actionCode"))
                    .isEqualTo(AdminOperationLogService.USER_DISABLE);
            assertThat(row.get("success")).isEqualTo(0);
        });

        Map<String, Object> rangedData = getData(
                mockMvc.perform(get("/api/admin/audit/list")
                                .header("Authorization", "Bearer " + getAdminToken())
                                .param("keyword", prefix)
                                .param("startTime", now.minusMinutes(100).toString())
                                .param("endTime", now.minusMinutes(60).toString())
                                .param("page", "1")
                                .param("size", "10"))
                        .andExpect(status().isOk())
                        .andReturn()
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rangedRecords =
                (List<Map<String, Object>>) rangedData.get("records");
        assertThat(rangedRecords).hasSize(1);
        assertThat(rangedRecords.get(0).get("targetId")).isEqualTo(prefix + "-mid");
    }

    @Test
    @DisplayName("审计列表分页正确，普通用户无权查询")
    void auditList_paginatesAndRequiresAdmin() throws Exception {
        String prefix = "audit_page_" + System.nanoTime();
        LocalDateTime now = LocalDateTime.now().withNano(0);

        for (int index = 0; index < 3; index++) {
            insertAuditLog(
                    AdminOperationLogService.USER_ENABLE,
                    1,
                    "paging-admin",
                    prefix + "-" + index,
                    now.minusMinutes(index)
            );
        }

        Map<String, Object> data = getData(
                mockMvc.perform(get("/api/admin/audit/list")
                                .header("Authorization", "Bearer " + getAdminToken())
                                .param("keyword", prefix)
                                .param("page", "2")
                                .param("size", "2"))
                        .andExpect(status().isOk())
                        .andReturn()
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records =
                (List<Map<String, Object>>) data.get("records");
        assertThat(records).hasSize(1);
        assertThat(((Number) data.get("total")).intValue()).isEqualTo(3);
        assertThat(((Number) data.get("current")).intValue()).isEqualTo(2);
        assertThat(((Number) data.get("size")).intValue()).isEqualTo(2);

        mockMvc.perform(get("/api/admin/audit/list")
                        .header("Authorization", "Bearer " + getTestUserToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("非法页码、超大分页和时间范围返回 400")
    void auditList_rejectsInvalidQuery() throws Exception {
        String adminToken = getAdminToken();
        LocalDateTime now = LocalDateTime.now().withNano(0);

        mockMvc.perform(get("/api/admin/audit/list")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/admin/audit/list")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("size", "101"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/admin/audit/list")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("startTime", now.toString())
                        .param("endTime", now.minusHours(1).toString()))
                .andExpect(status().isBadRequest());
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

    private SysUser createUser(int role) {
        SysUser user = new SysUser();
        user.setUsername("audit_test_" + System.nanoTime());
        user.setPassword("123456");
        user.setRealName("审计测试用户");
        user.setRole(role);
        user.setStatus(1);
        return sysUserService.register(user);
    }

    private void insertAuditLog(
            String actionCode,
            int success,
            String operatorName,
            String targetId,
            LocalDateTime createTime
    ) {
        AdminOperationLog log = new AdminOperationLog();
        log.setOperatorId(1);
        log.setOperatorName(operatorName);
        log.setActionCode(actionCode);
        log.setTargetType("USER");
        log.setTargetId(targetId);
        log.setRequestMethod("PUT");
        log.setRequestPath("/api/admin/test/" + targetId);
        log.setRequestIp("127.0.0.1");
        log.setSuccess(success);
        log.setDetail("审计查询测试");
        log.setCreateTime(createTime);
        adminOperationLogMapper.insert(log);
    }

    private AdminOperationLog inNewTransaction(
            org.springframework.transaction.support.TransactionCallback<AdminOperationLog> callback
    ) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template.execute(callback);
    }
}
