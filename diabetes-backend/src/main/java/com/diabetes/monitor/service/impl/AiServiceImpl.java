package com.diabetes.monitor.service.impl;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.diabetes.monitor.common.BizException;
import com.diabetes.monitor.common.Result;
import com.diabetes.monitor.config.AiConfig;
import com.diabetes.monitor.dto.DashScopeInput;
import com.diabetes.monitor.dto.DashScopeParameters;
import com.diabetes.monitor.dto.DashScopeRequest;
import com.diabetes.monitor.service.AiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import com.alibaba.dashscope.common.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class AiServiceImpl implements AiService {
    
    @Resource
    private AiConfig aiConfig;

    /**
     * 模型名称
     */
    private static final String MODEL_NAME = "qwen3.7-max";
    private static final String DASHSCOPE_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 构建系统提示词（糖尿病顾问角色）
     * @return System 角色的 Message
     */
    private Message buildSystemPrompt() {
         String systemPrompt = "你是一名专业的糖尿病健康管理顾问，具备以下能力：\n" +
                 "1. 血糖管理：解读血糖数据，提供控糖建议\n" +
                 "2. 饮食指导：推荐适合糖尿病患者的饮食方案\n" +
                 "3. 运动建议：根据患者状况推荐合适的运动\n" +
                 "4. 并发症预防：科普并发症知识及预防措施\n" +
                 "5. 用药提醒：提醒按时用药的重要性\n" +
                 "\n" +
                 "注意事项：\n" +
                 "- 回复简洁、专业、易懂\n" +
                 "- 涉及具体医疗建议时，提示用户咨询医生\n" +
                 "- 不做诊断、不开处方\n" +
                 "- 语气温暖、鼓励为主";

         return Message.builder()
                 .role(Role.SYSTEM.getValue())
                 .content(systemPrompt).build();
    }

    public Result chat (@RequestBody Map<String, String> body) {

        String content = body.get("content");

        if (content == null || content.isEmpty()) {
            return Result.error("请输入问题");
        }
        
        String reply = AiChat(content);

        Map<String, String> data = new HashMap<>();
        data.put("reply", reply);

        return Result.ok(data);
    }

    private String AiChat(String content) {
        try {
            //1.创建生成客户端
            Generation generation = new Generation();

            //2.构建系统消息
            Message systemMessage = buildSystemPrompt();

            //3.构建用户消息
            Message userMsg = Message.builder()
                    .role(Role.USER.getValue())
                    .content(content).build();
            // 4. 构建消息列表（system 在前，user 在后）
            List<Message> messages = Arrays.asList(systemMessage, userMsg);

            //5.构建请求参数
            GenerationParam param = GenerationParam.builder()
                    .model(MODEL_NAME)
                    .apiKey(aiConfig.getApiKey())
                    .messages(messages)
                    .temperature(aiConfig.getTemperature().floatValue())
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

    /**
     * 流式对话
     */
    @Override
    public SseEmitter aiChatStream(Map<String, String> body) {
        String content = body.get("content");
        if (content == null || content.isEmpty()) {
            SseEmitter emitter = new SseEmitter();
            emitter.completeWithError(new BizException("请输入问题"));
            return emitter;
        }

        return chatStream(content);
    }

    /**
     * 构建 DashScope HTTP 请求体
     */
    private DashScopeRequest buildRequest(String content) {
        Map<String, String> systemMsg = new LinkedHashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", "你是一名专业的糖尿病健康管理顾问，具备以下能力：\n" +
                "1. 血糖管理：解读血糖数据，提供控糖建议\n" +
                "2. 饮食指导：推荐适合糖尿病患者的饮食方案\n" +
                "3. 运动建议：根据患者状况推荐合适的运动\n" +
                "4. 并发症预防：科普并发症知识及预防措施\n" +
                "5. 用药提醒：提醒按时用药的重要性\n" +
                "\n注意事项：\n- 回复简洁、专业、易懂\n- 涉及具体医疗建议时，提示用户咨询医生\n- 不做诊断、不开处方\n- 语气温暖、鼓励为主");

        Map<String, String> userMsg = new LinkedHashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", content);

        DashScopeInput input = new DashScopeInput();
        input.setMessages(Arrays.asList(systemMsg, userMsg));

        DashScopeParameters params = new DashScopeParameters();
        params.setTemperature(aiConfig.getTemperature() != null ? aiConfig.getTemperature() : 0.7);

        DashScopeRequest request = new DashScopeRequest();
        request.setModel(MODEL_NAME);
        request.setInput(input);
        request.setParameters(params);

        return request;
    }

    /**
     * HTTP SSE 流式调用 DashScope
     */
    private SseEmitter chatStream(String content) {
        //5分钟超时
        SseEmitter emitter = new SseEmitter(300000L);

        //2.设置超时，错误，完成回调
        emitter.onTimeout(() -> {
            log.warn("SSE 连接超时");
        });

        emitter.onError((e) -> {
            log.error("SSE 连接错误:{}", e.getMessage());
            emitter.complete();
        });

        emitter.onCompletion(() -> {
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
               conn.setRequestProperty("Authorization", "Bearer " + aiConfig.getApiKey());
               conn.setRequestProperty("X-DashScope-SSE", "enable");
               conn.setDoOutput(true);
               conn.setConnectTimeout(5000);
               conn.setReadTimeout(120000);

               //发送请求体
               String requestBody = objectMapper.writeValueAsString(buildRequest(content));
               try (OutputStream os = conn.getOutputStream()) {
                   os.write(requestBody.getBytes(StandardCharsets.UTF_8));
                   os.flush();
               }

               log.info("开始流式AI调用，消息：{}", content);

               //逐行读SSE响应
               try (BufferedReader reader = new BufferedReader(
                       new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                   String line;
                   while ((line = reader.readLine()) != null) {
                       if (line.startsWith("data:")) {
                           String json = line.substring(5).trim();
                           emitter.send(SseEmitter.event().data(json));
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
       });

       return emitter;
    }
}