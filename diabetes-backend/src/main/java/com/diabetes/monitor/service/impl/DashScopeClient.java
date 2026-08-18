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
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.diabetes.monitor.common.BizException;
import com.diabetes.monitor.common.SseEmitterUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
@Slf4j
public class DashScopeClient {

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private WebClient dashscopeWebClient;

    /**
     * 模型名称
     */
    private static final String MODEL_NAME = "qwen3.7-max";
    private static final String DASHSCOPE_URL = "/api/v1/services/aigc/text-generation/generation";

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
        SseEmitter emitter = SseEmitterUtils.createEmitter();
        StringBuffer fullReply = new StringBuffer();//收集完整回复

        //2.设置超时，错误，完成回调
        emitter.onTimeout(() -> {
            log.warn("SSE 连接超时");
            SseEmitterUtils.sendError(emitter, 504, "AI 响应超时，请稍后重试");
        });

        emitter.onError((e) -> {
            log.error("SSE 连接错误:{}", e.getMessage());
        });

        //不直接调 service/Redis，改为调回调
        emitter.onCompletion(() -> {
            String reply = fullReply.toString();
            if (!reply.isEmpty() && onComplete != null) {
                onComplete.accept(reply);// 交给 AiServiceImpl 处理
            }
            log.info("连接完成");
        });

        //构建请求对象
        DashScopeRequest request = buildRequest(messages, temperature);

        //使用 WebClient 发起流式请求（异步非阻塞）
        dashscopeWebClient.post()
                .uri(DASHSCOPE_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .header("X-DashScope-SSE", "enable")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})//接收SSE原始数据
                .map(ServerSentEvent::data)// 去掉 "data:" 前缀
                .filter(json -> json != null && !json.isEmpty() && !"[DONE]".equals(json))
                .subscribe(
                        data-> handleSseEvent(emitter,fullReply,data),
                        //错误处理
                        error -> {
                            log.error("SSE流错误", error);
                            if (error instanceof WebClientResponseException wre) {
                                log.error("DashScope 流式响应异常，状态码：{}，响应体：{}", wre.getStatusCode(), wre.getResponseBodyAsString());
                            }
                            SseEmitterUtils.sendError(emitter, error);
                        },
                        //完成处理
                        () ->{
                            log.info("SSE流完成");
                            emitter.complete();
                        }

                );

        return emitter;
    }

    /**
     * 处理单个SSE事件
     * @param emitter
     * @param fullReply
     * @param data
     */
    private void handleSseEvent(SseEmitter emitter, StringBuffer fullReply, String data) {
        try {
            // 解析 JSON，提取 content 和结束标记
            JsonNode node = objectMapper.readTree(data);
            JsonNode choice = node.path("output").path("choices").path(0);
            String content = choice.path("message").path("content").asText();
            String finishReason = choice.path("finish_reason").asText();

            // 只有正式回答内容或结束标记才推给前端，过滤 reasoning_content
            boolean hasContent = content != null && !content.isEmpty();
            boolean isStop = "stop".equals(finishReason);
            if (hasContent || isStop) {
                emitter.send(SseEmitter.event().data(data));
            }

            if (hasContent) {
                fullReply.append(content).append("\n");
            }

            if (isStop) {
                log.debug("收到结束标记");
            }
        } catch (Exception e) {
            log.warn("解析 SSE 事件失败，跳过该事件：{}", e.getMessage());
        }
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
