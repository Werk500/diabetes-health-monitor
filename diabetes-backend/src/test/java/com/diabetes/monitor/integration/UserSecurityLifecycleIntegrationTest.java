package com.diabetes.monitor.integration;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@DisplayName("用户安全生命周期")
class UserSecurityLifecycleIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("修改密码后旧 Refresh Token 失效，新密码可以登录")
    void changePassword_revokesSessionsAndUpdatesPassword() throws Exception {
        MvcResult loginResult = loginRaw("test001", "123456");
        Cookie oldRefreshCookie = loginResult.getResponse()
                .getCookie("refresh_token");
        String accessToken = getTokenFromResponse(loginResult);

        String body = objectMapper.writeValueAsString(Map.of(
                "oldPassword", encryptPassword("123456"),
                "newPassword", encryptPassword("new_password_123")
        ));

        mockMvc.perform(put("/api/user/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/user/refresh")
                        .cookie(oldRefreshCookie))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("test001", "new_password_123")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("test001", "123456")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("管理员禁用用户后该用户会话失效，普通用户不能调用禁用接口")
    void disableUser_revokesSessionsAndRequiresAdmin() throws Exception {
        MvcResult userLogin = loginRaw("user002", "123456");
        Cookie userCookie = userLogin.getResponse().getCookie("refresh_token");
        String adminToken = getAdminToken();
        String normalUserToken = getTestUserToken();

        mockMvc.perform(put("/api/user/status/7")
                        .header("Authorization", "Bearer " + normalUserToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/user/status/7")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        assertThat(userCookie).isNotNull();
        mockMvc.perform(post("/api/user/refresh")
                        .cookie(userCookie))
                .andExpect(status().isUnauthorized());
    }

    private MvcResult loginRaw(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(username, password)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private String loginJson(String username, String password) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "username", username,
                "password", encryptPassword(password)
        ));
    }
}
