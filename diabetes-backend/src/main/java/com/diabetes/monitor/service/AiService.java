package com.diabetes.monitor.service;

import com.diabetes.monitor.common.Result;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

public interface AiService {
    Result chat(Map<String, String> body);

    SseEmitter aiChatStream(Map<String, String> body);

    SseEmitter aiChatStream(Map<String, String> body, String systemPrompt);

    /**
     * 带场景标识的流式对话。
     * scene 决定这次调用的耗时/错误落在监控的哪个维度（ai_chat / ai_analysis_*），
     * 否则血糖分析、饮食分析、日报会和普通对话混在一条曲线里，看不出是哪块慢。
     */
    SseEmitter aiChatStream(Map<String, String> body, String systemPrompt, String scene);


}
