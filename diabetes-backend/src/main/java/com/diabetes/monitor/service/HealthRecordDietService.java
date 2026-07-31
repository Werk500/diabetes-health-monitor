package com.diabetes.monitor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.diabetes.monitor.dto.DietStatsDTO;
import com.diabetes.monitor.entity.HealthRecordDiet;

public interface HealthRecordDietService extends IService<HealthRecordDiet> {
    DietStatsDTO getDietStats(Integer userId, String date);
}
