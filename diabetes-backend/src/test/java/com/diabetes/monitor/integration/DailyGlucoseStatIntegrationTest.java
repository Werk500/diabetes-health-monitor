package com.diabetes.monitor.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diabetes.monitor.domain.metric.BloodGlucoseMetrics;
import com.diabetes.monitor.domain.metric.BloodGlucoseMetricsCalculator;
import com.diabetes.monitor.domain.metric.GlucoseReading;
import com.diabetes.monitor.entity.HealthDailyGlucoseStat;
import com.diabetes.monitor.entity.HealthRecordBloodSugar;
import com.diabetes.monitor.job.DailyGlucoseStatJob;
import com.diabetes.monitor.mapper.HealthDailyGlucoseStatMapper;
import com.diabetes.monitor.service.HealthDailyStatService;
import com.diabetes.monitor.service.HealthRecordBloodSugarService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 血糖日聚合验收：幂等写入 / 落库字段与计算器一致 / 分布式锁。
 * <p>
 * 测试数据用远期日期（2030-01-01）与 test001 用户，跑完即清理，不污染演示数据。
 */
@DisplayName("血糖日聚合：幂等 / 口径一致 / 分布式锁")
class DailyGlucoseStatIntegrationTest extends BaseIntegrationTest {

    private static final LocalDate DAY = LocalDate.of(2030, 1, 1);
    private static final Integer USER_ID = 2;   // test001
    private static final String LOCK_KEY = "lock:job:daily-glucose:" + DAY;

    @Resource
    private HealthRecordBloodSugarService bloodSugarService;
    @Resource
    private HealthDailyStatService statService;
    @Resource
    private HealthDailyGlucoseStatMapper statMapper;
    @Resource
    private BloodGlucoseMetricsCalculator calculator;
    @Resource
    private DailyGlucoseStatJob job;
    @Resource
    private RedissonClient redissonClient;

    @AfterEach
    void cleanUp() {
        bloodSugarService.remove(new LambdaQueryWrapper<HealthRecordBloodSugar>()
                .eq(HealthRecordBloodSugar::getUserId, USER_ID)
                .ge(HealthRecordBloodSugar::getMeasureTime, DAY.atStartOfDay())
                .lt(HealthRecordBloodSugar::getMeasureTime, DAY.plusDays(1).atStartOfDay()));
        statMapper.delete(new LambdaQueryWrapper<HealthDailyGlucoseStat>()
                .eq(HealthDailyGlucoseStat::getUserId, USER_ID)
                .eq(HealthDailyGlucoseStat::getStatDate, DAY));
    }

    @Test
    @DisplayName("落库字段与计算器输出逐项一致（avg 不能错写成 max）")
    void aggregatePersistsAllFields() {
        List<HealthRecordBloodSugar> rows = goldenEntities();
        rows.forEach(bloodSugarService::save);

        int affected = statService.aggregateOneDay(USER_ID, DAY);
        assertThat(affected).isEqualTo(1);

        HealthDailyGlucoseStat stat = findStat();
        assertThat(stat).isNotNull();
        assertThat(stat.getSampleCount()).isEqualTo(10);
        assertThat(stat.getAvgGlucose()).isEqualTo(7.4);
        assertThat(stat.getFastingAvg()).isEqualTo(5.4);
        assertThat(stat.getPostprandialAvg()).isEqualTo(10.5);
        assertThat(stat.getMaxGlucose()).isEqualTo(12.8);
        assertThat(stat.getMinGlucose()).isEqualTo(3.6);
        assertThat(stat.getTirPercent()).isEqualTo(70.0);
        assertThat(stat.getHypoEventCount()).isEqualTo(1);
        assertThat(stat.getCalcVersion()).isEqualTo(1);

        // 回归守卫：avg_glucose 曾经被错写成 max_glucose
        assertThat(stat.getAvgGlucose()).isNotEqualTo(stat.getMaxGlucose());

        // 与计算器直接计算的结果对齐（这才是"实时算 = 离线算"的证明）
        BloodGlucoseMetrics expected = calculator.calculate(
                rows.stream().map(r -> GlucoseReading.builder()
                        .value(r.getBloodSugar())
                        .measureType(r.getMeasureType())
                        .measureTime(r.getMeasureTime())
                        .build()).toList(),
                DAY.atStartOfDay(), DAY.plusDays(1).atStartOfDay());

        assertThat(stat.getSampleCount()).isEqualTo(expected.getSampleCount());
        assertThat(stat.getAvgGlucose()).isEqualTo(expected.getMeanGlucose());
        assertThat(stat.getFastingAvg()).isEqualTo(expected.getFastingAvg());
        assertThat(stat.getPostprandialAvg()).isEqualTo(expected.getPostprandialAvg());
        assertThat(stat.getMaxGlucose()).isEqualTo(expected.getMaxGlucose());
        assertThat(stat.getMinGlucose()).isEqualTo(expected.getMinGlucose());
        assertThat(stat.getTirPercent()).isEqualTo(expected.getTirPercent());
        assertThat(stat.getHypoEventCount()).isEqualTo(expected.getHypoEventCount());
    }

