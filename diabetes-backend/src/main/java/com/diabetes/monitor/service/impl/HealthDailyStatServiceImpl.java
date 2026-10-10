package com.diabetes.monitor.service.impl;

import com.diabetes.monitor.domain.metric.BloodGlucoseMetrics;
import com.diabetes.monitor.domain.metric.BloodGlucoseMetricsCalculator;
import com.diabetes.monitor.domain.metric.GlucoseReading;
import com.diabetes.monitor.entity.HealthDailyGlucoseStat;
import com.diabetes.monitor.entity.HealthRecordBloodSugar;
import com.diabetes.monitor.mapper.HealthDailyGlucoseStatMapper;
import com.diabetes.monitor.service.HealthDailyStatService;
import com.diabetes.monitor.service.HealthRecordBloodSugarService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class HealthDailyStatServiceImpl implements HealthDailyStatService {

    @Resource
    private HealthRecordBloodSugarService bloodSugarService;
    @Resource
    private BloodGlucoseMetricsCalculator bloodGlucoseMetricsCalculator;
    @Resource
    private HealthDailyGlucoseStatMapper healthDailyGlucoseStatMapper;

    private static final int CALC_VERSION = 1;

    @Override
    public int aggregateOneDay(Integer userId, LocalDate date) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.plusDays(1).atStartOfDay();

        // 1. 查当天明细
        List<HealthRecordBloodSugar> entities = bloodSugarService.lambdaQuery()
                .select(HealthRecordBloodSugar::getBloodSugar,
                        HealthRecordBloodSugar::getMeasureType,
                        HealthRecordBloodSugar::getMeasureTime)
                .eq(HealthRecordBloodSugar::getUserId, userId)
                .ge(HealthRecordBloodSugar::getMeasureTime, from)
                .lt(HealthRecordBloodSugar::getMeasureTime, to)
                .list();

        if (entities.isEmpty()) {
            return 0;
        }

        //2.映射成 List<GlucoseReading>
        List<GlucoseReading> readings = entities.stream()
                .map(r -> GlucoseReading.builder()
                        .value(r.getBloodSugar())
                        .measureType(r.getMeasureType())
                        .measureTime(r.getMeasureTime())
                        .build())
                .toList();
        //3. 复用 A1 计算器，保证口径一致
        BloodGlucoseMetrics metrics = bloodGlucoseMetricsCalculator.calculate(readings, from, to);

        // 4. 组装 HealthDailyGlucoseStat（calcVersion = CALC_VERSION）→ mapper.upsert(...)
        HealthDailyGlucoseStat stat = HealthDailyGlucoseStat.builder()
                .userId(userId)
                .statDate(date)
                .sampleCount(metrics.getSampleCount())
                .avgGlucose(metrics.getMeanGlucose())
                .fastingAvg(metrics.getFastingAvg())
                .postprandialAvg(metrics.getPostprandialAvg())
                .maxGlucose(metrics.getMaxGlucose())
                .minGlucose(metrics.getMinGlucose())
                .tirPercent(metrics.getTirPercent())
                .hypoEventCount(metrics.getHypoEventCount())
                .calcVersion(CALC_VERSION)
                .build();
        int affected = healthDailyGlucoseStatMapper.upsert(stat);

        return affected;
    }

    /**
     * 补算一个用户在 [from, to] 闭区间内每一天的聚合。
     * 返回实际写入的天数（当天没有记录的天会被跳过）。
     * 单天失败不影响其余天，方便任务挂掉后按区间回补。
     */
    @Override
    public int backfill(Integer userId, LocalDate from, LocalDate to) {
        if (userId == null || from == null || to == null || to.isBefore(from)) {
            return 0;
        }
        int days = 0;
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            try {
                days += aggregateOneDay(userId, d);
            } catch (Exception e) {
                log.error("补算失败 userId={} date={}", userId, d, e);
            }
        }
        log.info("补算完成 userId={} 区间={}~{} 写入天数={}", userId, from, to, days);
        return days;
    }
}
