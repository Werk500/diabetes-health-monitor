package com.diabetes.monitor.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.diabetes.monitor.dto.BodyTrendDTO;
import com.diabetes.monitor.entity.HealthRecordBody;
import java.util.List;

public interface HealthRecordBodyService extends IService<HealthRecordBody> {
    BodyTrendDTO getBodyTrend(Integer userId, String startDate, String endDate);
    HealthRecordBody getLatest(Integer userId);
}
