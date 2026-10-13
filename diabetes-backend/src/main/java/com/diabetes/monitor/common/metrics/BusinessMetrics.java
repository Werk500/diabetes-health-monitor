package com.diabetes.monitor.common.metrics;


import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
public class BusinessMetrics {

    @Resource
    private MeterRegistry registry;
    //线程安全
    private final AtomicLong lastJobSuccessSeconds = new AtomicLong(0);

    @PostConstruct
    void init() {
        Gauge.builder("daily_stat_last_success_timestamp", lastJobSuccessSeconds, AtomicLong::get)
                .baseUnit("seconds")
                .description("日聚合任务上次成功的时间戳（epoch 秒）")
                .register(registry);
    }

    //AI调用
    public void aiCall(String scene,long mills){
        Timer.builder("ai_call").tag("scene",scene).register(registry)
                .record(Duration.ofMillis(mills));
    }
    public void aiError(String scene){
        Counter.builder("ai_call_error").tag("scene",scene).register(registry).increment();
    }

    //缓存
    public void cacheHit(String cache){
        cacheAccess(cache,"hit");
    }
    public void cacheMiss(String cache){
        cacheAccess(cache,"miss");
    }

    private void cacheAccess(String cache, String result) {
        Counter.builder("cache_access")
                .tag("cache",cache)
                // 标签名必须是 result：Grafana 看板的 PromQL 按 result="hit|miss" 过滤，
                // 写成 op 会导致看板查不到（埋点了但图是空的）
                .tag("result",result)
                .register(registry)
                // 少了 increment，计数器会被创建但永远停在 0 —— 值看起来"有指标"，实际什么都没统计
                .increment();
    }

    public void cacheError(String cache,String op){
        Counter.builder("cache_error").tag("cache",cache)
                .tag("op",op).register(registry).increment();
    }

    //日聚合任务

    /**
     * 整个日聚合任务花了多久
     * @param mills
     */
    public void jobDuration(long mills){
        Timer.builder("daily_stat_job").register(registry).record(Duration.ofMillis(mills));
    }

    /**
     * 每个用户处理成功还是失败
     * @param success
     */
    public void jobUser(boolean success) {
        Counter.builder("daily_stat_user").tag("result", success ? "success" : "fail")
                .register(registry).increment();
    }

    /**
     * 任务成功完成的时刻
     */
    public void markJobSuccess() {
        lastJobSuccessSeconds.set(Instant.now().getEpochSecond());
    }

    // ---- AI 分析请求受理（按场景 + 结果）----
    public void analysisRequest(String scene, String result) {
        Counter.builder("ai_analysis_request")
                .tag("scene", scene)
                .tag("result", result)   // accepted / unauthorized / timeout / error
                .register(registry)
                .increment();
    }

    // ---- 并行查询耗时（日报场景）----
    public void dailyFetchDuration(String result, long millis) {
        Timer.builder("daily_fetch")
                .tag("result", result)   // success / timeout / error
                .register(registry)
                .record(Duration.ofMillis(millis));
    }

    // ---- 持久化（MySQL 等）----
    public void dbPersist(String table, boolean success) {
        Counter.builder("db_persist")
                .tag("table", table)
                .tag("result", success ? "success" : "fail")
                .register(registry)
                .increment();
    }

    public Timer.Sample startAiCall() {
        return Timer.start(registry);
    }

    public long stopAiCall(String scene, Timer.Sample sample) {
        return sample.stop(Timer.builder("ai_call").tag("scene", scene).register(registry));
    }


}
