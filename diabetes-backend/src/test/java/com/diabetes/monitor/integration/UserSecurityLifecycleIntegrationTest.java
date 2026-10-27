package com.diabetes.monitor.integration;

import com.diabetes.monitor.entity.SysUser;
import com.diabetes.monitor.service.SysUserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@DisplayName("用户安全生命周期")
class UserSecurityLifecycleIntegrationTest extends BaseIntegrationTest {

    @Resource
    private SysUserService sysUserService;

    @Test
    @DisplayName("修改密码后旧 Access/Refresh Token 均失效，新密码可以重新登录")
    void changePassword_revokesSessionsAndUpdatesPassword() throws Exception {
        MvcResult loginResult = loginRaw("test001", "123456");
        Cookie oldRefreshCookie = loginResult.getResponse()
                .getCookie("refresh_token");
        String oldAccessToken = getTokenFromResponse(loginResult);
        Integer userId = extractUserId(loginResult);

        String body = objectMapper.writeValueAsString(Map.of(
                "oldPassword", encryptPassword("123456"),
                "newPassword", encryptPassword("new_password_123")
        ));

        mockMvc.perform(put("/api/user/password")
                        .header("Authorization", "Bearer " + oldAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // Access Token 不依赖 Refresh Token，必须由 JWT 版本校验单独拦截。
        mockMvc.perform(get("/api/user/info/" + userId)
                        .header("Authorization", "Bearer " + oldAccessToken))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/user/refresh")
                        .cookie(oldRefreshCookie))
                .andExpect(status().isUnauthorized());

        MvcResult newLoginResult = mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("test001", "new_password_123")))
                .andExpect(status().isOk())
                .andReturn();

        // revokeAll 后的新登录必须拿到新版本 Token，否则会出现“登录成功但请求全 401”。
        mockMvc.perform(get("/api/user/info/" + userId)
                        .header("Authorization", "Bearer " + getTokenFromResponse(newLoginResult)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("test001", "123456")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("管理员禁用用户后旧 Access/Refresh Token 均失效，普通用户不能调用禁用接口")
    void disableUser_revokesSessionsAndRequiresAdmin() throws Exception {
        SysUser targetUser = new SysUser();
        targetUser.setUsername("disable_test_" + System.nanoTime());
        targetUser.setPassword("123456");
        targetUser.setRealName("禁用测试用户");
        targetUser.setRole(0);
        targetUser.setStatus(1);
        sysUserService.register(targetUser);

        MvcResult userLogin = loginRaw(targetUser.getUsername(), "123456");
        Cookie userCookie = userLogin.getResponse().getCookie("refresh_token");
        String oldUserAccessToken = getTokenFromResponse(userLogin);
        String adminToken = getAdminToken();
        String normalUserToken = getTestUserToken();

        mockMvc.perform(put("/api/admin/user/" + targetUser.getId() + "/status")
                        .header("Authorization", "Bearer " + normalUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusJson(0)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/admin/user/" + targetUser.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusJson(0)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/user/info/" + targetUser.getId())
                        .header("Authorization", "Bearer " + oldUserAccessToken))
                .andExpect(status().isUnauthorized());

        assertThat(userCookie).isNotNull();
        mockMvc.perform(post("/api/user/refresh")
                        .cookie(userCookie))
                .andExpect(status().isUnauthorized());
    }

    @SuppressWarnings("unchecked")
    private Integer extractUserId(MvcResult loginResult) throws Exception {
        Map<String, Object> data = getData(loginResult);
        Map<String, Object> user = (Map<String, Object>) data.get("user");
        return ((Number) user.get("id")).intValue();
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

    private String statusJson(int status) throws Exception {
        return objectMapper.writeValueAsString(Map.of("status", status));
    }
}
