package com.diabetes.monitor.dto;

import lombok.Data;

@Data
public class DashboardDTO {
    private BodyTrendDTO bodyTrend;
    private BloodSugarTrendDTO bloodSugarTrend;
    private DietStatsDTO dietStats;
    private ExerciseStatsDTO exerciseStats;
    private Double latestWeight;
    private Double latestBmi;
    private Double latestBodyFat;
    private Integer latestSystolic;
    private Integer latestDiastolic;
    private Integer latestHeartRate;
    private Double latestBloodSugar;
    private Double todayCalories;
    private Double todayCarbs;
    private Double todayExerciseCalories;
}
