package com.diabetes.monitor.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diabetes.monitor.common.BizException;
import com.diabetes.monitor.common.Result;
import com.diabetes.monitor.common.cache.MetricsCacheManager;
import com.diabetes.monitor.dto.DailyGlucoseStatVO;
import com.diabetes.monitor.entity.*;
import com.diabetes.monitor.export.ExcelExportWriter;
import com.diabetes.monitor.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Tag(name = "健康记录", description = "身体指标、血糖、饮食、运动记录管理")
@RestController
@RequestMapping("/api/record")
public class HealthRecordController {

    @Resource private HealthRecordBodyService bodyService;
    @Resource private HealthRecordBloodSugarService bloodSugarService;
    @Resource private HealthRecordDietService dietService;
    @Resource private HealthRecordExerciseService exerciseService;
    @Resource private SysExerciseTypeService exerciseTypeService;
    @Resource private ThreadPoolTaskExecutor taskExecutor;
    @Resource private MetricsService metricsService;
    @Resource private MetricsCacheManager metricsCacheManager;
    @Resource private HealthDailyStatService statService;
    @Resource private ExcelExportWriter  excelExportWriter;

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    /** 补算区间上限（天）——逐天补算等于批量扫明细，必须设上限 */
    private static final int MAX_BACKFILL_DAYS = 90;

    // ===== 身体指标 =====
    @Operation(summary = "新增身体指标记录")
    @PostMapping("/body")
    public Result addBody(@RequestBody HealthRecordBody record) {
        if (record.getWeight() != null && record.getUserId() != null && record.getRecordDate() == null) {
            record.setRecordDate(LocalDate.now());
        }
        bodyService.save(record);
        return Result.ok("记录成功");
    }

