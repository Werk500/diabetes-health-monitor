package com.diabetes.monitor.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;


public interface AiAnalysisService {
    /**
     * 血糖智能分析（流式）
     * @param userId 用户ID
     * @param days 分析天数（默认7）
     * @return SSE流式输出
     */
    SseEmitter analyzeBloodSugar(Integer userId, Integer days);

    /**
     * 饮食智能分析（流式）
     * @param userId 用户ID
     * @param days 分析天数（默认7）
     * @return SSE流式输出
     */
    SseEmitter analyzeDiet(Integer userId, Integer days);

    /**
     * 每日综合健康小结（流式）
     * @param userId 用户ID
     * @return SSE流式输出
     */
    SseEmitter dailyReport(Integer userId);
}
