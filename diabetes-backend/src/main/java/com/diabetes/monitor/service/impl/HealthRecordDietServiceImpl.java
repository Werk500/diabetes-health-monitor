package com.diabetes.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.diabetes.monitor.dto.DietStatsDTO;
import com.diabetes.monitor.entity.HealthRecordDiet;
import com.diabetes.monitor.mapper.HealthRecordDietMapper;
import com.diabetes.monitor.service.HealthRecordDietService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class HealthRecordDietServiceImpl extends ServiceImpl<HealthRecordDietMapper, HealthRecordDiet> implements HealthRecordDietService {

    private static final Map<Integer, String> MEAL_NAMES = new HashMap<>();
    static {
        MEAL_NAMES.put(1, "早餐");
        MEAL_NAMES.put(2, "午餐");
        MEAL_NAMES.put(3, "晚餐");
        MEAL_NAMES.put(4, "加餐");
    }

    @Override
    public DietStatsDTO getDietStats(Integer userId, String date) {
        LambdaQueryWrapper<HealthRecordDiet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthRecordDiet::getUserId, userId);
        if (date != null) {
            wrapper.ge(HealthRecordDiet::getEatTime, date + " 00:00:00")
                   .le(HealthRecordDiet::getEatTime, date + " 23:59:59");
        }
        List<HealthRecordDiet> records = list(wrapper);

        DietStatsDTO dto = new DietStatsDTO();
        dto.setMealNames(records.stream().map(r -> r.getFoodName() + "(" + MEAL_NAMES.getOrDefault(r.getMealType(), "其他") + ")").collect(Collectors.toList()));
        dto.setCalorieValues(records.stream().map(HealthRecordDiet::getCalories).collect(Collectors.toList()));
        dto.setCarbValues(records.stream().map(HealthRecordDiet::getCarbs).collect(Collectors.toList()));
        dto.setProteinValues(records.stream().map(HealthRecordDiet::getProtein).collect(Collectors.toList()));
        dto.setFatValues(records.stream().map(HealthRecordDiet::getFat).collect(Collectors.toList()));
        return dto;
    }
}
