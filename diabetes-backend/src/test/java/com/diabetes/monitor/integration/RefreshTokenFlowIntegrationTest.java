package com.diabetes.monitor.integration;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("短长 Token 接口流程")
class RefreshTokenFlowIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("登录、刷新轮换、旧 Token 重放、退出失效")
    void loginRefreshLogoutFlow() throws Exception {
        String encryptedPassword = encryptPassword("123456");
        String loginJson = objectMapper.writeValueAsString(Map.of(
                "username", "test001",
                "password", encryptedPassword
        ));

        MvcResult loginResult = mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        Cookie oldRefreshCookie = loginResult.getResponse()
                .getCookie("refresh_token");
        String oldAccessToken = getTokenFromResponse(loginResult);

        assertThat(oldRefreshCookie).isNotNull();
        assertThat(oldRefreshCookie.isHttpOnly()).isTrue();
        assertThat(oldRefreshCookie.getPath()).isEqualTo("/api/user");
        assertThat(loginResult.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                .contains("SameSite=Lax");

        Map<String, Object> loginData = getData(loginResult);
        assertThat(loginData).doesNotContainKey("refreshToken");

        @SuppressWarnings("unchecked")
        Map<String, Object> loginUser =
                (Map<String, Object>) loginData.get("user");
        assertThat(loginUser).doesNotContainKey("password");

        mockMvc.perform(get("/api/user/info/2")
                        .header("Authorization", "Bearer " + oldAccessToken))
                .andExpect(status().isOk());

        MvcResult refreshResult = mockMvc.perform(post("/api/user/refresh")
                        .cookie(oldRefreshCookie))
                .andExpect(status().isOk())
                .andReturn();

        Cookie newRefreshCookie = refreshResult.getResponse()
                .getCookie("refresh_token");
        String newAccessToken = getTokenFromResponse(refreshResult);

        assertThat(newRefreshCookie).isNotNull();
        assertThat(newRefreshCookie.getValue())
                .isNotEqualTo(oldRefreshCookie.getValue());

        mockMvc.perform(get("/api/user/info/2")
                        .header("Authorization", "Bearer " + newAccessToken))
                .andExpect(status().isOk());

        // 旧 Refresh Token 已原子消费，重放必须失败
        MvcResult replayResult = mockMvc.perform(post("/api/user/refresh")
                        .cookie(oldRefreshCookie))
                .andExpect(status().isUnauthorized())
                .andReturn();
        assertThat(parseResponse(replayResult).get("code"))
                .isEqualTo(401);

        mockMvc.perform(post("/api/user/logout")
                        .cookie(newRefreshCookie))
                .andExpect(status().isOk());

        MvcResult logoutRefreshResult = mockMvc.perform(post("/api/user/refresh")
                        .cookie(newRefreshCookie))
                .andExpect(status().isUnauthorized())
                .andReturn();
        assertThat(parseResponse(logoutRefreshResult).get("code"))
                .isEqualTo(401);
    }
}
