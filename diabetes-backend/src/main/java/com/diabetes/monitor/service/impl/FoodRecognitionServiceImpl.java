package com.diabetes.monitor.service.impl;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.diabetes.monitor.dto.FoodRecognitionResult;
import com.diabetes.monitor.service.AiChatHistoryService;
import com.diabetes.monitor.service.FoodRecognitionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class FoodRecognitionServiceImpl implements FoodRecognitionService {

    /** 多模态模型名称 */
    private static final String MODEL_NAME = "qwen-vl-max";

    @Resource
    private ChatModel  chatModel;

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

        // ==================== 图片转 Base64 ====================
        byte[] imageBytes = file.getBytes();
        log.info("原始文件字节数: {}, 文件名: {}", imageBytes.length, file.getOriginalFilename());

        byte[] compressed = compress(imageBytes);
        log.info("压缩后字节数: {}", compressed.length);
        //计算图片 MD5，查 Redis 缓存
        String md5 = DigestUtils.md5DigestAsHex(compressed);
        String cacheKey = "food:cache:" + md5;

        //查缓存
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.info("命中缓存，直接返回：md5={}", md5);
            if (cached instanceof FoodRecognitionResult) {
                return (FoodRecognitionResult) cached;
            }
            // 如果存的是 JSON 字符串，反序列化
            String json = cached.toString();
            return objectMapper.readValue(json, FoodRecognitionResult.class);
        }

        // 1. 构建图片媒体（compress 后统一是 JPEG）
        Media media = Media.builder()
                .mimeType(new MimeType("image", "jpeg"))
                .data(compressed).build();

        // 文字部分：告诉 AI 以 JSON 格式返回识别结果
        String promptText = "请识别图中的食物，以JSON格式返回："
                + "{\"foodName\":\"食物名\",\"calories\":热量千卡,\"carbs\":碳水克数,"
                + "\"protein\":蛋白克数,\"fat\":脂肪克数,\"glycemicIndex\":\"高/中/低\","
                + "\"suggestion\":\"适合糖尿病患者的食用建议\"}";

        // 2. 构建多模态消息：图片 + 原 promptText
        UserMessage userMessage = UserMessage.builder()
                .text(promptText).media(media).build();

        // 3. 指定视觉模型并调用
        // 必须开启 multiModel，DashScopeChatModel 才会走多模态接口而不是文本接口
        DashScopeChatOptions options = DashScopeChatOptions.builder()
                .withModel(MODEL_NAME)
                .withMultiModel(true)
                .build();

        ChatResponse response = chatModel.call(new Prompt(List.of(userMessage), options));

        // 4. 取文本
        String aiText = response.getResult().getOutput().getText();
        log.info("AI 食物识别原始返回：{}", aiText);

        // 5. 解析 AI 返回的 JSON
        FoodRecognitionResult result = parseAiJson(aiText);


        //6. 保存到 MySQL + Redis
        if (userId != null) {
            // 保存用户上传的提示词（占位）
            aiChatHistoryService.saveMessage(userId, "user", "[上传食物图片]", null);
            // 保存 AI 识别结果
            aiChatHistoryService.saveMessage(userId, "assistant",
                    "识别结果：" + result.getFoodName() + " | 热量：" + result.getCalories() + "kcal", null);
        }

        // 存入 Redis 缓存（24 小时过期）
        redisTemplate.opsForValue().set(cacheKey, result, 24, TimeUnit.HOURS);
        log.info("识别结果已缓存：md5={}", md5);

        return result;
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

    /**
     * 压缩图片，限制最大宽高 1024px，输出 JPEG 格式
     * @param imageBytes 原始图片字节
     * @return 压缩后的图片字节
     */
    private byte[] compress(byte[] imageBytes) throws IOException {
         //读入BuffereedImage,将字节数组转换为 BufferedImage 对象
        ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
        BufferedImage image = ImageIO.read(bis);
        if (image == null) {
            log.warn("ImageIO 无法解析该图片格式，跳过压缩直接使用原始数据");
            return imageBytes;
        }
        //如果原图宽或高 > 1024,等比例放缩
        // 如果原图带透明通道（PNG等），转为不透明的RGB，确保JPEG能正常写入
        if (image.getType() != BufferedImage.TYPE_INT_RGB && image.getType() != BufferedImage.TYPE_INT_BGR) {
            BufferedImage rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgbImage.createGraphics();
            g.drawImage(image, 0, 0, null);
            g.dispose();
            image = rgbImage;
            log.info("compress: 已将图片转为RGB格式");
        }

        int maxSize =  1024;
        int width = image.getWidth();
        int height = image.getHeight();
        log.info("compress: 原始尺寸={}x{}, 原始字节数={}", width, height, imageBytes.length);
        if (width > maxSize || height > maxSize) {
            double ratio = Math.min((double) maxSize / width, (double) maxSize / height);
            width = (int) (width * ratio);
            height = (int) (height * ratio);
            //创建一个新的空白图片画布，尺寸为缩放后的宽高，RGB表示彩色（无透明度）
            BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            //获取画布的"画笔"（Graphics2D对象），用于绘制
            Graphics2D g2d = scaled.createGraphics();
            //设置缩放质量为"双线性插值"，让缩放后的图片更平滑，不出现锯齿
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            //把原始图片（image）绘制到新画布上，自动缩放到指定尺寸
            g2d.drawImage(image, 0, 0, width, height, null);
            g2d.dispose();
            image = scaled;
        }

        // 输出压缩后的 JPEG
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        boolean writeOk = ImageIO.write(image, "jpg", bos);
        log.info("compress: ImageIO.write 返回: {}", writeOk);
        byte[] result = bos.toByteArray();
        log.info("compress: JPEG 输出大小={} bytes", result.length);
        return result;

    }
}
