package com.diabetes.monitor.service.impl;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.diabetes.monitor.common.BizException;
import com.diabetes.monitor.common.Result;
import com.diabetes.monitor.config.AiConfig;
import com.diabetes.monitor.service.AiService;
import jakarta.annotation.Resource;
import com.alibaba.dashscope.common.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AiServiceImpl implements AiService {
    
    @Resource
    private AiConfig aiConfig;

    /**
     * 模型名称
     */
    private static final String MODEL_NAME = "qwen3.7-max";

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
}