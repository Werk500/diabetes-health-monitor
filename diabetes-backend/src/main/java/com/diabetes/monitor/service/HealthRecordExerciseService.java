package com.diabetes.monitor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.diabetes.monitor.dto.ExerciseStatsDTO;
import com.diabetes.monitor.entity.HealthRecordExercise;

public interface HealthRecordExerciseService extends IService<HealthRecordExercise> {
    ExerciseStatsDTO getExerciseStats(Integer userId, String startDate, String endDate);
}
