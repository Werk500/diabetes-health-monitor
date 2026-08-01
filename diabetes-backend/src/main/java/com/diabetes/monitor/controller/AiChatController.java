package com.diabetes.monitor.controller;

import com.diabetes.monitor.common.Result;
import com.diabetes.monitor.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@Tag(name = "AI智能助手", description = "糖尿病健康AI问答")
@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    @Resource
    private AiService aiService;

    @Operation(summary = "发送消息给AI助手")
    @PostMapping("/chat")
    public Result chat(@RequestBody Map<String, String> body) {
        return aiService.chat(body);
    }

    @Operation(summary = "流式对话")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody Map<String, String> body) {
        return aiService.aiChatStream(body);
    }
}