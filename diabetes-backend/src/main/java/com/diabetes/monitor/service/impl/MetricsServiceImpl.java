package com.diabetes.monitor.service.impl;

import com.diabetes.monitor.common.cache.MetricsCacheManager;
import com.diabetes.monitor.domain.metric.BloodGlucoseMetrics;
import com.diabetes.monitor.domain.metric.BloodGlucoseMetricsCalculator;
import com.diabetes.monitor.domain.metric.GlucoseReading;
import com.diabetes.monitor.entity.HealthRecordBloodSugar;
import com.diabetes.monitor.service.HealthRecordBloodSugarService;
import com.diabetes.monitor.service.MetricsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 血糖指标查询服务
 */
@Service
@Slf4j
public class MetricsServiceImpl implements MetricsService {

    private static final int DEFAULT_DAYS = 14;
    private static final int MAX_DAYS = 90;
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    /** 每个缓存 key 一把本地锁，防止同 key 并发回源（缓存击穿） */
    private final ConcurrentHashMap<String, ReentrantLock> keyLocks = new ConcurrentHashMap<>();

    @Resource
    private MetricsCacheManager cacheManager;
    @Resource
    private BloodGlucoseMetricsCalculator calculator;
    @Resource
    private HealthRecordBloodSugarService bloodSugarService;

    @Override
    public BloodGlucoseMetrics getBloodSugarMetrics(Integer userId, Integer days) {

        int d = (days == null || days <= 0) ? DEFAULT_DAYS : Math.min(days, MAX_DAYS);


        //时间窗口（左闭右开，含今天）
        LocalDate today = LocalDate.now(ZONE);
        LocalDateTime from = today.minusDays(d - 1).atStartOfDay();
        LocalDateTime to   = today.plusDays(1).atStartOfDay();

        //命中缓存直接返回
        String key = cacheManager.key(userId, d, today);
        BloodGlucoseMetrics cached = cacheManager.get(key);
        if (cached != null) {
            return cached;
        }

        //未命中缓存：同 key 加本地锁，防止并发回源（缓存击穿）
        ReentrantLock lock = keyLocks.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
        try {
            //双重检查：可能已被前一个线程回填
            cached = cacheManager.get(key);
            if (cached != null) {
                return cached;
            }

            // 回源 DB，只取必要列
            //从数据库查出指定用户在指定时间窗口内的所有血糖记录
            //并把数据库实体（Entity）转换成领域读模型（Domain Model），供纯计算器使用。
            List<GlucoseReading> readings = bloodSugarService.lambdaQuery()
                    .eq(HealthRecordBloodSugar::getUserId, userId)
                    .ge(HealthRecordBloodSugar::getMeasureTime, from)
                    .lt(HealthRecordBloodSugar::getMeasureTime, to)
                    .select(HealthRecordBloodSugar::getBloodSugar,
                            HealthRecordBloodSugar::getMeasureType,
                            HealthRecordBloodSugar::getMeasureTime)
                    .list()
                    .stream()
                    .map(r -> GlucoseReading.builder()
                            .value(r.getBloodSugar())
                            .measureType(r.getMeasureType())
                            .measureTime(r.getMeasureTime())
                            .build())
                    .toList();

            // 计算 + 回填
            BloodGlucoseMetrics metrics = calculator.calculate(readings, from, to);
            cacheManager.put(key, metrics);
            return metrics;
        } finally {
            lock.unlock();
            // key 里带日期和版本号，用完即清理，避免 keyLocks 无限增长
            if (!lock.hasQueuedThreads()) {
                keyLocks.remove(key, lock);
            }
        }
    }
}
