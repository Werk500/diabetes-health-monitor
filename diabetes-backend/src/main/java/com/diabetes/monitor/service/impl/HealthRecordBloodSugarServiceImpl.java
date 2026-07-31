package com.diabetes.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.diabetes.monitor.dto.BloodSugarTrendDTO;
import com.diabetes.monitor.entity.HealthRecordBloodSugar;
import com.diabetes.monitor.mapper.HealthRecordBloodSugarMapper;
import com.diabetes.monitor.service.HealthRecordBloodSugarService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class HealthRecordBloodSugarServiceImpl extends ServiceImpl<HealthRecordBloodSugarMapper, HealthRecordBloodSugar> implements HealthRecordBloodSugarService {

    @Override
    public BloodSugarTrendDTO getBloodSugarTrend(Integer userId, String startDate, String endDate) {
        LambdaQueryWrapper<HealthRecordBloodSugar> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthRecordBloodSugar::getUserId, userId)
               .ge(startDate != null, HealthRecordBloodSugar::getMeasureTime, startDate + " 00:00:00")
               .le(endDate != null, HealthRecordBloodSugar::getMeasureTime, endDate + " 23:59:59")
               .orderByAsc(HealthRecordBloodSugar::getMeasureTime);
        List<HealthRecordBloodSugar> records = list(wrapper);

        BloodSugarTrendDTO dto = new BloodSugarTrendDTO();
        List<String> dates = new ArrayList<>();
        List<Double> fasting = new ArrayList<>();
        List<Double> beforeMeal = new ArrayList<>();
        List<Double> afterMeal = new ArrayList<>();
        List<Double> bedtime = new ArrayList<>();

        for (HealthRecordBloodSugar r : records) {
            String dateStr = r.getMeasureTime().toLocalDate().toString();
            switch (r.getMeasureType()) {
                case 1: dates.add("空腹 " + dateStr); fasting.add(r.getBloodSugar()); break;
                case 2: dates.add("餐前 " + dateStr); beforeMeal.add(r.getBloodSugar()); break;
                case 3: dates.add("餐后 " + dateStr); afterMeal.add(r.getBloodSugar()); break;
                case 4: dates.add("睡前 " + dateStr); bedtime.add(r.getBloodSugar()); break;
                case 5: dates.add("凌晨 " + dateStr); break;
            }
        }

        dto.setDates(dates);
        dto.setFastingValues(fasting);
        dto.setBeforeMealValues(beforeMeal);
        dto.setAfterMealValues(afterMeal);
        dto.setBedtimeValues(bedtime);
        return dto;
    }

    @Override
    public HealthRecordBloodSugar getLatest(Integer userId) {
        LambdaQueryWrapper<HealthRecordBloodSugar> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthRecordBloodSugar::getUserId, userId)
               .orderByDesc(HealthRecordBloodSugar::getMeasureTime)
               .last("LIMIT 1");
        return getOne(wrapper);
    }
}
