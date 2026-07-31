package com.diabetes.monitor.controller;

import com.diabetes.monitor.common.Result;
import com.diabetes.monitor.dto.*;
import com.diabetes.monitor.entity.*;
import com.diabetes.monitor.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "数据看板", description = "用户健康数据概览仪表盘")
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired private HealthRecordBodyService bodyService;
    @Autowired private HealthRecordBloodSugarService bloodSugarService;
    @Autowired private HealthRecordDietService dietService;
    @Autowired private HealthRecordExerciseService exerciseService;

    @Operation(summary = "获取用户仪表盘数据")
    @GetMapping("/{userId}")
    public Result dashboard(@PathVariable Integer userId, @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate) {
        DashboardDTO dto = new DashboardDTO();
        dto.setBodyTrend(bodyService.getBodyTrend(userId, startDate, endDate));
        dto.setBloodSugarTrend(bloodSugarService.getBloodSugarTrend(userId, startDate, endDate));
        dto.setDietStats(dietService.getDietStats(userId, null));
        dto.setExerciseStats(exerciseService.getExerciseStats(userId, startDate, endDate));

        HealthRecordBody latestBody = bodyService.getLatest(userId);
        if (latestBody != null) {
            dto.setLatestWeight(latestBody.getWeight());
            dto.setLatestBmi(latestBody.getBmi());
            dto.setLatestBodyFat(latestBody.getBodyFat());
            dto.setLatestSystolic(latestBody.getSystolicPressure());
            dto.setLatestDiastolic(latestBody.getDiastolicPressure());
            dto.setLatestHeartRate(latestBody.getHeartRate());
        }

        HealthRecordBloodSugar latestBs = bloodSugarService.getLatest(userId);
        if (latestBs != null) dto.setLatestBloodSugar(latestBs.getBloodSugar());

        String today = LocalDate.now().toString();
        DietStatsDTO dietStats = dietService.getDietStats(userId, today);
        if (dietStats.getCalorieValues() != null) {
            dto.setTodayCalories(dietStats.getCalorieValues().stream().mapToDouble(Double::doubleValue).sum());
        }
        if (dietStats.getCarbValues() != null) {
            dto.setTodayCarbs(dietStats.getCarbValues().stream().mapToDouble(Double::doubleValue).sum());
        }

        ExerciseStatsDTO exStats = exerciseService.getExerciseStats(userId, today, today);
        if (exStats.getCalorieBurnedValues() != null) {
            dto.setTodayExerciseCalories(exStats.getCalorieBurnedValues().stream().mapToDouble(Double::doubleValue).sum());
        }

        return Result.ok(dto);
    }
}