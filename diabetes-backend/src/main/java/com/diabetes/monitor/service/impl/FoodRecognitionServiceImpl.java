package com.diabetes.monitor.service.impl;

import com.diabetes.monitor.dto.FoodRecognitionResult;
import com.diabetes.monitor.service.AiChatHistoryService;
import com.diabetes.monitor.service.FoodRecognitionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Slf4j
public class FoodRecognitionServiceImpl implements FoodRecognitionService {

    /** DashScope API Key */
    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    /** 多模态模型名称 */
    private static final String MODEL_NAME = "qwen-vl-max";

    /** DashScope 多模态 API 地址 */
    private static final String DASHSCOPE_URL =
            "https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation";

    private final ObjectMapper objectMapper;

    @Resource
    private AiChatHistoryService aiChatHistoryService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    public FoodRecognitionServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public FoodRecognitionResult recognize(MultipartFile file, Integer userId) throws IOException {

        // ==================== 1. 图片转 Base64 ====================
        byte[] imageBytes = file.getBytes();
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String imageUrl = "data:" + file.getContentType() + ";base64," + base64Image;

        // ==================== 2. 构建请求体（content 数组：image + text） ====================
        List<Map<String, Object>> content = new ArrayList<>();

        // 图片部分
        Map<String, Object> imagePart = new LinkedHashMap<>();
        imagePart.put("image", imageUrl);
        content.add(imagePart);

        // 文字部分：告诉 AI 以 JSON 格式返回识别结果
        String promptText = "请识别图中的食物，以JSON格式返回："
                + "{\"foodName\":\"食物名\",\"calories\":热量千卡,\"carbs\":碳水克数,"
                + "\"protein\":蛋白克数,\"fat\":脂肪克数,\"glycemicIndex\":\"高/中/低\","
                + "\"suggestion\":\"适合糖尿病患者的食用建议\"}";
        Map<String, Object> textPart = new LinkedHashMap<>();
        textPart.put("text", promptText);
        content.add(textPart);

        // ==================== 3. 构建 message ====================
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "user");
        message.put("content", content);

        // ==================== 4. 构建 input ====================
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("messages", Collections.singletonList(message));

        // ==================== 5. 构建最外层请求体 ====================
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", MODEL_NAME);
        requestBody.put("input", input);

        // ==================== 6. 序列化为 JSON ====================
        String jsonBody = objectMapper.writeValueAsString(requestBody);

        // ==================== 7. 发送 HTTP 请求 ====================
        URL url = new URL(DASHSCOPE_URL);
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);

            // 写入请求体
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            // ==================== 8. 读取响应 ====================
            int responseCode = conn.getResponseCode();
            InputStream inputStream = responseCode >= 400
                    ? conn.getErrorStream()
                    : conn.getInputStream();

            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }

            // ==================== 9. 解析响应的 content ====================
            JsonNode root = objectMapper.readTree(response.toString());

            // 检查是否有错误
            if (root.has("code")) {
                String errorMsg = root.path("code").asText() + ": " + root.path("message").asText();
                log.error("DashScope 返回错误：{}", errorMsg);
                throw new RuntimeException("AI 识别失败：" + errorMsg);
            }

            // 提取 output.choices[0].message.content[0].text
            String aiText = root.path("output")
                    .path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .path(0)
                    .path("text")
                    .asText();

            log.info("AI 食物识别原始返回：{}", aiText);

            // ==================== 10. 解析 AI 返回的 JSON ====================
            FoodRecognitionResult result = parseAiJson(aiText);

            // ==================== 11. 保存到 MySQL + Redis ====================
            if (userId != null) {
                // 保存用户上传的提示词（占位）
                aiChatHistoryService.saveMessage(userId, "user", "[上传食物图片]", null);
                // 保存 AI 识别结果
                aiChatHistoryService.saveMessage(userId, "assistant",
                        "识别结果：" + result.getFoodName() + " | 热量：" + result.getCalories() + "kcal", null);
            }

            return result;

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 解析 AI 返回的 JSON 字符串为 FoodRecognitionResult
     * 支持两种格式：
     * 1. 单个对象 {"foodName":"...","calories":...,...}
     * 2. 数组 [{...}] 或 [{...},{...}]，取第一个元素
     * 同时处理 markdown 代码块包裹（```json ... ```）、前后有说明文字等情况
     */
    private FoodRecognitionResult parseAiJson(String aiText) {
        FoodRecognitionResult result = new FoodRecognitionResult();
        try {
            // 1. 预处理：去掉 markdown 代码块标记
            String jsonStr = aiText
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();

            // 2. 尝试提取 JSON：找第一个 [ 或 { 和最后一个 ] 或 }
            int startBrace = jsonStr.indexOf('{');
            int startBracket = jsonStr.indexOf('[');
            int start = -1, end = -1;
            int tmpEndBrace = jsonStr.lastIndexOf('}');
            int tmpEndBracket = jsonStr.lastIndexOf(']');

            // 判断是数组格式还是对象格式
            if (startBracket >= 0 && (startBracket < startBrace || startBrace < 0)) {
                // 数组格式 [{...}, {...}]
                start = startBracket;
                end = (tmpEndBracket > tmpEndBrace) ? tmpEndBracket : tmpEndBrace;
            } else if (startBrace >= 0) {
                // 对象格式 {...}
                start = startBrace;
                end = tmpEndBrace;
            }

            if (start >= 0 && end > start) {
                jsonStr = jsonStr.substring(start, end + 1);
            }

            // 3. 解析 JSON
            JsonNode rootNode = objectMapper.readTree(jsonStr);

            // 4. 如果 AI 返回的是数组，取第一个元素
            JsonNode resultNode;
            if (rootNode.isArray() && rootNode.size() > 0) {
                log.info("AI 返回了 {} 个食物识别结果，取第一个", rootNode.size());
                resultNode = rootNode.get(0);
            } else {
                resultNode = rootNode;
            }

            // 5. 填充结果
            result.setFoodName(resultNode.path("foodName").asText("未知"));
            result.setCalories(resultNode.path("calories").asDouble());
            result.setCarbs(resultNode.path("carbs").asDouble());
            result.setProtein(resultNode.path("protein").asDouble());
            result.setFat(resultNode.path("fat").asDouble());
            result.setGlycemicIndex(resultNode.path("glycemicIndex").asText("未知"));
            result.setSuggestion(resultNode.path("suggestion").asText("无"));

        } catch (Exception e) {
            log.warn("解析 AI 返回 JSON 失败，使用原始文本：{}", e.getMessage());
            result.setFoodName("识别失败");
            result.setSuggestion("AI 返回格式异常，请重试");
        }
        result.setRawResponse(aiText);
        return result;
    }
}