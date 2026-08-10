package com.diabetes.monitor.service.impl;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.diabetes.monitor.common.BizException;
import com.diabetes.monitor.common.Result;
import com.diabetes.monitor.config.AiConfig;
import com.diabetes.monitor.dto.DashScopeInput;
import com.diabetes.monitor.dto.DashScopeParameters;
import com.diabetes.monitor.dto.DashScopeRequest;
import com.diabetes.monitor.entity.*;
import com.diabetes.monitor.service.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.annotation.Resource;
import com.alibaba.dashscope.common.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


@Service
@Slf4j
public class AiServiceImpl implements AiService {
    
    @Resource
    private AiConfig aiConfig;

    @Resource private SysUserService sysUserService;
    @Resource private HealthRecordBloodSugarService bloodSugarService;
    @Resource private HealthRecordDietService dietService;
    @Resource private HealthRecordExerciseService exerciseService;
    @Resource private HealthRecordBodyService bodyService;

    @Resource
    private AiChatHistoryService aiChatHistoryService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private static final String REDIS_CONTEXT_PREFIX = "chat:context:";   // Redis key 前缀
    private static final String REDIS_SESSION_PREFIX = "chat:session:";   // session key 前缀
    private static final int MAX_CONTEXT_MESSAGES = 20;                    // Redis 最多保留20条
    private static final int CONTEXT_WINDOW = 10;                          // 每次发给AI最近10条
    private static final int CONTEXT_TTL_SECONDS = 1800;                   //30分钟

    /**
     * 模型名称
     */
    private static final String MODEL_NAME = "qwen3.7-max";
    private static final String DASHSCOPE_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";

    // 类顶部常量
    private static final String FONT_PATH = "C:/Windows/Fonts/simsun.ttc,0";  // 宋体

    @Resource
    private ThreadPoolTaskExecutor aiExecutor;
    @Resource
    private ThreadPoolTaskExecutor taskExecutor;

    @Resource
    private ObjectMapper objectMapper;

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

