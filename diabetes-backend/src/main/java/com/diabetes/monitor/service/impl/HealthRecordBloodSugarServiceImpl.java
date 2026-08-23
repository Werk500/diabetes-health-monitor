package com.diabetes.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.diabetes.monitor.dto.BloodSugarTrendDTO;
import com.diabetes.monitor.entity.HealthRecordBloodSugar;
import com.diabetes.monitor.mapper.HealthRecordBloodSugarMapper;
import com.diabetes.monitor.service.HealthRecordBloodSugarService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class HealthRecordBloodSugarServiceImpl extends ServiceImpl<HealthRecordBloodSugarMapper, HealthRecordBloodSugar> implements HealthRecordBloodSugarService {

    @Override
    public BloodSugarTrendDTO getBloodSugarTrend(Integer userId, String startDate, String endDate) {

        if (userId == null) {
            log.warn("getBloodSugarTrend: userId为空");
            return new BloodSugarTrendDTO();
        }

        log.info("开始查询血糖趋势，userId: {}, startDate: {}, endDate: {}", userId, startDate, endDate);


        LambdaQueryWrapper<HealthRecordBloodSugar> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthRecordBloodSugar::getUserId, userId)
               .ge(startDate != null, HealthRecordBloodSugar::getMeasureTime, startDate + " 00:00:00")
               .le(endDate != null, HealthRecordBloodSugar::getMeasureTime, endDate + " 23:59:59")
               .orderByAsc(HealthRecordBloodSugar::getMeasureTime);
        List<HealthRecordBloodSugar> records = list(wrapper);

        if (records.isEmpty()) {
            return new BloodSugarTrendDTO();
        }

        return convertToTrendDTO(records);
    }

    /**
     * 将血糖记录列表转换为趋势DTO
     * 按测量类型分类：空腹(1)、餐前(2)、餐后(3)、睡前(4)、凌晨(5)
     */
    private BloodSugarTrendDTO convertToTrendDTO(List<HealthRecordBloodSugar> records) {

        // 使用Map按类型分组，便于处理
        Map<Integer, List<HealthRecordBloodSugar>> groupedByType = records.stream().
                collect(Collectors.groupingBy(
                        r -> r.getMeasureType() != null ? r.getMeasureType() : 0,
                        Collectors.toList()));

        BloodSugarTrendDTO dto = new BloodSugarTrendDTO();

        // 初始化所有列表
        List<String> dates = new ArrayList<>();
        List<Double> fastingValues = new ArrayList<>();
        List<Double> beforeMealValues = new ArrayList<>();
        List<Double> afterMealValues = new ArrayList<>();
        List<Double> bedtimeValues = new ArrayList<>();

        for (HealthRecordBloodSugar r : records) {
            String dateStr = r.getMeasureTime().toLocalDate().toString();
            switch (r.getMeasureType()) {
                case 1: dates.add("空腹 " + dateStr); fastingValues.add(r.getBloodSugar()); break;
                case 2: dates.add("餐前 " + dateStr); beforeMealValues.add(r.getBloodSugar()); break;
                case 3: dates.add("餐后 " + dateStr); afterMealValues.add(r.getBloodSugar()); break;
                case 4: dates.add("睡前 " + dateStr); bedtimeValues.add(r.getBloodSugar()); break;
                case 5: dates.add("凌晨 " + dateStr); break;
            }
        }

        dto.setDates(dates);
        dto.setFastingValues(fastingValues);
        dto.setBeforeMealValues(beforeMealValues);
        dto.setAfterMealValues(afterMealValues);
        dto.setBedtimeValues(bedtimeValues);
        return dto;
    }


    /**
     * 获取用户最新的一条血糖记录
     *
     * @param userId 用户ID，不能为空
     * @return 最新的血糖记录，如果没有则返回null
     */
    @Override
    public HealthRecordBloodSugar getLatest(Integer userId) {
        LambdaQueryWrapper<HealthRecordBloodSugar> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthRecordBloodSugar::getUserId, userId)
               .orderByDesc(HealthRecordBloodSugar::getMeasureTime)
               .last("LIMIT 1");
        return getOne(wrapper);
    }
}
