package com.diabetes.monitor.common;


import com.diabetes.monitor.integration.BaseIntegrationTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GlobalExceptionHandler 测试类
 * 职责：测试全局异常处理器的各种异常场景
 */
public class GlobalExceptionHandlerTest extends BaseIntegrationTest {
    @Resource
    private MockMvc mockMvc;

    /**
     * 测试1：BizException(400) 返回 HTTP 400
     */
    @Test
    void testBizExceptionBadRequest() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/biz-error"))
                .andExpect(status().isBadRequest())
                .andReturn();//把结果保存到 result 变量中

        //把响应内容从 JSON 字符串转换成 Java 的 Map 对象。
        Map<String, Object> response = parseResponse(result);

        // 断言响应体格式统一为 code/msg/data
        assertResponseFormat(response);
        assertError(response,ResultCode.BAD_REQUEST);
    }

    /**
     * 测试2：BizException(401) 返回 HTTP 401
     */
    @Test
    void testBizExceptionUnauthorized() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/biz-error")
                        .param("code", "401"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        Map<String, Object> response = parseResponse(result);
        assertResponseFormat(response);
        assertError(response, ResultCode.UNAUTHORIZED);
    }

    /**
     * 测试3：BizException(403) 返回 HTTP 403
     */
    @Test
    void testBizExceptionForbidden() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/biz-error")
                        .param("code", "403"))
                .andExpect(status().isForbidden())
                .andReturn();

        Map<String, Object> response = parseResponse(result);
        assertResponseFormat(response);
        assertError(response, ResultCode.FORBIDDEN);
    }

    /**
     * 测试4：BizException(404) 返回 HTTP 404
     */
    @Test
    void testBizExceptionNotFound() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/biz-error")
                        .param("code", "404"))
                .andExpect(status().isNotFound())
                .andReturn();

        Map<String, Object> response = parseResponse(result);
        assertResponseFormat(response);
        assertError(response, ResultCode.NOT_FOUND);
    }

    /**
     * 测试5：BizException(500) 返回 HTTP 500
     */
    @Test
    void testBizExceptionInternalError() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/biz-error")
                        .param("code", "500"))
                .andExpect(status().isInternalServerError())
                .andReturn();

        Map<String, Object> response = parseResponse(result);
        assertResponseFormat(response);
        assertError(response, ResultCode.INTERNAL_ERROR);
    }

    /**
     * 测试6：参数校验失败返回 400
     */
    @Test
    void testValidationExceptionReturns400() throws Exception {
        String invalidRequest = """
                {
                    "name": "",
                    "age": -1
                }
                """;

        MvcResult result = mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andReturn();

        Map<String, Object> response = parseResponse(result);
        assertResponseFormat(response);

        // 验证错误码是400
        assertThat(response.get("code")).isEqualTo(400);

        // 验证错误信息包含字段校验信息
        String msg = (String) response.get("msg");
        assertThat(msg).contains("name");
        assertThat(msg).contains("age");
    }

    /**
     * 测试7：权限不足返回 403
     */
    @Test
    void testAccessDeniedExceptionReturns403() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/forbidden"))
                .andExpect(status().isForbidden())
                .andReturn();

        Map<String, Object> response = parseResponse(result);
        assertResponseFormat(response);
        assertError(response, ResultCode.FORBIDDEN, "权限不足，请联系管理员");
    }

    /**
     * 测试8：未知异常返回 500
     */
    @Test
    void testUnknownExceptionReturns500() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/error"))
                .andExpect(status().isInternalServerError())
                .andReturn();

        Map<String, Object> response = parseResponse(result);
        assertResponseFormat(response);
        assertError(response, ResultCode.INTERNAL_ERROR, "系统繁忙，请稍后重试");
    }

    /**
     * 测试9：ConstraintViolationException 返回 400
     */
    @Test
    void testConstraintViolationExceptionReturns400() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/constraint")
                        .param("name", ""))
                .andExpect(status().isBadRequest())
                .andReturn();

        Map<String, Object> response = parseResponse(result);
        assertResponseFormat(response);
        assertThat(response.get("code")).isEqualTo(400);
    }

    /**
     * 测试10：BindException 返回 400
     */
    @Test
    void testBindExceptionReturns400() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/bind")
                        .param("age", "abc"))  // 类型转换错误
                .andExpect(status().isBadRequest())
                .andReturn();

        Map<String, Object> response = parseResponse(result);
        assertResponseFormat(response);
        assertThat(response.get("code")).isEqualTo(400);
    }
}
