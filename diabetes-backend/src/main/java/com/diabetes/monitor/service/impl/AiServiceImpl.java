package com.diabetes.monitor.service.impl;

import com.diabetes.monitor.common.Result;
import com.diabetes.monitor.common.SseEmitterUtils;
import com.diabetes.monitor.entity.*;
import com.diabetes.monitor.service.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;


@Service
@Slf4j
public class AiServiceImpl implements AiService {
    
    @Resource
    private ChatModel chatModel;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private AiChatHistoryService aiChatHistoryService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private static final String REDIS_CONTEXT_PREFIX = "chat:context:";   // Redis key 前缀
    private static final String REDIS_SESSION_PREFIX = "chat:session:";   // session key 前缀
    private static final int MAX_CONTEXT_MESSAGES = 20;                    // Redis 最多保留20条
    private static final int CONTEXT_WINDOW = 10;                          // 每次发给AI最近10条
    private static final int CONTEXT_TTL_SECONDS = 1800;                   //30分钟

    @Resource
    private ThreadPoolTaskExecutor persistenceExecutor;

    /**
     * 构建系统提示词（糖尿病顾问角色）
     * @return System 角色的 Message
     */
    private Message buildSystemPrompt() {
         String systemPrompt = """
                 你是一名专业的糖尿病健康管理顾问，具备以下能力：
                 1. 血糖管理：解读血糖数据，提供控糖建议
                 2. 饮食指导：推荐适合糖尿病患者的饮食方案
                 3. 运动建议：根据患者状况推荐合适的运动
                 4. 并发症预防：科普并发症知识及预防措施
                 5. 用药提醒：提醒按时用药的重要性
                 
                 注意事项：
                 - 回复简洁、专业、易懂
                 - 涉及具体医疗建议时，提示用户咨询医生
                 - 不做诊断、不开处方
                 - 语气温暖、鼓励为主""";

         return new SystemMessage(systemPrompt);
    }

    public Result chat (Map<String, String> body) {

        String content = body.get("content");

        if (content == null || content.isEmpty()) {
            return Result.error("请输入问题");
        }

        Integer userId = getCurrentUserId();
        String sessionId = getOrCreateSessionId(userId);

        //组装消息列表
        List<Message> messages = new ArrayList<>();
        messages.add(buildSystemPrompt());

        // 加载历史上下文（此时 Redis 中已有当前用户消息）
        List<Map<String, String>> context = loadContextFromRedis(userId);
        for (Map<String, String> msg : context) {
            messages.add(convertRedisMapToMessage(msg));
        }

        messages.add(new UserMessage(content));

        //1. 保存用户消息到 Redis（异步，1秒超时保护）
       CompletableFuture<Void> redisSaveFuture =
               CompletableFuture.runAsync(() -> saveToRedis(userId,"user",content),
                       persistenceExecutor)
                       .orTimeout(1, TimeUnit.SECONDS)
                       .exceptionally(ex -> {
                           log.warn("用户消息 Redis 保存超时或失败，继续执行", ex);
                           return null;
                       });
        // 2. 保存用户消息到 MySQL（异步，不等待）
        CompletableFuture.runAsync(() ->
                        aiChatHistoryService.saveMessage(userId, "user", content, sessionId), persistenceExecutor)
                .orTimeout(1, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    log.warn("MySQL保存失败，userId: {}", userId, ex);
                    return null;
                });
        // 3. 等待 Redis 保存完成（最多1秒）
        try {
            redisSaveFuture.join();
        } catch (Exception e) {
            log.warn("Redis保存超时，继续执行", e);
        }

        // 4. 调用 AI
        ChatResponse response = chatModel.call(new Prompt(messages));
        String reply = response.getResult().getOutput().getText();

