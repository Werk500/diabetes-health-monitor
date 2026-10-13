package com.diabetes.monitor.job;

import com.diabetes.monitor.common.metrics.BusinessMetrics;
import com.diabetes.monitor.mapper.HealthRecordBloodSugarMapper;
import com.diabetes.monitor.service.HealthDailyStatService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class DailyGlucoseStatJob {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final String LOCK_PREFIX = "lock:job:daily-glucose:";


    @Resource
    private HealthRecordBloodSugarMapper healthRecordBloodSugarMapper;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private HealthDailyStatService statService;
    @Resource
    private BusinessMetrics businessMetrics;


    /** 每天 02:10 聚合"昨天"——这时候昨天的数据基本录完了 */
    @Scheduled(cron = "0 10 2 * * ?")
    public  void aggregateYesterday() {
        aggregateWithLock(LocalDate.now(ZONE).minusDays(1));   // 必须走带锁入口，否则多实例各跑一遍
    }


    public  void aggregateWithLock(LocalDate date) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + date);
        boolean locked = false;

        try {
            // waitTime = 0：拿不到锁立刻跳过，定时任务不该排队等
            // leaseTime = 30min：必须显式给，否则实例宕机后锁不会释放
            locked = lock.tryLock(0,30, TimeUnit.MINUTES);
            if (!locked) {
                log.info("其他实例正在聚合 {}，本次跳过", date);
                return;
            }
            long start = System.currentTimeMillis();
            try {
                aggregate(date);
                // 整轮跑完才算"成功"：抢不到锁直接 return、或中途抛异常，都不该刷新这个时间戳，
                // 否则看板上的"距上次成功多久"会撒谎
                businessMetrics.markJobSuccess();
            } finally {
                businessMetrics.jobDuration(System.currentTimeMillis() - start);
            }
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
            log.warn("获取日聚合锁被中断，date={}", date, e);
        }finally {
            // 只释放自己持有的锁，避免把别的实例的锁删掉
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

    }
    /** 核心：某一天的全量聚合，按用户逐个算 */
    private void aggregate(LocalDate date) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.plusDays(1).atStartOfDay();

        List<Integer> userIds = healthRecordBloodSugarMapper.selectBloodSugarIdsBetween(from, to);
        if (userIds.isEmpty()) {
            log.info("{} 无血糖记录，跳过聚合", date);
            return;
        }

        int ok = 0, fail = 0;
        for (Integer userId : userIds) {

            try {
                statService.aggregateOneDay(userId, date);   // ← 复用 A1 的 Calculator
                ok++;
                businessMetrics.jobUser(true);
            } catch (Exception e) {
                fail++;                                                // 单用户失败不影响整批
                businessMetrics.jobUser(false);
                log.error("日聚合失败 userId={} date={}", userId, date, e);
            }
        }
        log.info("日聚合完成 date={} 用户数={} 成功={} 失败={}", date, userIds.size(), ok, fail);
    }
}

