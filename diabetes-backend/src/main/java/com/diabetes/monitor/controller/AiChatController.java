package com.diabetes.monitor.controller;


import com.diabetes.monitor.common.Result;
import com.diabetes.monitor.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "AI智能助手", description = "糖尿病健康AI问答")
@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    @Resource
    private AiService aiService;

    @Operation(summary = "发送消息给AI助手")
    @PostMapping("/chat")
    public Result chat (@RequestBody Map<String, String> body) {
        return aiService.chat(body);
    }
}