        // 5. AI回复保存（也等待Redis保存）
        CompletableFuture<Void> aiRedisSaveFuture =
                CompletableFuture.runAsync(() -> saveToRedis(userId, "assistant", reply), persistenceExecutor)
                        .orTimeout(1, TimeUnit.SECONDS)
                        .exceptionally(ex -> {
                            log.error("AI回复Redis保存失败", ex);
                            return null;
                        });

        CompletableFuture.runAsync(() ->
                        aiChatHistoryService.saveMessage(userId, "assistant", reply, sessionId), persistenceExecutor)
                .orTimeout(1, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    log.error("AI回复MySQL保存失败", ex);
                    return null;
                });

        aiRedisSaveFuture.join();  // 等待1秒
        Map<String, String> data = new HashMap<>();
        data.put("reply", reply);

        return Result.ok(data);
    }


    /**
     * 将 Redis 中的 Map 转换为 Spring AI Message 对象
     */
    private Message convertRedisMapToMessage(Map<String, String> msg) {
        if (msg == null || !msg.containsKey("role") || !msg.containsKey("content")) {
            throw new IllegalArgumentException("Message map must contain 'role' and 'content' keys");
        }
        return switch (msg.get("role").toLowerCase()) {
            case "system"    -> new SystemMessage(msg.get("content"));
            case "assistant" -> new AssistantMessage(msg.get("content"));
            case "user"      -> new UserMessage(msg.get("content"));
            default -> throw new IllegalArgumentException("Unknown role: " + msg.get("role"));
        };
    }

    /**
     * 流式对话
     */
    @Override
    public SseEmitter aiChatStream(Map<String, String> body) {
        String content = body.get("content");
        if (content == null || content.isEmpty()) {
            return SseEmitterUtils.error(400, "请输入问题");
        }

        Integer userId = getCurrentUserId();
        if (userId == null) {
            return SseEmitterUtils.error(401, "登录状态已失效，请重新登录");
        }

        try {
            String sessionId = getOrCreateSessionId(userId);

            // 1.拼消息列表（system + Redis历史 + 当前用户消息）
            List<Message> messages = new ArrayList<>();
            messages.add(buildSystemPrompt());
            for (Map<String, String> msg : loadContextFromRedis(userId)) {
                messages.add(convertRedisMapToMessage(msg));
            }
            messages.add(new UserMessage(content));

            // 2.保存用户消息
            // 2.1 Redis保存（异步，1秒超时保护）
            CompletableFuture<Void> redisFuture =
                    CompletableFuture.runAsync(() -> saveToRedis(userId, "user", content), persistenceExecutor)
                            .orTimeout(1, TimeUnit.SECONDS)
                            .exceptionally(ex -> {
                                log.warn("用户消息Redis保存失败，userId: {}", userId, ex);
                                return null;
                            });

            // 2.2 MySQL保存（异步，不等待）
            CompletableFuture.runAsync(() ->
                            aiChatHistoryService.saveMessage(userId, "user", content, sessionId), persistenceExecutor)
                    .exceptionally(ex -> {
                        log.warn("用户消息MySQL保存失败，userId: {}", userId, ex);
                        return null;
                    });

            // 3. 调用AI（Spring AI 流式），回调里保存 AI 回复
            return streamWithSpringAi(messages, reply -> {
                // 4. AI回复保存
                // 4.1 Redis保存（等待1秒）
                CompletableFuture<Void> aiRedisFuture =
                        CompletableFuture.runAsync(() -> saveToRedis(userId, "assistant", reply), persistenceExecutor)
                                .orTimeout(1, TimeUnit.SECONDS)
                                .exceptionally(ex -> {
                                    log.error("AI回复Redis保存失败，userId: {}", userId, ex);
                                    return null;
                                });

                // 4.2 MySQL保存（异步，不等待）
                CompletableFuture.runAsync(() ->
                                aiChatHistoryService.saveMessage(userId, "assistant", reply, sessionId), persistenceExecutor)
                        .exceptionally(ex -> {
                            log.error("AI回复MySQL保存失败，userId: {}", userId, ex);
                            return null;
                        });
            });

        } catch (Exception e) {
            log.error("AI 流式对话初始化失败", e);
            return SseEmitterUtils.error(500, "AI 服务暂时不可用，请稍后重试");
        }
    }

    @Override
    public SseEmitter aiChatStream(Map<String, String> body, String systemPrompt) {
        String content = body.get("content");
        if (content == null || content.isEmpty()) {
            return SseEmitterUtils.error(400, "请输入问题");
        }

        Integer userId = getCurrentUserId();
        if (userId == null) {
            return SseEmitterUtils.error(401, "登录状态已失效，请重新登录");
        }

        try {
            String sessionId = getOrCreateSessionId(userId);

            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(systemPrompt));
            messages.add(new UserMessage(content));

            CompletableFuture.allOf(
                    CompletableFuture.runAsync(() -> aiChatHistoryService.saveMessage(userId,"user",content,sessionId), persistenceExecutor),
                    CompletableFuture.runAsync(() -> saveToRedis(userId,"user",content), persistenceExecutor)
            ).orTimeout(3, TimeUnit.SECONDS)
                    .exceptionally(ex -> { log.warn("用户消息保存超时或失败，继续执行", ex); return null; })
                    .join();

            return streamWithSpringAi(messages, reply -> CompletableFuture.allOf(
                    CompletableFuture.runAsync(() -> aiChatHistoryService.saveMessage(userId,"assistant",reply,sessionId), persistenceExecutor),
                    CompletableFuture.runAsync(() -> saveToRedis(userId,"assistant",reply), persistenceExecutor)
            ).exceptionally(ex -> { log.error("AI回复保存失败", ex); return null; }));
        } catch (Exception e) {
            log.error("AI 流式分析初始化失败", e);
            return SseEmitterUtils.error(500, "AI 服务暂时不可用，请稍后重试");
        }
    }

    /**
     * 使用 Spring AI ChatModel 流式调用，并桥接到 SseEmitter
     * @param messages 已拼好的 Spring AI 消息列表
     * @param onComplete 完整回复收集完成后的回调（用于持久化）
     * @return SSE 响应
     */
    private SseEmitter streamWithSpringAi(List<Message> messages, Consumer<String> onComplete) {
        SseEmitter emitter = SseEmitterUtils.createEmitter();
        StringBuilder fullReply = new StringBuilder();

        emitter.onTimeout(() -> {
            log.warn("SSE 连接超时");
            SseEmitterUtils.sendError(emitter, 504, "AI 响应超时，请稍后重试");
        });

        emitter.onError(
                e -> {
                    log.error("SSE 连接错误: {}", e.getMessage());
                    SseEmitterUtils.sendError(emitter, e);
                }
        );

        emitter.onCompletion(() -> {
            String reply = fullReply.toString();
            if (!reply.isEmpty() && onComplete != null) {
                onComplete.accept(reply);   // 交给调用方存 Redis/MySQL
            }
            log.info("AI 流式输出完成");
        });

        chatModel.stream(new Prompt(messages))
                .subscribe(
                        // 每收到一个分块，累加并推送给前端
                        response -> {
                            String chunk = response.getResult().getOutput().getText();
                            if (chunk != null && !chunk.isEmpty()) {
                                fullReply.append(chunk);
                                try {
                                    emitter.send(SseEmitter.event().data(chunk));
                                } catch (IOException e) {
                                    log.warn("SSE 发送分块失败: {}", e.getMessage());
                                }
                            }
                        },
                        // AI 调用出错，发送统一错误事件（sendError 内部会 complete）
                        error -> SseEmitterUtils.sendError(emitter, error),
                        // 正常结束
                        emitter::complete
                );

        return emitter;
    }

    /**
     * 获取当前用户ID
     */
    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Integer) {
            return (Integer) principal;
        }
        return null;

    }

    /**
     * 获取或创建sessionId
     */
    private String getOrCreateSessionId(Integer userId) {

        if (userId == null) {
            throw new IllegalArgumentException("userId不能为空");
        }
        String key = REDIS_SESSION_PREFIX + userId;

        Object value = redisTemplate.opsForValue().get(key);
        String sessionId = null;

        if (value instanceof String) {
            sessionId = (String) value;
        }

        if (sessionId == null || sessionId.isEmpty()) {
            //创建新的对话
           sessionId = UUID.randomUUID().toString();

           redisTemplate.opsForValue().set(key,sessionId,CONTEXT_TTL_SECONDS, TimeUnit.SECONDS);
            log.debug("创建新会话: userId={}, sessionId={}", userId, sessionId);
        }else  {
            // 刷新过期时间（用户活跃，延长会话）
            redisTemplate.expire(key, CONTEXT_TTL_SECONDS, TimeUnit.SECONDS);
            log.debug("刷新会话: userId={}, sessionId={}", userId, sessionId);
        }
        return sessionId;
    }

    /**
     * 保存消息到Redis list
     */
        private void saveToRedis(Integer userId, String role, String content) {

        if (userId == null || role == null || content == null) {
            throw new IllegalArgumentException("参数不能为空");
        }

        String key = REDIS_CONTEXT_PREFIX + userId;
        Map<String, String> msg = new LinkedHashMap<>();

        msg.put("role", role);
        msg.put("content", content);

        try {
            String json = objectMapper.writeValueAsString(msg);
            // 直接使用Redis List操作，避免Lua脚本序列化兼容问题
            redisTemplate.opsForList().rightPush(key, json);
            // 裁剪：只保留最近 MAX_CONTEXT_MESSAGES 条
            redisTemplate.opsForList().trim(key, -MAX_CONTEXT_MESSAGES, -1);
            // 设置过期时间
            redisTemplate.expire(key, CONTEXT_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.error("消息序列化失败", e);
            throw new RuntimeException("消息序列化失败", e);
        }
    }

    /**
     * 从Redis加载最近N条上下文
     */
    private List<Map<String,String>> loadContextFromRedis(Integer userId) {
        if (userId == null) {
            log.warn("userId 为空，无法加载上下文");
            return Collections.emptyList();
        }
        String key = REDIS_CONTEXT_PREFIX + userId;

        //获取列表大小
        Long size = redisTemplate.opsForList().size(key);
        if (size == null) {
            return Collections.emptyList();
        }

        //3.计算范围(获取最近的CONTEXT_WINDOW条)
        long start = Math.max(0, size - CONTEXT_WINDOW);
        long end = size - 1;

        //4.获取数据
        List<Object> rawList = redisTemplate.opsForList().range(key, start, end);
        if (rawList == null || rawList.isEmpty()) {
            return Collections.emptyList();
        }

        //5.刷新过期时间（用户还在使用）
        redisTemplate.expire(key,CONTEXT_TTL_SECONDS,TimeUnit.SECONDS);

        //6.反序列化
        List<Map<String,String>> context = new ArrayList<>();
        for (Object obj : rawList) {
            if (obj == null) {
                continue;
            }

            try {
                //如果是JSON字符串，反序列化
                if (obj instanceof String) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> map = objectMapper.readValue(
                            (String) obj,
                            new TypeReference<>() {
                            }
                    );
                    context.add(map);
                }
                //如果已经是Map(Jackson 自动反序列化)
                else if (obj instanceof Map) {
                    @SuppressWarnings("unchecked")
                            Map<String, String> map = (Map<String, String>) obj;
                    context.add(map);
                }else {
                    log.warn("未知的消息类型:{}",obj.getClass().getName());
                }
            } catch (JsonProcessingException e) {
                log.error("消息反序列化失败: {}", obj, e);
            }
        }
        log.debug("加载上下文成功: userId={}, 消息数={}", userId, context.size());

        return context;
    }
}