         return Message.builder()
                 .role(Role.SYSTEM.getValue())
                 .content(systemPrompt).build();
    }

    public Result chat (Map<String, String> body) {

        String content = body.get("content");

        if (content == null || content.isEmpty()) {
            return Result.error("请输入问题");
        }

        Integer userId = getCurrentUserId();
        String sessionId = getOrCreateSessionId(userId);

        //保存用户消息到MySQL + Redis
        CompletableFuture.allOf(
                CompletableFuture.runAsync(() -> aiChatHistoryService.saveMessage(userId,"user",content,sessionId),taskExecutor),
                CompletableFuture.runAsync(() ->saveToRedis(userId,"user",content),taskExecutor)
        ).orTimeout(3, TimeUnit.SECONDS)
                .exceptionally(ex ->{log.warn("用户消息保存超时或失败，继续执行", ex); return null;})
                .join();


        String reply = AiChat(content, userId);

        //保存AI回复到MySQL + Redis
        CompletableFuture.allOf(
                CompletableFuture.runAsync(() -> aiChatHistoryService.saveMessage(userId,"assistant",reply,sessionId), taskExecutor),
                CompletableFuture.runAsync(() -> saveToRedis(userId,"assistant",reply), taskExecutor)
        ).exceptionally(ex -> { log.error("AI回复保存失败", ex); return null; });

        Map<String,String> data = new HashMap<>();
        data.put("reply",reply);

        return Result.ok(data);
    }

    private String AiChat(String content, Integer userId) {
        try {
            //1.创建生成客户端
            Generation generation = new Generation();

            //2.构建系统消息
            Message systemMessage = buildSystemPrompt();

            //3.构建用户消息
            Message userMsg = Message.builder()
                    .role(Role.USER.getValue())
                    .content(content).build();
            // 4. 构建消息列表
            List<Message> messages = new ArrayList<>();
            messages.add(systemMessage);//第一条 :system prompt

            if (userId != null) {
                List<Map<String, String>> context = loadContextFromRedis(userId);
                for (Map<String, String> msg : context) {
                    messages.add(Message.builder()
                            .role(msg.get("role"))
                            .content(msg.get("content"))
                            .build());
                }
            }

            messages.add(userMsg);

            //5.构建请求参数
            GenerationParam param = GenerationParam.builder()
                    .model(MODEL_NAME)
                    .apiKey(aiConfig.getApiKey())
                    .messages(messages)
                    .temperature(aiConfig.getTemperature().floatValue())
                    .topP(0.8)
                    .maxTokens(2000)
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .build();

            //6.同步调用
            GenerationResult result = generation.call(param);

            //7.提取回复内容
            return result.getOutput()//获取输出对象
                    .getChoices()//获取候选列表
                    .get(0)//取第一个候选
                    .getMessage()
                    .getContent();
        } catch (ApiException | NoApiKeyException e) {
            log.error("AI 调用失败：{}", e.getMessage(), e);
            throw new BizException(500, "AI 服务调用失败：" + e.getMessage());
        } catch (Exception e) {
            log.error("AI 调用发生未知异常：", e);
            throw new BizException(500, "AI 服务调用失败，请稍后重试");
        }
    }

    /**
     * 流式对话
     */
    @Override
    public SseEmitter aiChatStream(Map<String, String> body) {
        String content = body.get("content");
        if (content == null || content.isEmpty()) {
            SseEmitter emitter = new SseEmitter();
            emitter.completeWithError(new BizException("请输入问题"));
            return emitter;
        }

        Integer userId = getCurrentUserId();
        String sessionId = getOrCreateSessionId(userId);

        CompletableFuture.allOf(
                CompletableFuture.runAsync(() -> aiChatHistoryService.saveMessage(userId,"user",content,sessionId), taskExecutor),
                CompletableFuture.runAsync(() -> saveToRedis(userId,"user",content), taskExecutor)
        ).orTimeout(3, TimeUnit.SECONDS)
                .exceptionally(ex -> { log.warn("用户消息保存超时或失败，继续执行", ex); return null; })
                .join();

        return chatStream(content,userId,sessionId);
    }

    @Override
    public SseEmitter aiChatStream(Map<String, String> body, String systemPrompt) {
        String content = body.get("content");
        if (content == null || content.isEmpty()) {
            SseEmitter emitter = new SseEmitter();
            emitter.completeWithError(new BizException("请输入问题"));
            return emitter;
        }

        Integer userId = getCurrentUserId();
        String sessionId = getOrCreateSessionId(userId);

        CompletableFuture.allOf(
                CompletableFuture.runAsync(() -> aiChatHistoryService.saveMessage(userId,"user",content,sessionId), taskExecutor),
                CompletableFuture.runAsync(() -> saveToRedis(userId,"user",content), taskExecutor)
        ).orTimeout(3, TimeUnit.SECONDS)
                .exceptionally(ex -> { log.warn("用户消息保存超时或失败，继续执行", ex); return null; })
                .join();

        //5分钟超时
        SseEmitter emitter = new SseEmitter(300000L);
        StringBuilder fullReply = new StringBuilder();//收集完整回复

        //2.设置超时，错误，完成回调
        emitter.onTimeout(() -> {
            log.warn("SSE 连接超时");
        });

        emitter.onError((e) -> {
            log.error("SSE 连接错误:{}", e.getMessage());
            emitter.complete();
        });

        emitter.onCompletion(() -> {

            //保存Ai完整回复
            String reply = fullReply.toString();
            if (!reply.isEmpty()) {
                CompletableFuture.allOf(
                        CompletableFuture.runAsync(() -> aiChatHistoryService.saveMessage(userId,"assistant",reply,sessionId), taskExecutor),
                        CompletableFuture.runAsync(() -> saveToRedis(userId,"assistant",reply), taskExecutor)
                ).exceptionally(ex -> { log.error("AI回复保存失败", ex); return null; });
            }
            log.info("连接完成");
        });

        //3.异步执行AI调用（避免阻塞主线程）
        CompletableFuture.runAsync(() -> {
            HttpURLConnection conn = null;
            try {
                //建立连接
                URL url = new URL(DASHSCOPE_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + aiConfig.getApiKey());
                conn.setRequestProperty("X-DashScope-SSE", "enable");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(120000);

                //发送请求体
                String requestBody = objectMapper.writeValueAsString(buildRequest(content, userId, systemPrompt));
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(requestBody.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }

                log.info("开始流式AI调用，消息：{}", content);

                //逐行读SSE响应
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data:")) {
                            String json = line.substring(5).trim();
                            emitter.send(SseEmitter.event().data(json));
                            // 解析 JSON 提取 content，拼到 fullReply
                            JsonNode node = objectMapper.readTree(json);
                            String context = node.path("choices").path(0)
                                    .path("delta")
                                    .path("content").asText();
                            if (context != null && !context.isEmpty()) {
                                fullReply.append(context).append("\n");

                            }
                            if (json.contains("\"finish_reason\":\"stop\"")) {
                                break;
                            }
                        }
                    }
                }
                emitter.complete();
            } catch (Exception e) {
                log.error("SSE error", e);
                emitter.completeWithError(e);
            } finally {
                if (conn != null) conn.disconnect();
            }
        },aiExecutor);

        return emitter;
    }


    /**
     * 生成健康报告
     */
    @Override
    public ResponseEntity<byte[]> generateReport(Integer userId) {

        //查用户信息 + 全部健康数据（近7天）
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            log.warn("生成报告失败：用户不存在，userId={}", userId);
            return new ResponseEntity<>(new byte[0], HttpStatus.OK);
        }
        HealthRecordBody latestBody = bodyService.getLatest(userId);
        // 血糖/饮食/运动近7天...
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        //血糖
        QueryWrapper<HealthRecordBloodSugar> sugarWrapper = new QueryWrapper<>();
        sugarWrapper.eq("user_id", userId)
                .ge("measure_time", sevenDaysAgo)
                .orderByAsc("measure_time");
        List<HealthRecordBloodSugar> sugarList = bloodSugarService.list(sugarWrapper);

        // 饮食
        QueryWrapper<HealthRecordDiet> dietWrapper = new QueryWrapper<>();
        dietWrapper.eq("user_id", userId)
                .ge("eat_time", sevenDaysAgo)
                .orderByAsc("eat_time");
        List<HealthRecordDiet> dietList = dietService.list(dietWrapper);

        // 运动
        QueryWrapper<HealthRecordExercise> exerciseWrapper = new QueryWrapper<>();
        exerciseWrapper.eq("user_id", userId)
                .ge("exercise_date", sevenDaysAgo.toLocalDate().toString())
                .orderByAsc("exercise_date");
        List<HealthRecordExercise> exerciseList = exerciseService.list(exerciseWrapper);

        //生成pdf
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, bos);
        document.open();

        // 注册中文字体
        BaseFont baseFont = null;
        try {
            baseFont = BaseFont.createFont("C:/Windows/Fonts/simsun.ttc,0", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        } catch (IOException e) {
            log.error("加载中文字体失败", e);
        }
        Font titleFont = new Font(baseFont, 20, Font.BOLD);
        Font headerFont = new Font(baseFont, 14, Font.BOLD);
        Font bodyFont = new Font(baseFont, 11);
        Font smallFont = new Font(baseFont, 9);

        // ===== 标题 =====
        Paragraph title = new Paragraph("糖尿病健康管理报告", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(10);
        document.add(title);

        Paragraph subtitle = new Paragraph("生成日期：" + java.time.LocalDate.now(), smallFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(20);
        document.add(subtitle);

        // ===== 用户基本信息 =====
        document.add(new Paragraph("一、基本信息", headerFont));
        document.add(new Paragraph("姓名：" + (user.getRealName() != null ? user.getRealName() : user.getUsername()), bodyFont));
        document.add(new Paragraph("年龄：" + (user.getAge() != null ? user.getAge() : "未知") + " | 性别：" + (user.getGender() != null && user.getGender() == 1 ? "男" : "女"), bodyFont));
        document.add(new Paragraph("糖尿病类型：" + (user.getDiabetesType() != null ? (user.getDiabetesType() == 1 ? "1型" : user.getDiabetesType() == 2 ? "2型" : "妊娠") : "未知"), bodyFont));
        document.add(new Paragraph("确诊日期：" + (user.getDiagnosedDate() != null ? user.getDiagnosedDate().toString() : "未知"), bodyFont));
        document.add(new Paragraph(" "));

        // ===== 最新体征 =====
        document.add(new Paragraph("二、最新体征指标", headerFont));
        if (latestBody != null) {
            PdfPTable bodyTable = new PdfPTable(4);
            bodyTable.setWidthPercentage(100);
            bodyTable.addCell(createPdfCell("体重(kg)", bodyFont));
            bodyTable.addCell(createPdfCell("BMI", bodyFont));
            bodyTable.addCell(createPdfCell("体脂率", bodyFont));
            bodyTable.addCell(createPdfCell("血压", bodyFont));
            bodyTable.addCell(createPdfCell(latestBody.getWeight() != null ? String.format("%.1f", latestBody.getWeight()) : "-", bodyFont));
            bodyTable.addCell(createPdfCell(latestBody.getBmi() != null ? String.format("%.1f", latestBody.getBmi()) : "-", bodyFont));
            bodyTable.addCell(createPdfCell(latestBody.getBodyFat() != null ? String.format("%.1f", latestBody.getBodyFat()) : "-", bodyFont));
            bodyTable.addCell(createPdfCell(latestBody.getSystolicPressure() != null ? latestBody.getSystolicPressure() + "/" + latestBody.getDiastolicPressure() : "-", bodyFont));
            document.add(bodyTable);
        } else {
            document.add(new Paragraph("暂无体征数据", bodyFont));
        }
        document.add(new Paragraph(" "));

        // ===== 近7天血糖 =====
        document.add(new Paragraph("三、近7天血糖记录", headerFont));
        if (sugarList.isEmpty()) {
            document.add(new Paragraph("暂无血糖记录", bodyFont));
        } else {
            double sum = 0, max = 0, min = 999; int overCount = 0;
            PdfPTable sugarTable = new PdfPTable(4);
            sugarTable.setWidthPercentage(100);
            sugarTable.addCell(createPdfCell("时间", bodyFont));
            sugarTable.addCell(createPdfCell("类型", bodyFont));
            sugarTable.addCell(createPdfCell("血糖值", bodyFont));
            sugarTable.addCell(createPdfCell("状态", bodyFont));
            for (HealthRecordBloodSugar r : sugarList) {
                double val = r.getBloodSugar();
                sum += val; max = Math.max(max, val); min = Math.min(min, val);
                if (val > 7.8) overCount++;
                String type = r.getMeasureType() != null && r.getMeasureType() == 1 ? "空腹" : r.getMeasureType() != null && r.getMeasureType() == 2 ? "餐后2h" : "随机";
                String status = val > 7.8 ? "偏高" : val < 3.9 ? "偏低" : "正常";
                sugarTable.addCell(createPdfCell(r.getMeasureTime() != null ? r.getMeasureTime().toString() : "-", bodyFont));
                sugarTable.addCell(createPdfCell(type, bodyFont));
                sugarTable.addCell(createPdfCell(String.format("%.1f mmol/L", val), bodyFont));
                sugarTable.addCell(createPdfCell(status, bodyFont));
            }
            document.add(sugarTable);
            document.add(new Paragraph(" "));
            double avg = sum / sugarList.size();
            document.add(new Paragraph(String.format("统计：均值%.1f | 最高%.1f | 最低%.1f | 超标%d次", avg, max, min, overCount), bodyFont));
        }
        document.add(new Paragraph(" "));

        // ===== 近7天饮食 =====
        document.add(new Paragraph("四、近7天饮食汇总", headerFont));
        if (dietList.isEmpty()) {
            document.add(new Paragraph("暂无饮食记录", bodyFont));
        } else {
            double totalCal = 0, totalCarb = 0, totalProtein = 0;
            for (HealthRecordDiet r : dietList) {
                totalCal += r.getCalories() != null ? r.getCalories() : 0;
                totalCarb += r.getCarbs() != null ? r.getCarbs() : 0;
                totalProtein += r.getProtein() != null ? r.getProtein() : 0;
            }
            int days = Math.max(1, (int) java.time.temporal.ChronoUnit.DAYS.between(sevenDaysAgo.toLocalDate(), java.time.LocalDate.now()) + 1);
            document.add(new Paragraph(String.format("日均摄入：%.0fkcal | 碳水%.0fg | 蛋白%.0fg", totalCal/days, totalCarb/days, totalProtein/days), bodyFont));
            document.add(new Paragraph("糖友建议：1500-1800kcal/日，碳水占比45-55%", smallFont));
        }
        document.add(new Paragraph(" "));

        // ===== 近7天运动 =====
        document.add(new Paragraph("五、近7天运动汇总", headerFont));
        if (exerciseList.isEmpty()) {
            document.add(new Paragraph("暂无运动记录", bodyFont));
        } else {
            int totalMin = 0; double totalBurn = 0;
            for (HealthRecordExercise r : exerciseList) {
                totalMin += r.getDurationMinutes() != null ? r.getDurationMinutes() : 0;
                totalBurn += r.getCaloriesBurned() != null ? r.getCaloriesBurned() : 0;
            }
            document.add(new Paragraph(String.format("总运动时长：%d分钟 | 总消耗：%.0fkcal", totalMin, totalBurn), bodyFont));
            document.add(new Paragraph("建议：每周至少150分钟中等强度运动", smallFont));
        }

        document.close();

        byte[] pdfBytes = bos.toByteArray();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "健康报告.pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
    /**
     * 构建 DashScope HTTP 请求体
     */
    private DashScopeRequest buildRequest(String content,Integer userId) {
        Map<String, String> systemMsg = new LinkedHashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", "你是一名专业的糖尿病健康管理顾问，具备以下能力：\n" +
                "1. 血糖管理：解读血糖数据，提供控糖建议\n" +
                "2. 饮食指导：推荐适合糖尿病患者的饮食方案\n" +
                "3. 运动建议：根据患者状况推荐合适的运动\n" +
                "4. 并发症预防：科普并发症知识及预防措施\n" +
                "5. 用药提醒：提醒按时用药的重要性\n" +
                "\n注意事项：\n- 回复简洁、专业、易懂\n- 涉及具体医疗建议时，提示用户咨询医生\n- 不做诊断、不开处方\n- 语气温暖、鼓励为主");

        Map<String, String> userMsg = new LinkedHashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", content);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(systemMsg);

        //插入历史上下文
        if (userId != null) {
            List<Map<String, String>> context = loadContextFromRedis(userId);
            messages.addAll(context);
        }
        messages.add(userMsg);//当前用户消息

        DashScopeInput input = new DashScopeInput();
        input.setMessages(messages);

        DashScopeParameters params = new DashScopeParameters();
        params.setTemperature(aiConfig.getTemperature() != null ? aiConfig.getTemperature() : 0.7);

        DashScopeRequest request = new DashScopeRequest();
        request.setModel(MODEL_NAME);
        request.setInput(input);
        request.setParameters(params);

        return request;
    }


    /**
     * 构建 DashScope HTTP 请求体（自定义 system prompt，不加载 Redis 上下文）
     */
    private DashScopeRequest buildRequest(String content, Integer userId, String systemPrompt) {
        Map<String, String> systemMsg = new LinkedHashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);

        Map<String, String> userMsg = new LinkedHashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", content);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(systemMsg);
        messages.add(userMsg);

        //将消息列表封装进DashScope的输入对象中
        DashScopeInput input = new DashScopeInput();
        input.setMessages(messages);

        DashScopeParameters params = new DashScopeParameters();
        params.setTemperature(aiConfig.getTemperature() != null ? aiConfig.getTemperature() : 0.7);

        DashScopeRequest request = new DashScopeRequest();
        request.setModel(MODEL_NAME);
        request.setInput(input);
        request.setParameters(params);

        return request;
    }

    /**
     * HTTP SSE 流式调用 DashScope
     */
    private SseEmitter chatStream(String content, Integer userId, String sessionId) {
        //5分钟超时
        SseEmitter emitter = new SseEmitter(300000L);
        StringBuilder fullReply = new StringBuilder();//收集完整回复

        //2.设置超时，错误，完成回调
        emitter.onTimeout(() -> {
            log.warn("SSE 连接超时");
        });

        emitter.onError((e) -> {
            log.error("SSE 连接错误:{}", e.getMessage());
            emitter.complete();
        });

        emitter.onCompletion(() -> {

            //保存Ai完整回复
            String reply = fullReply.toString();
            if (!reply.isEmpty()) {
                CompletableFuture.allOf(
                        CompletableFuture.runAsync(() -> aiChatHistoryService.saveMessage(userId,"assistant",reply,sessionId), taskExecutor),
                        CompletableFuture.runAsync(() -> saveToRedis(userId,"assistant",reply), taskExecutor)
                ).exceptionally(ex -> { log.error("AI回复保存失败", ex); return null; });
            }
            log.info("连接完成");
        });

        //3.异步执行AI调用（避免阻塞主线程）
        CompletableFuture.runAsync(() -> {
           HttpURLConnection conn = null;
           try {
               //建立连接
               URL url = new URL(DASHSCOPE_URL);
               conn = (HttpURLConnection) url.openConnection();
               conn.setRequestMethod("POST");
               conn.setRequestProperty("Content-Type", "application/json");
               conn.setRequestProperty("Authorization", "Bearer " + aiConfig.getApiKey());
               conn.setRequestProperty("X-DashScope-SSE", "enable");
               conn.setDoOutput(true);
               conn.setConnectTimeout(5000);
               conn.setReadTimeout(120000);

               //发送请求体
               String requestBody = objectMapper.writeValueAsString(buildRequest(content,userId));
               try (OutputStream os = conn.getOutputStream()) {
                   os.write(requestBody.getBytes(StandardCharsets.UTF_8));
                   os.flush();
               }

               log.info("开始流式AI调用，消息：{}", content);

               //逐行读SSE响应
               try (BufferedReader reader = new BufferedReader(
                       new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                   String line;
                   while ((line = reader.readLine()) != null) {
                       if (line.startsWith("data:")) {
                           String json = line.substring(5).trim();
                           emitter.send(SseEmitter.event().data(json));
                           // 解析 JSON 提取 content，拼到 fullReply
                           JsonNode node = objectMapper.readTree(json);
                           String context = node.path("choices").path(0)
                                   .path("delta")
                                   .path("content").asText();
                           if (context != null && !context.isEmpty()) {
                               fullReply.append(context).append("\n");

                           }
                           if (json.contains("\"finish_reason\":\"stop\"")) {
                               break;
                           }
                       }
                   }
               }
               emitter.complete();
           } catch (Exception e) {
               log.error("SSE error", e);
               emitter.completeWithError(e);
           } finally {
               if (conn != null) conn.disconnect();
           }
       },aiExecutor);

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

    /**
     * 创建 PDF 表格单元格
     */
    private PdfPCell createPdfCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }
}
