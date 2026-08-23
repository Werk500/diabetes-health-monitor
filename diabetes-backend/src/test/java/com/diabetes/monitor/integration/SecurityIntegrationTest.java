package com.diabetes.monitor.integration;

import com.diabetes.monitor.common.ResultCode;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security 集成测试
 * 职责：测试未登录和权限不足的场景
 */
class SecurityIntegrationTest extends BaseIntegrationTest {

    @Test
    void testUnauthorizedAccessReturns401() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/ai/chat"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        Map<String, Object> response = parseResponse(result);
        assertResponseFormat(response);
        assertError(response, ResultCode.UNAUTHORIZED);
        assertThat(result.getResponse().getContentType()).contains("application/json");
    }

    @Test
    void testUnauthorizedAccessToSseReturns401() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/ai/chat/stream"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        Map<String, Object> response = parseResponse(result);
        assertResponseFormat(response);
        assertError(response, ResultCode.UNAUTHORIZED);
    }

    @Test
    void testUserAccessAdminEndpointReturns403() throws Exception {
        String token = getTestUserToken();

        MvcResult result = mockMvc.perform(get("/api/admin/user/list")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andReturn();

        Map<String, Object> response = parseResponse(result);
        assertResponseFormat(response);
        assertError(response, ResultCode.FORBIDDEN, "权限不足，请联系管理员");
        assertThat(result.getResponse().getContentType()).contains("application/json");
    }

    @Test
    void testUserAccessAdminStatsReturns403() throws Exception {
        String token = getTestUserToken();

        MvcResult result = mockMvc.perform(get("/api/admin/stats")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andReturn();

        Map<String, Object> response = parseResponse(result);
        assertResponseFormat(response);
        assertError(response, ResultCode.FORBIDDEN, "权限不足，请联系管理员");
    }

    @Test
    void testAdminAccessAdminEndpointSuccess() throws Exception {
        String token = getAdminToken();

        mockMvc.perform(get("/api/admin/user/list")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/user/list")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void testUnauthorizedAccessProtectedEndpointReturns401() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/user/list"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        Map<String, Object> response = parseResponse(result);
        assertResponseFormat(response);
        assertError(response, ResultCode.UNAUTHORIZED);
    }

    @Test
    void testInvalidTokenReturns401() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/user/list")
                        .header("Authorization", "Bearer invalid_token"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        Map<String, Object> response = parseResponse(result);
        assertResponseFormat(response);
        assertError(response, ResultCode.UNAUTHORIZED);
    }

    @Test
    void testExpiredTokenReturns401() throws Exception {
        String expiredToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE2MDAwMDAwMDB9.signature";

        MvcResult result = mockMvc.perform(get("/api/user/list")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andReturn();

        Map<String, Object> response = parseResponse(result);
        assertResponseFormat(response);
        assertError(response, ResultCode.UNAUTHORIZED);
    }
}
