package com.diabetes.monitor.integration;


import com.diabetes.monitor.common.ResultCode;
import com.diabetes.monitor.service.AiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
/**
 * 集成测试基类
 * 提供公共配置和工具方法，避免每个测试重复编写
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")  // 使用测试配置文件
public class BaseIntegrationTest {

    @Resource
    protected MockMvc mockMvc;

    @Resource
    protected ObjectMapper objectMapper;

    @Resource
    protected WebApplicationContext webApplicationContext;

    @MockBean
    protected AiService aiService;

    /**
     * RSA公钥缓存
     */
    private String cachedPublicKey;

    @BeforeEach
    public void setup() {
        // 使用 @AutoConfigureMockMvc 注入的 MockMvc，保留 Spring Security 过滤链
    }

    /**
     * 获取 RSA 公钥
     */
    protected String getPublicKey() throws Exception {
        if (cachedPublicKey == null) {
            MvcResult result = mockMvc.perform(get("/api/user/public-key"))
                    .andExpect(status().isOk())
                    .andReturn();

            Map<String, Object> response = parseResponse(result);
            cachedPublicKey = (String) response.get("data");
        }
        return cachedPublicKey;
    }

    /**
     * 使用 RSA 公钥加密密码
     */
    protected String encryptPassword(String publicKeyStr,String password) throws Exception {
        //1.解码Base64公钥
        byte[] keyBytes = Base64.getDecoder().decode(publicKeyStr);

        //2.生成公钥对象
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        //3.使用公钥加密
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encrypted = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));

        //4.Base64编码
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * 便捷方法：加密密码（自动获取公钥）
     */
    protected String encryptPassword(String password) throws Exception {
        String publicKeyStr = getPublicKey();
        return encryptPassword(publicKeyStr,password);
    }

    /**
     * 登录并获取 Token
     */
    protected String loginAndGetToken(String username, String password) throws Exception {

        //1.获取公钥
        String publicKey = getPublicKey();

        //2.加密密码
        String encryptedPassword = encryptPassword(publicKey, password);

        //3.构建登录请求
        String loginJson = objectMapper.writeValueAsString(Map.of("username", username,
                "password", encryptedPassword));

        // 4. 发送登录请求
        MvcResult result = mockMvc.perform(post("/api/user/login")
                        .contentType("application/json")
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        //5.解析响应
        Map<String, Object> response = parseResponse(result);

        //6.从data中提取token(data是map)
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        return (String) data.get("token");

    }

    protected Map<String,Object> loginAndGetUserInfo(String username, String password) throws Exception {
        String publicKey = getPublicKey();
        String encryptedPassword = encryptPassword(publicKey, password);

        String loginJson = objectMapper.writeValueAsString(Map.of(
                "username", username,
                "password", encryptedPassword
        ));

        MvcResult result = mockMvc.perform(post("/api/user/login")
                        .contentType("application/json")
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> response = parseResponse(result);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        return data;
    }

    /**
     * 获取测试用户的 Token
     */
    protected String getTestUserToken() throws Exception {
        return loginAndGetToken("test001", "123456");
    }

    /**
     * 获取管理员 Token
     */
    protected String getAdminToken() throws Exception {
        return loginAndGetToken("admin", "123456");
    }

    // ==================== 响应解析工具方法 ====================

    /**
     * 解析响应为 Map
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> parseResponse(MvcResult result) throws Exception {
        String content = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readValue(content, Map.class);
    }

    /**
     * 从响应中提取 data
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> getData(MvcResult result) throws Exception {
        Map<String, Object> response = parseResponse(result);
        return (Map<String, Object>) response.get("data");
    }

    /**
     * 从响应中提取 token
     */
    protected String getTokenFromResponse(MvcResult result) throws Exception {
        Map<String, Object> data = getData(result);
        return (String) data.get("token");
    }

    // ==================== 断言工具方法 ====================

    /**
     * 断言响应格式统一（code/msg/data）
     */
    protected void assertResponseFormat(Map<String, Object> response) {
        assertThat(response).containsKeys("code", "msg", "data");
    }

    /**
     * 断言响应成功
     */
    protected void assertSuccess(Map<String, Object> response) {
        assertResponseFormat(response);
        assertThat(response.get("code")).isEqualTo(ResultCode.SUCCESS.getCode());
    }

    /**
     * 断言响应错误
     */
    protected void assertError(Map<String, Object> response, ResultCode expectedCode) {
        assertResponseFormat(response);
        assertThat(response.get("code")).isEqualTo(expectedCode.getCode());
        assertThat(response.get("msg")).isEqualTo(expectedCode.getMessage());
    }

    /**
     * 断言响应错误（带自定义消息）
     */
    protected void assertError(Map<String, Object> response, ResultCode expectedCode, String expectedMsg) {
        assertResponseFormat(response);
        assertThat(response.get("code")).isEqualTo(expectedCode.getCode());
        assertThat(response.get("msg")).isEqualTo(expectedMsg);
    }


}