    @Test
    @DisplayName("同一天重复聚合：仍只有一行，值被最新一次覆盖（幂等）")
    void aggregateIsIdempotent() {
        goldenEntities().forEach(bloodSugarService::save);
        statService.aggregateOneDay(USER_ID, DAY);
        HealthDailyGlucoseStat first = findStat();

        // 再补一条高血糖记录后重跑同一天
        bloodSugarService.save(entity(16.0, 3, 600));
        statService.aggregateOneDay(USER_ID, DAY);

        assertThat(countStats()).isEqualTo(1);                       // 没有插出第二行
        HealthDailyGlucoseStat second = findStat();
        assertThat(second.getSampleCount()).isEqualTo(11);           // 值被更新
        assertThat(second.getMaxGlucose()).isEqualTo(16.0);
        assertThat(second.getAvgGlucose()).isNotEqualTo(first.getAvgGlucose());
    }

    @Test
    @DisplayName("分布式锁：锁被其他线程持有时本次直接跳过，锁释放后正常执行")
    void aggregateSkipsWhenLockHeldByAnotherThread() throws Exception {
        goldenEntities().forEach(bloodSugarService::save);

        RLock lock = redissonClient.getLock(LOCK_KEY);
        CountDownLatch acquired = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Thread holder = new Thread(() -> {
            lock.lock(30, TimeUnit.SECONDS);
            acquired.countDown();
            try {
                release.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }, "lock-holder");
        holder.start();
        assertThat(acquired.await(5, TimeUnit.SECONDS)).isTrue();

        // 锁被别的线程持有 -> tryLock(0, ...) 拿不到，应当跳过，不写库
        job.aggregateWithLock(DAY);
        assertThat(countStats()).isZero();

        // 释放锁后再跑一次，证明上一次确实是"被锁挡住"而不是执行失败
        release.countDown();
        holder.join(5000);
        job.aggregateWithLock(DAY);
        assertThat(countStats()).isEqualTo(1);
    }

    // ---------- helpers ----------

    /** 与 A1 黄金数据集同一组数据：10 条，覆盖空腹/餐后2h/睡前 */
    private List<HealthRecordBloodSugar> goldenEntities() {
        return List.of(
                entity(6.2, 1, 0),
                entity(5.8, 1, 60),
                entity(7.4, 1, 120),
                entity(9.1, 3, 180),
                entity(11.3, 3, 240),
                entity(8.6, 3, 300),
                entity(4.1, 1, 360),
                entity(5.5, 4, 420),
                entity(12.8, 3, 480),
                entity(3.6, 1, 540));
    }

    private HealthRecordBloodSugar entity(double value, int measureType, int minutesFromDayStart) {
        HealthRecordBloodSugar e = new HealthRecordBloodSugar();
        e.setUserId(USER_ID);
        e.setBloodSugar(value);
        e.setMeasureType(measureType);
        LocalDateTime time = DAY.atStartOfDay().plusMinutes(minutesFromDayStart);
        e.setMeasureTime(time);
        return e;
    }

    private HealthDailyGlucoseStat findStat() {
        return statMapper.selectOne(new LambdaQueryWrapper<HealthDailyGlucoseStat>()
                .eq(HealthDailyGlucoseStat::getUserId, USER_ID)
                .eq(HealthDailyGlucoseStat::getStatDate, DAY));
    }

    private long countStats() {
        return statMapper.selectCount(new LambdaQueryWrapper<HealthDailyGlucoseStat>()
                .eq(HealthDailyGlucoseStat::getUserId, USER_ID)
                .eq(HealthDailyGlucoseStat::getStatDate, DAY));
    }
}
