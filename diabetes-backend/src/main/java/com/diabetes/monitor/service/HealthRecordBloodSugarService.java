package com.diabetes.monitor.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.diabetes.monitor.dto.BloodSugarTrendDTO;
import com.diabetes.monitor.entity.HealthRecordBloodSugar;

public interface HealthRecordBloodSugarService extends IService<HealthRecordBloodSugar> {
    BloodSugarTrendDTO getBloodSugarTrend(Integer userId, String startDate, String endDate);
    HealthRecordBloodSugar getLatest(Integer userId);
}
