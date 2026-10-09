package com.diabetes.monitor.service;

import com.diabetes.monitor.domain.metric.BloodGlucoseMetrics;

public interface MetricsService {
    BloodGlucoseMetrics getBloodSugarMetrics(Integer userId, Integer days);
}
