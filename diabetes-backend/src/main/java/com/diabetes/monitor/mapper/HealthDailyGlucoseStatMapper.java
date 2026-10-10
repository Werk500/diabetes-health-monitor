package com.diabetes.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.diabetes.monitor.entity.HealthDailyGlucoseStat;
import org.apache.ibatis.annotations.Insert;

public interface HealthDailyGlucoseStatMapper extends BaseMapper<HealthDailyGlucoseStat> {

    @Insert("""
    INSERT INTO health_daily_glucose_stat
        (user_id, stat_date, sample_count, avg_glucose, fasting_avg, postprandial_avg,
         max_glucose, min_glucose, tir_percent, hypo_event_count, calc_version)
    VALUES (#{userId}, #{statDate}, #{sampleCount}, #{avgGlucose}, #{fastingAvg}, #{postprandialAvg},
            #{maxGlucose}, #{minGlucose}, #{tirPercent}, #{hypoEventCount}, #{calcVersion})
    ON DUPLICATE KEY UPDATE
        sample_count = VALUES(sample_count),
        avg_glucose = VALUES(avg_glucose),
        fasting_avg = VALUES(fasting_avg),
        postprandial_avg = VALUES(postprandial_avg),
        max_glucose = VALUES(max_glucose),
        min_glucose = VALUES(min_glucose),
        tir_percent = VALUES(tir_percent),
        hypo_event_count = VALUES(hypo_event_count),
        calc_version = VALUES(calc_version)
    """)
    int upsert(HealthDailyGlucoseStat stat);
}
