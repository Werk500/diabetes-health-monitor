package com.diabetes.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.diabetes.monitor.dto.BodyTrendDTO;
import com.diabetes.monitor.entity.HealthRecordBody;
import com.diabetes.monitor.mapper.HealthRecordBodyMapper;
import com.diabetes.monitor.service.HealthRecordBodyService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HealthRecordBodyServiceImpl extends ServiceImpl<HealthRecordBodyMapper, HealthRecordBody> implements HealthRecordBodyService {

    @Override
    public BodyTrendDTO getBodyTrend(Integer userId, String startDate, String endDate) {
        LambdaQueryWrapper<HealthRecordBody> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthRecordBody::getUserId, userId)
               .ge(startDate != null, HealthRecordBody::getRecordDate, startDate)
               .le(endDate != null, HealthRecordBody::getRecordDate, endDate)
               .orderByAsc(HealthRecordBody::getRecordDate);
        List<HealthRecordBody> records = list(wrapper);

        BodyTrendDTO dto = new BodyTrendDTO();
        dto.setDates(records.stream().map(r -> r.getRecordDate().toString()).collect(Collectors.toList()));
        dto.setWeightValues(records.stream().map(HealthRecordBody::getWeight).collect(Collectors.toList()));
        dto.setBmiValues(records.stream().map(HealthRecordBody::getBmi).collect(Collectors.toList()));
        dto.setBodyFatValues(records.stream().map(HealthRecordBody::getBodyFat).collect(Collectors.toList()));
        dto.setSystolicValues(records.stream().map(HealthRecordBody::getSystolicPressure).collect(Collectors.toList()));
        dto.setDiastolicValues(records.stream().map(HealthRecordBody::getDiastolicPressure).collect(Collectors.toList()));
        dto.setHeartRateValues(records.stream().map(HealthRecordBody::getHeartRate).collect(Collectors.toList()));
        return dto;
    }

    @Override
    public HealthRecordBody getLatest(Integer userId) {
        LambdaQueryWrapper<HealthRecordBody> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthRecordBody::getUserId, userId)
               .orderByDesc(HealthRecordBody::getRecordDate)
               .last("LIMIT 1");
        return getOne(wrapper);
    }
}
