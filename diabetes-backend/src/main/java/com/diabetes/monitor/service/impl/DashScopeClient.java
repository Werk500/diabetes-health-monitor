package com.diabetes.monitor.service.impl;

import com.diabetes.monitor.dto.DashScopeInput;
import com.diabetes.monitor.dto.DashScopeParameters;
import com.diabetes.monitor.dto.DashScopeRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.diabetes.monitor.common.BizException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Component
@Slf4j
public class DashScopeClient {

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private ThreadPoolTaskExecutor aiExecutor;

    /**
     * 模型名称
     */
    private static final String MODEL_NAME = "qwen3.7-max";
    private static final String DASHSCOPE_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";

    public String callBlocking(List<Message> messages,
                               String apiKey, Double temperature){
        try {
            //1.创建生成客户端
            Generation generation = new Generation();

            //2.构建请求参数
            GenerationParam param = GenerationParam.builder()
                    .model(MODEL_NAME)
                    .apiKey(apiKey)
                    .messages(messages)
                    .temperature(temperature.floatValue())
                    .topP(0.8)
                    .maxTokens(2000)
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .build();

            //6.同步调用
            GenerationResult result = generation.call(param);

            //7.提取回复内容
            return result.getOutput()//获取输出对象
                    .getChoices()//获取候选列表
                    .get(0)//取第一个候选
                    .getMessage()
                    .getContent();
        } catch (ApiException | NoApiKeyException e) {
            log.error("AI 调用失败：{}", e.getMessage(), e);
            throw new BizException(500, "AI 服务调用失败：" + e.getMessage());
        } catch (Exception e) {
            log.error("AI 调用发生未知异常：", e);
            throw new BizException(500, "AI 服务调用失败，请稍后重试");
        }
    }

    public SseEmitter callStream(List<Map<String,String>> messages,// 已拼好的完整消息列表
                                 String apiKey, Double temperature,
                                 Consumer<String> onComplete)  // 回复完成后的回调
    {
        //5分钟超时
        SseEmitter emitter = new SseEmitter(300000L);
        StringBuilder fullReply = new StringBuilder();//收集完整回复

        //2.设置超时，错误，完成回调
        emitter.onTimeout(() -> {
            log.warn("SSE 连接超时");
        });

        emitter.onError((e) -> {
            log.error("SSE 连接错误:{}", e.getMessage());
            emitter.complete();
        });

        //不直接调 service/Redis，改为调回调
        emitter.onCompletion(() -> {
            String reply = fullReply.toString();
            if (!reply.isEmpty() && onComplete != null) {
                onComplete.accept(reply);// 交给 AiServiceImpl 处理
            }

            log.info("连接完成");
        });

        //3.异步执行AI调用（避免阻塞主线程）
        CompletableFuture.runAsync(() -> {
            HttpURLConnection conn = null;
            try {
                //建立连接
                URL url = new URL(DASHSCOPE_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setRequestProperty("X-DashScope-SSE", "enable");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(120000);

                //发送请求体
                String requestBody = objectMapper.writeValueAsString(buildRequest(messages, temperature));
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(requestBody.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }

                log.info("开始流式AI调用，消息：{}", messages);

                //逐行读SSE响应
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data:")) {
                            String json = line.substring(5).trim();

                            //转发给前端
                            emitter.send(SseEmitter.event().data(json));
                            // 解析 JSON 提取 content，拼到 fullReply
                            JsonNode node = objectMapper.readTree(json);
                            String context = node.path("choices").path(0)
                                    .path("delta")
                                    .path("content").asText();
                            if (context != null && !context.isEmpty()) {
                                fullReply.append(context).append("\n");

                            }
                            if (json.contains("\"finish_reason\":\"stop\"")) {
                                break;
                            }
                        }
                    }
                }
                emitter.complete();
            } catch (Exception e) {
                log.error("SSE error", e);
                emitter.completeWithError(e);
            } finally {
                if (conn != null) conn.disconnect();
            }
        },aiExecutor);

        return emitter;
    }

    private DashScopeRequest buildRequest(List<Map<String, String>> messages, Double temperature) {
        DashScopeInput input = new DashScopeInput();
        input.setMessages(messages);

        DashScopeParameters params = new DashScopeParameters();
        params.setTemperature(temperature != null ? temperature : 0.7);

        DashScopeRequest request = new DashScopeRequest();
        request.setModel(MODEL_NAME);
        request.setInput(input);
        request.setParameters(params);
        return request;
    }

}
