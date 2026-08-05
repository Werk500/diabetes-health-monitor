package com.diabetes.monitor.dto;

import lombok.Data;

@Data
public class FoodRecognitionResult {
    private String foodName;        // 食物名称
    private Double calories;        // 估算热量(kcal)
    private Double carbs;           // 碳水化合物(g)
    private Double protein;         // 蛋白质(g)
    private Double fat;             // 脂肪(g)
    private String glycemicIndex;   // 升糖指数（高/中/低）
    private String suggestion;      // 食用建议
    private String rawResponse;     // AI 原始回复
}
