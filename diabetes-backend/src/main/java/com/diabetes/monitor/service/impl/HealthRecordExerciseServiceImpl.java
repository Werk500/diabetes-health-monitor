package com.diabetes.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.diabetes.monitor.dto.ExerciseStatsDTO;
import com.diabetes.monitor.entity.HealthRecordExercise;
import com.diabetes.monitor.mapper.HealthRecordExerciseMapper;
import com.diabetes.monitor.service.HealthRecordExerciseService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HealthRecordExerciseServiceImpl extends ServiceImpl<HealthRecordExerciseMapper, HealthRecordExercise> implements HealthRecordExerciseService {

    @Override
    public ExerciseStatsDTO getExerciseStats(Integer userId, String startDate, String endDate) {
        LambdaQueryWrapper<HealthRecordExercise> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthRecordExercise::getUserId, userId)
               .ge(startDate != null, HealthRecordExercise::getExerciseDate, startDate)
               .le(endDate != null, HealthRecordExercise::getExerciseDate, endDate)
               .orderByAsc(HealthRecordExercise::getExerciseDate);
        List<HealthRecordExercise> records = list(wrapper);

        ExerciseStatsDTO dto = new ExerciseStatsDTO();
        dto.setDates(records.stream().map(r -> r.getExerciseDate().toString()).collect(Collectors.toList()));
        dto.setCalorieBurnedValues(records.stream().map(HealthRecordExercise::getCaloriesBurned).collect(Collectors.toList()));
        dto.setDurationValues(records.stream().map(HealthRecordExercise::getDurationMinutes).collect(Collectors.toList()));
        dto.setExerciseTypes(records.stream().map(r -> String.valueOf(r.getExerciseTypeId())).collect(Collectors.toList()));
        return dto;
    }
}
