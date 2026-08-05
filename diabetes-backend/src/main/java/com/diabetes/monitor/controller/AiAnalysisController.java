package com.diabetes.monitor.controller;

import com.diabetes.monitor.service.AiAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@Tag(name = "AI智能分析", description = "基于健康数据的AI分析建议")
@RestController
@RequestMapping("/api/ai/analysis")
public class AiAnalysisController {

    @Resource
    private AiAnalysisService aiAnalysisService;

    @Operation(summary = "血糖智能分析")
    @PostMapping(value = "/blood-sugar", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter bloodSugar(@RequestBody Map<String, Integer> body) {
        Integer days = body.getOrDefault("days", 7);
        Integer userId = getCurrentUserIdFromSecurity();
        return aiAnalysisService.analyzeBloodSugar(userId, days);
    }

    @Operation(summary = "饮食智能分析")
    @PostMapping(value = "/diet", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter diet(@RequestBody Map<String, Integer> body) {
        Integer days = body.getOrDefault("days", 7);
        Integer userId = getCurrentUserIdFromSecurity();
        return aiAnalysisService.analyzeDiet(userId, days);
    }

    @Operation(summary = "每日综合健康小结")
    @PostMapping(value = "/daily-report", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter dailyReport() {
        Integer userId = getCurrentUserIdFromSecurity();
        return aiAnalysisService.dailyReport(userId);
    }

    private Integer getCurrentUserIdFromSecurity() {
        return (Integer) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }


}
