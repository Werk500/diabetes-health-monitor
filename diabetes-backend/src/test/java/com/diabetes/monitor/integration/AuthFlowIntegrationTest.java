package com.diabetes.monitor.integration;

import com.diabetes.monitor.common.ResultCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 认证流程集成测试
 * 职责：测试完整登录流程和登录失败场景
 */
class AuthFlowIntegrationTest extends BaseIntegrationTest {

    @Test
    void testPublicKeyResponseFormat() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/user/public-key"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> response = parseResponse(result);
        assertResponseFormat(response);
        assertSuccess(response);

        Object data = response.get("data");
        assertThat(data).isInstanceOf(String.class);
        assertThat((String) data).isNotBlank();
    }

    @Test
    void testFullLoginFlowSuccess() throws Exception {
        String token = getTestUserToken();
        assertThat(token).isNotBlank();

        MvcResult userListResult = mockMvc.perform(get("/api/user/list")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        assertSuccess(parseResponse(userListResult));

        MvcResult adminResult = mockMvc.perform(get("/api/admin/user/list")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andReturn();
        assertError(parseResponse(adminResult), ResultCode.FORBIDDEN, "权限不足，请联系管理员");
    }

    @Test
    void testLoginWithWrongPasswordFails() throws Exception {
        String encryptedPassword = encryptPassword("wrong_password");
        String loginJson = objectMapper.writeValueAsString(Map.of(
                "username", "test001",
                "password", encryptedPassword
        ));

        MvcResult result = mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertError(parseResponse(result), ResultCode.UNAUTHORIZED, "用户名或密码错误");
    }

    @Test
    void testLoginWithNonExistentUserFails() throws Exception {
        String encryptedPassword = encryptPassword("password");
        String loginJson = objectMapper.writeValueAsString(Map.of(
                "username", "nonexistent_user",
                "password", encryptedPassword
        ));

        MvcResult result = mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertError(parseResponse(result), ResultCode.UNAUTHORIZED, "用户名或密码错误");
    }

    @Test
    void testLoginWithoutUsernameFails() throws Exception {
        String loginJson = objectMapper.writeValueAsString(Map.of(
                "password", encryptPassword("password")
        ));

        MvcResult result = mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(parseResponse(result).get("code")).isEqualTo(400);
    }

    @Test
    void testLoginWithoutPasswordFails() throws Exception {
        String loginJson = objectMapper.writeValueAsString(Map.of(
                "username", "test001"
        ));

        MvcResult result = mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(parseResponse(result).get("code")).isEqualTo(400);
    }

    @Test
    void testAdminLoginFlowSuccess() throws Exception {
        String token = getAdminToken();

        mockMvc.perform(get("/api/admin/user/list")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/user/list")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