    @Operation(summary = "查询身体指标列表")
    @GetMapping("/body/list/{userId}")
    public Result bodyList(@PathVariable Integer userId, @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate) {
        LambdaQueryWrapper<HealthRecordBody> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthRecordBody::getUserId, userId)
               .ge(startDate != null && !startDate.isEmpty(), HealthRecordBody::getRecordDate, startDate)
               .le(endDate != null && !endDate.isEmpty(), HealthRecordBody::getRecordDate, endDate)
               .orderByDesc(HealthRecordBody::getRecordDate);
        return Result.ok(bodyService.list(wrapper));
    }

    @Operation(summary = "查询身体指标趋势")
    @GetMapping("/body/trend/{userId}")
    public Result bodyTrend(@PathVariable Integer userId, @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate) {
        return Result.ok(bodyService.getBodyTrend(userId, startDate, endDate));
    }

    @Operation(summary = "查询最新身体指标")
    @GetMapping("/body/latest/{userId}")
    public Result bodyLatest(@PathVariable Integer userId) {
        return Result.ok(bodyService.getLatest(userId));
    }

    @Operation(summary = "删除身体指标记录")
    @DeleteMapping("/body/{id}")
    public Result deleteBody(@PathVariable Integer id) {
        bodyService.removeById(id);
        return Result.ok();
    }

    // ===== 血糖记录 =====
    @Operation(summary = "新增血糖记录")
    @PostMapping("/bloodSugar")
    public Result addBloodSugar(@RequestBody HealthRecordBloodSugar record) {
        bloodSugarService.save(record);
        metricsCacheManager.invalidate(record.getUserId());
        return Result.ok("记录成功");
    }

    @Operation(summary = "查询血糖记录列表")
    @GetMapping("/bloodSugar/list/{userId}")
    public Result bloodSugarList(@PathVariable Integer userId, @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate) {
        LambdaQueryWrapper<HealthRecordBloodSugar> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthRecordBloodSugar::getUserId, userId);
        if (startDate != null && !startDate.isEmpty()) wrapper.ge(HealthRecordBloodSugar::getMeasureTime, startDate + " 00:00:00");
        if (endDate != null && !endDate.isEmpty()) wrapper.le(HealthRecordBloodSugar::getMeasureTime, endDate + " 23:59:59");
        wrapper.orderByDesc(HealthRecordBloodSugar::getMeasureTime);
        return Result.ok(bloodSugarService.list(wrapper));
    }

    @Operation(summary = "查询血糖变化趋势")
    @GetMapping("/bloodSugar/trend/{userId}")
    public Result bloodSugarTrend(@PathVariable Integer userId, @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate) {
        return Result.ok(bloodSugarService.getBloodSugarTrend(userId, startDate, endDate));
    }

    @Operation(summary = "查询最新血糖记录")
    @GetMapping("/bloodSugar/latest/{userId}")
    public Result bloodSugarLatest(@PathVariable Integer userId) {
        return Result.ok(bloodSugarService.getLatest(userId));
    }

    @Operation(summary = "删除血糖记录")
    @DeleteMapping("/bloodSugar/{id}")
    public Result deleteBloodSugar(@PathVariable Integer id) {
        HealthRecordBloodSugar old = bloodSugarService.getById(id);
        bloodSugarService.removeById(id);
        if (old != null) {
            metricsCacheManager.invalidate(old.getUserId());
        }
        return Result.ok();
    }

    // ===== 饮食记录 =====
    @Operation(summary = "新增饮食记录")
    @PostMapping("/diet")
    public Result addDiet(@RequestBody HealthRecordDiet record) {
        dietService.save(record);
        return Result.ok("记录成功");
    }

    @Operation(summary = "查询饮食记录列表")
    @GetMapping("/diet/list/{userId}")
    public Result dietList(@PathVariable Integer userId, @RequestParam(required = false) String date) {
        LambdaQueryWrapper<HealthRecordDiet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthRecordDiet::getUserId, userId);
        if (date != null && !date.isEmpty()) {
            wrapper.ge(HealthRecordDiet::getEatTime, date + " 00:00:00")
                   .le(HealthRecordDiet::getEatTime, date + " 23:59:59");
        }
        wrapper.orderByDesc(HealthRecordDiet::getEatTime);
        return Result.ok(dietService.list(wrapper));
    }

    @Operation(summary = "查询饮食营养统计")
    @GetMapping("/diet/stats/{userId}")
    public Result dietStats(@PathVariable Integer userId, @RequestParam(required = false) String date) {
        return Result.ok(dietService.getDietStats(userId, date));
    }

    @Operation(summary = "删除饮食记录")
    @DeleteMapping("/diet/{id}")
    public Result deleteDiet(@PathVariable Integer id) {
        dietService.removeById(id);
        return Result.ok();
    }

    // ===== 运动记录 =====
    @Operation(summary = "新增运动记录")
    @PostMapping("/exercise")
    public Result addExercise(@RequestBody HealthRecordExercise record) {
        if (record.getExerciseDate() == null) record.setExerciseDate(LocalDate.now());
        exerciseService.save(record);
        return Result.ok("记录成功");
    }

    @Operation(summary = "查询运动记录列表")
    @GetMapping("/exercise/list/{userId}")
    public Result exerciseList(@PathVariable Integer userId, @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate) {
        LambdaQueryWrapper<HealthRecordExercise> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthRecordExercise::getUserId, userId)
               .ge(startDate != null && !startDate.isEmpty(), HealthRecordExercise::getExerciseDate, startDate)
               .le(endDate != null && !endDate.isEmpty(), HealthRecordExercise::getExerciseDate, endDate)
               .orderByDesc(HealthRecordExercise::getExerciseDate);
        return Result.ok(exerciseService.list(wrapper));
    }

    @Operation(summary = "查询运动消耗趋势")
    @GetMapping("/exercise/trend/{userId}")
    public Result exerciseTrend(@PathVariable Integer userId, @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate) {
        return Result.ok(exerciseService.getExerciseStats(userId, startDate, endDate));
    }

    @Operation(summary = "删除运动记录")
    @DeleteMapping("/exercise/{id}")
    public Result deleteExercise(@PathVariable Integer id) {
        exerciseService.removeById(id);
        return Result.ok();
    }

    // ===== 运动类型 =====
    @Operation(summary = "获取运动类型列表")
    @GetMapping("/exerciseType/list")
    public Result exerciseTypeList() {
        return Result.ok(exerciseTypeService.list(new LambdaQueryWrapper<SysExerciseType>().eq(SysExerciseType::getStatus, 1)));
    }

    // ===== 数据导出 =====
    @Operation(summary = "导出健康记录Excel")
    @GetMapping("/export")
    public void export(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                       HttpServletResponse response) throws IOException {
        Integer userId = (Integer) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"health-records-" + LocalDate.now() + ".xlsx\"");

        try (OutputStream out = response.getOutputStream()) {
            excelExportWriter.write(userId, startDate, endDate, out);
        }

    }

    @Operation(summary = "血糖指标分析（TIR / CV / GMI 等）")
    @GetMapping("/bloodSugar/metrics/{userId}")
    public Result bloodSugarMetrics(@PathVariable Integer userId,
                                    @RequestParam(required = false, defaultValue = "14") Integer days) {
        return Result.ok(metricsService.getBloodSugarMetrics(userId, days));
    }

    @Operation(summary = "血糖日聚合趋势（含今日实时兜底）")
    @GetMapping("/bloodSugar/daily/{userId}")
    public Result<List<DailyGlucoseStatVO>> bloodSugarDaily(
            @PathVariable Integer userId,
            @RequestParam(required = false, defaultValue = "30") Integer days) {
        return Result.ok(statService.trend(userId, days));
    }

    @Operation(summary = "手动补算日聚合（不传参数则补最近 7 天）")
    @PostMapping("/bloodSugar/daily/backfill/{userId}")
    public Result<Integer> backfillDaily(
            @PathVariable Integer userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to){
        LocalDate end = (to != null) ? to : LocalDate.now(ZONE).minusDays(1);
        LocalDate start = (from != null) ? from : end.minusDays(6);

        if (start.isAfter(end)) {
            return Result.error("起始日期不能晚于结束日期");
        }

        //区间上限校验：防止逐天补算扫全表
        long span = ChronoUnit.DAYS.between(start, end) + 1;   // 含首尾
        if (span > MAX_BACKFILL_DAYS) {
            return Result.error("补算区间不能超过 " + MAX_BACKFILL_DAYS + " 天，当前 " + span + " 天");
        }

        int affected = statService.backfill(userId, start, end);
        return Result.ok(affected);
    }

}
