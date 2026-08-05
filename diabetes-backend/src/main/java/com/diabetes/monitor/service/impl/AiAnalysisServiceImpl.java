package com.diabetes.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.diabetes.monitor.common.BizException;
import com.diabetes.monitor.entity.HealthRecordBloodSugar;
import com.diabetes.monitor.entity.HealthRecordBody;
import com.diabetes.monitor.entity.HealthRecordDiet;
import com.diabetes.monitor.entity.HealthRecordExercise;
import com.diabetes.monitor.service.*;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class AiAnalysisServiceImpl implements AiAnalysisService {

    @Resource
    private AiService aiService;

    @Resource
    private HealthRecordBloodSugarService bloodSugarService;
    @Resource
    private HealthRecordDietService dietService;
    @Resource
    private HealthRecordExerciseService exerciseService;
    @Resource
    private HealthRecordBodyService bodyService;

    /**
     * 血糖分析prompt
     * @return
     */
    /**
     * 饮食分析prompt
     * @return
     */
    /**
     * 血糖智能分析（流式）
     * @param userId 用户ID
     * @param days 分析天数（默认7）
     * @return SSE流式输出
     */
    @Override
    public SseEmitter analyzeBloodSugar(Integer userId, Integer days) {
        if (userId == null) {
            SseEmitter emitter = new SseEmitter();
            emitter.completeWithError(new BizException("用户未登录"));
            return emitter;
        }

        if (days == null || days <= 0) days = 7;

        //1.查询近N天血糖数据
        LocalDateTime startTime = LocalDateTime.now().minusDays(days);
        QueryWrapper<HealthRecordBloodSugar> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .ge("measure_time", startTime)
                .orderByAsc("measure_time")
                .last("limit 20");
        List<HealthRecordBloodSugar> records = bloodSugarService.list(wrapper);


        //2.拼接用户信息文本
        StringBuilder sb = new StringBuilder();
        sb.append("用户近").append(days).append("天血糖记录:\n\n");
        if (records.isEmpty()) {
            sb.append("（暂无血糖记录）\n");
        } else {
            for (HealthRecordBloodSugar r : records) {
                String type = measureTypeToName(r.getMeasureType());
                Double sugar = r.getBloodSugar();
                String status = sugar > 7.8 ? "偏高" : sugar < 3.9 ?"偏低" : "正常";
                sb.append(String.format("- %s | %s | %.1f mmol/L | %s\n",
                        r.getMeasureTime(), type, sugar, status));
            }
        }

        sb.append("\n请从以下角度分析：\n");
        sb.append("1. 整体趋势（对比正常范围）\n");
        sb.append("2. 异常点及可能原因\n");
        sb.append("3. 控糖建议\n");
        sb.append("4. 未来3天注意事项");

        //3.system prompt
        String systemPrompt = "你是糖尿病血糖管理专家。空腹血糖正常范围3.9-6.1 mmol/L，餐后2h正常范围<7.8 mmol/L。请根据用户数据给出专业分析。";

        HashMap<String, String> body = new HashMap<>();
        body.put("content", sb.toString());

        return aiService.aiChatStream(body,systemPrompt);
    }

    // 辅助方法：测量类型 1→空腹, 2→餐后2h, 3→随机
    private String measureTypeToName(Integer type) {
        if (type == null) return "未知";
        switch (type) {
            case 1: return "空腹";
            case 2: return "餐后2h";
            case 3: return "随机";
            default: return "其他";
        }
    }

    @Override
    public SseEmitter analyzeDiet(Integer userId, Integer days) {

        if (userId == null) {
            SseEmitter emitter = new SseEmitter();
            emitter.completeWithError(new BizException("用户未登录"));
            return emitter;
        }

        if (days == null || days <= 0) days = 7;

        //1.查询近N天饮食数据
        LocalDateTime startTime = LocalDateTime.now().minusDays(days);
        QueryWrapper<HealthRecordDiet> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .ge("eat_time", startTime)
                .orderByAsc("eat_time")
                .last("limit 30");
        List<HealthRecordDiet> records = dietService.list(wrapper);


        //2.拼接用户饮食记录文本
        StringBuilder sb = new StringBuilder();
        sb.append("用户近").append(days).append("天饮食记录:\n\n");
        if (records.isEmpty()) {
            sb.append("（暂无饮食记录）\n");
        } else {
            for (HealthRecordDiet r : records) {
                String mealType = getMealTypeName(r.getMealType());
                String foodName = r.getFoodName() != null ? r.getFoodName() : "-";
                String portion = r.getPortion() != null ?
                        String.format("%.1f 份", r.getPortion()) : "-";
                String calories = r.getCalories() != null ?
                        String.format("%.0f kcal", r.getCalories()) : "-";
                String carbs = r.getCarbs() != null ?
                        String.format("%.1f g", r.getCarbs()) : "-";
                String protein = r.getProtein() != null ?
                        String.format("%.1f g", r.getProtein()) : "-";
                String fat = r.getFat() != null ?
                        String.format("%.1f g", r.getFat()) : "-";
                String remark = r.getRemark() != null ? " | 备注:" + r.getRemark() : "";

                sb.append(String.format("- %s | %s | %s | 份量:%s | 热量:%s | 碳水:%s | 蛋白:%s | 脂肪:%s%s\n",
                        r.getEatTime(), mealType, foodName, portion,
                        calories, carbs, protein, fat, remark));
            }
        }

        sb.append("\n请从以下角度分析：\n");
        sb.append("1. 饮食结构合理性（三大营养素比例）\n");
        sb.append("2. 热量摄入是否合适\n");
        sb.append("3. 碳水摄入是否超标（糖尿病关键）\n");
        sb.append("4. 饮食习惯评价（规律性、食物多样性）\n");
        sb.append("5. 具体改善建议：\n");
        sb.append("   - 推荐食物\n");
        sb.append("   - 份量调整\n");
        sb.append("   - 进餐顺序\n");
        sb.append("   - 进食时间\n");
        sb.append("6. 未来3天饮食计划建议");

        //3.system prompt
        String systemPrompt = "你是一位糖尿病营养专家。根据用户的饮食记录分析饮食结构，" +
                "重点关注：碳水摄入是否超标、营养是否均衡、热量是否合适。" +
                "建议要具体、可执行，包含食物名称、份量、时间。";

        HashMap<String, String> body = new HashMap<>();
        body.put("content", sb.toString());

        return aiService.aiChatStream(body,systemPrompt);
    }

    /**
     * 获取餐次名称
     */
    private String getMealTypeName(Integer type) {
        if (type == null) return "未知";
        switch (type) {
            case 1: return "早餐";
            case 2: return "午餐";
            case 3: return "晚餐";
            case 4: return "加餐";
            default: return "其他";
        }
    }

    @Override
    public SseEmitter dailyReport(Integer userId) {

        if (userId == null) {
            SseEmitter emitter = new SseEmitter();
            emitter.completeWithError(new BizException("用户未登录"));
            return emitter;
        }

        String today = LocalDate.now().toString();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        // 1. 血糖 — 今天全部记录
        QueryWrapper<HealthRecordBloodSugar> sugarWrapper = new QueryWrapper<>();
        sugarWrapper.eq("user_id", userId).ge("measure_time", todayStart).orderByAsc("measure_time");
        List<HealthRecordBloodSugar> sugarList = bloodSugarService.list(sugarWrapper);

        // 2. 饮食 — 今天全部记录
        QueryWrapper<HealthRecordDiet> dietWrapper = new QueryWrapper<>();
        dietWrapper.eq("user_id", userId).ge("eat_time", todayStart).orderByAsc("eat_time");
        List<HealthRecordDiet> dietList = dietService.list(dietWrapper);

        // 3. 运动 — 今天全部记录
        QueryWrapper<HealthRecordExercise> exerciseWrapper = new QueryWrapper<>();
        exerciseWrapper.eq("user_id", userId).eq("exercise_date", today);
        List<HealthRecordExercise> exerciseList = exerciseService.list(exerciseWrapper);

        // 4. 身体指标 — 最新一条
        HealthRecordBody latestBody = bodyService.getLatest(userId);

        StringBuilder sb = new StringBuilder();
        sb.append("用户今日健康数据汇总（").append(today).append("）：\n\n");

        // 血糖
        sb.append("【血糖】\n");
        if (sugarList.isEmpty()) {
            sb.append("今日暂无血糖记录\n");
        } else {
            for (HealthRecordBloodSugar r : sugarList) {
                sb.append(String.format("- %s | %s | %.1f mmol/L\n",
                        r.getMeasureTime().toLocalTime(),
                        measureTypeToName(r.getMeasureType()),
                        r.getBloodSugar()));
            }
        }

        // 饮食
        sb.append("\n【饮食】\n");
        if (dietList.isEmpty()) {
            sb.append("今日暂无饮食记录\n");
        } else {
            double totalCal = 0, totalCarb = 0, totalProtein = 0;
            for (HealthRecordDiet r : dietList) {
                sb.append(String.format("- %s | %s | %.0fkcal | 碳水%.0fg | 蛋白%.0fg\n",
                        r.getEatTime().toLocalTime(),
                        r.getFoodName(),
                        r.getCalories(), r.getCarbs(), r.getProtein()));
                totalCal += r.getCalories() != null ? r.getCalories() : 0;
                totalCarb += r.getCarbs() != null ? r.getCarbs() : 0;
                totalProtein += r.getProtein() != null ? r.getProtein() : 0;
            }
            sb.append(String.format("合计：%.0fkcal | 碳%.0fg | 蛋白%.0fg\n", totalCal, totalCarb, totalProtein));
        }

        // 运动
        sb.append("\n【运动】\n");
        if (exerciseList.isEmpty()) {
            sb.append("今日暂无运动记录\n");
        } else {
            int totalMin = 0;
            double totalBurn = 0;
            for (HealthRecordExercise r : exerciseList) {
                sb.append(String.format("- %d分钟 | 消耗%.0fkcal\n",
                        r.getDurationMinutes(), r.getCaloriesBurned()));
                totalMin += r.getDurationMinutes() != null ? r.getDurationMinutes() : 0;
                totalBurn += r.getCaloriesBurned() != null ? r.getCaloriesBurned() : 0;
            }
            sb.append(String.format("合计：%d分钟 | 消耗%.0fkcal\n", totalMin, totalBurn));
        }

        // 身体指标
        sb.append("\n【身体指标】\n");
        if (latestBody == null) {
            sb.append("暂无身体数据\n");
        } else {
            sb.append(String.format("- 体重：%.1fkg | BMI：%.1f | 体脂：%.1f%%\n",
                    latestBody.getWeight(), latestBody.getBmi(), latestBody.getBodyFat()));
            if (latestBody.getSystolicPressure() != null) {
                sb.append(String.format("- 血压：%d/%d mmHg\n",
                        latestBody.getSystolicPressure(), latestBody.getDiastolicPressure()));
            }
        }

        sb.append("\n请综合以上数据，给出今日健康小结：\n");
        sb.append("1. 血糖评估\n");
        sb.append("2. 饮食质量（对比糖友建议1500-1800kcal/日）\n");
        sb.append("3. 运动达标情况\n");
        sb.append("4. 明日重点关注事项");

        // 6. system prompt
        String systemPrompt = "你是糖尿病健康管理专家。请根据用户当日血糖、饮食、运动、体征数据，给出综合健康评估和针对性建议。回复简洁，用自然的聊天语气，不用 markdown 格式。";

        Map<String, String> body = new HashMap<>();
        body.put("content", sb.toString());
        return aiService.aiChatStream(body, systemPrompt);

    }
}
