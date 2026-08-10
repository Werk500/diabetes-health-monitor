package com.diabetes.monitor.controller;

import com.diabetes.monitor.common.Result;
import com.diabetes.monitor.dto.FoodRecognitionResult;
import com.diabetes.monitor.service.FoodRecognitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * AI 食物识别控制器
 * 接收用户上传的食物图片，调用多模态 AI 识别食物并返回营养成分
 */
@Slf4j
@Tag(name = "AI食物识别", description = "拍照识别食物营养成分")
@RestController
@RequestMapping("/api/ai/food")
public class FoodRecognitionController {

    @Resource
    private FoodRecognitionService foodRecognitionService;

    @Operation(summary = "上传食物图片进行识别")
    @PostMapping("/recognize")
    public Result getFoodRecognitionResult(@RequestParam("file") MultipartFile file) {
        Integer userId = getCurrentUserId();

        try {
            FoodRecognitionResult result = foodRecognitionService.recognize(file, userId);
            return Result.ok(result);
        } catch (Exception e) {
            log.error("食物图片识别失败", e);
            return Result.error("图片识别失败：" + e.getMessage());
        }
    }

    /**
     * 从 SecurityContext 获取当前登录用户 ID
     */
    private Integer getCurrentUserId() {
        return (Integer) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }
}