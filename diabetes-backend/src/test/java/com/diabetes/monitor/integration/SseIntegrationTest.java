package com.diabetes.monitor.integration;

import com.diabetes.monitor.common.ResultCode;
import com.diabetes.monitor.common.SseEmitterUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SSE 集成测试
 * 职责：测试未登录和 SSE 错误事件格式
 */
class SseIntegrationTest extends BaseIntegrationTest {

    @Test
    void testUnauthorizedAccessSseReturns401() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/ai/chat/stream")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isUnauthorized())
                .andReturn();

        Map<String, Object> response = parseResponse(result);
        assertResponseFormat(response);
        assertError(response, ResultCode.UNAUTHORIZED);
    }

    @Test
    void testEmptyMessageReturnsErrorEvent() throws Exception {
        when(aiService.aiChatStream(any())).thenReturn(SseEmitterUtils.error(400, "请输入问题"));

        MvcResult asyncResult = performSseRequest("");

        assertThat(asyncResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .contains("event:error")
                .contains("\"code\":400")
                .contains("请输入问题");
    }

    @Test
    void testNormalMessageReturnsDataEvents() throws Exception {
        SseEmitter emitter = new SseEmitter();
        emitter.send(SseEmitter.event().data("{\"code\":200,\"msg\":\"success\",\"data\":\"hello\"}"));
        emitter.complete();
        when(aiService.aiChatStream(any())).thenReturn(emitter);

        MvcResult asyncResult = performSseRequest("你好");

        assertThat(asyncResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .contains("data:")
                .contains("\"code\":200");
    }

    @Test
    void testAiServiceErrorReturnsErrorEvent() throws Exception {
        when(aiService.aiChatStream(any())).thenReturn(SseEmitterUtils.error(500, "AI 服务暂时不可用，请稍后重试"));

        MvcResult asyncResult = performSseRequest("触发异常");

        assertThat(asyncResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .contains("event:error")
                .contains("\"code\":500")
                .contains("AI 服务暂时不可用，请稍后重试");
    }

    @Test
    void testTimeoutReturnsErrorEvent() throws Exception {
        when(aiService.aiChatStream(any())).thenReturn(SseEmitterUtils.error(504, "AI 响应超时，请稍后重试"));

        MvcResult asyncResult = performSseRequest("模拟超时");

        assertThat(asyncResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .contains("event:error")
                .contains("\"code\":504")
                .contains("AI 响应超时，请稍后重试");
    }

    private MvcResult performSseRequest(String content) throws Exception {
        String token = getTestUserToken();
        String body = objectMapper.writeValueAsString(Map.of("content", content));

        MvcResult mvcResult = mockMvc.perform(post("/api/ai/chat/stream")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();

        return mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andReturn();
    }
}
