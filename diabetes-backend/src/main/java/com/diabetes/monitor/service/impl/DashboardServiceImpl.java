package com.diabetes.monitor.service.impl;

import com.diabetes.monitor.common.Result;
import com.diabetes.monitor.dto.*;
import com.diabetes.monitor.entity.HealthRecordBloodSugar;
import com.diabetes.monitor.entity.HealthRecordBody;
import com.diabetes.monitor.service.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    @Resource
    private HealthRecordBodyService bodyService;
    @Resource private HealthRecordBloodSugarService bloodSugarService;
    @Resource private HealthRecordDietService dietService;
    @Resource private HealthRecordExerciseService exerciseService;

    @Override
    public Result<DashboardDTO> getDashboardData(Integer userId, String startDate, String endDate) {
        long startTime = System.currentTimeMillis();

        log.info("开始获取仪表盘数据，userId: {}, startDate: {}, endDate: {}",
                userId, startDate, endDate);

        String today = LocalDate.now().toString();


        // 1. 身体指标趋势
        CompletableFuture<BodyTrendDTO> bodyTrendFuture =
                CompletableFuture.supplyAsync(() ->
                        bodyService.getBodyTrend(userId, startDate, endDate));

        // 2. 血糖趋势
        CompletableFuture<BloodSugarTrendDTO> sugarTrendFuture =
                CompletableFuture.supplyAsync(() ->
                        bloodSugarService.getBloodSugarTrend(userId, startDate, endDate));

        // 3. 饮食统计（全部）
        CompletableFuture<DietStatsDTO> dietStatsFuture =
                CompletableFuture.supplyAsync(() ->
                        dietService.getDietStats(userId, null));

        // 4. 运动统计
        CompletableFuture<ExerciseStatsDTO> exerciseStatsFuture =
                CompletableFuture.supplyAsync(() ->
                        exerciseService.getExerciseStats(userId, startDate, endDate));

        // 5. 最新身体指标
        CompletableFuture<HealthRecordBody> latestBodyFuture =
                CompletableFuture.supplyAsync(() ->
                        bodyService.getLatest(userId));

        // 6. 最新血糖
        CompletableFuture<HealthRecordBloodSugar> latestBsFuture =
                CompletableFuture.supplyAsync(() ->
                        bloodSugarService.getLatest(userId));

        // 7. 今日饮食统计
        CompletableFuture<DietStatsDTO> todayDietFuture =
                CompletableFuture.supplyAsync(() ->
                        dietService.getDietStats(userId, today));

        // 8. 今日运动统计
        CompletableFuture<ExerciseStatsDTO> todayExerciseFuture =
                CompletableFuture.supplyAsync(() ->
                        exerciseService.getExerciseStats(userId, today, today));

        // 等待所有查询完成（最多 10 秒）
        try {
            CompletableFuture.allOf(
                    bodyTrendFuture, sugarTrendFuture, dietStatsFuture, exerciseStatsFuture,
                    latestBodyFuture, latestBsFuture, todayDietFuture, todayExerciseFuture
            ).get(10, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            log.error("仪表盘数据查询超时，userId: {}, 耗时: {}ms",
                    userId, System.currentTimeMillis() - startTime, e);
            throw new RuntimeException("数据查询超时，请稍后重试");
        } catch (Exception e) {
            log.error("获取仪表盘数据失败，userId: {}, 耗时: {}ms",
                    userId, System.currentTimeMillis() - startTime, e);
            throw new RuntimeException("获取仪表盘数据失败：" + e.getMessage());
        }


        DashboardDTO dto = new DashboardDTO();
        dto.setBodyTrend(bodyTrendFuture.join());
        dto.setBloodSugarTrend(sugarTrendFuture.join());
        dto.setDietStats(dietStatsFuture.join());
        dto.setExerciseStats(exerciseStatsFuture.join());

        // 最新身体指标
        //Optional.ofNullable().ifPresent() 就是"如果这个值不为空，就执行下面的操作
        Optional.ofNullable(latestBodyFuture.join()).ifPresent(body -> {
            dto.setLatestWeight(body.getWeight());
            dto.setLatestBmi(body.getBmi());
            dto.setLatestBodyFat(body.getBodyFat());
            dto.setLatestSystolic(body.getSystolicPressure());
            dto.setLatestDiastolic(body.getDiastolicPressure());
            dto.setLatestHeartRate(body.getHeartRate());
        });

        // 最新血糖
        Optional.ofNullable(latestBsFuture.join())
                .ifPresent(bs -> dto.setLatestBloodSugar(bs.getBloodSugar()));

        // 今日饮食汇总
        DietStatsDTO todayDiet = todayDietFuture.join();
        Optional.ofNullable(todayDiet.getCalorieValues())
                .ifPresent(cal -> dto.setTodayCalories(
                        cal.stream().mapToDouble(Double::doubleValue)
                                .sum()));
        Optional.ofNullable(todayDiet.getCarbValues())
                .ifPresent(carb -> dto.setTodayCarbs(
                        carb.stream().mapToDouble(Double::doubleValue).sum()));

        // 今日运动汇总
        ExerciseStatsDTO todayExercise = todayExerciseFuture.join();
        Optional.ofNullable(todayExercise.getCalorieBurnedValues()).ifPresent(
                burnedCalories -> dto.setTodayExerciseCalories(
                        burnedCalories.stream().mapToDouble(Double::doubleValue).sum())
        );

        long endTime = System.currentTimeMillis();
        log.info("仪表盘数据获取完成，userId: {}, 耗时: {}ms", userId, endTime - startTime);

        return Result.ok(dto);
    }
}
