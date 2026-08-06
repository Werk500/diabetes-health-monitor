package com.diabetes.monitor.service;

import com.diabetes.monitor.common.Result;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

public interface AiService {
    Result chat(Map<String, String> body);

    SseEmitter aiChatStream(Map<String, String> body);

    SseEmitter aiChatStream(Map<String, String> body, String systemPrompt);

    ResponseEntity<byte[]> generateReport(Integer userId);
}
