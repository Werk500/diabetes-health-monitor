package com.diabetes.monitor.common.cache;

import com.diabetes.monitor.common.metrics.BusinessMetrics;
import com.diabetes.monitor.domain.metric.BloodGlucoseMetrics;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;

@Slf4j
@Component
public class MetricsCacheManager {
    private static final Duration TTL = Duration.ofMinutes(10);
    private static final String KEY_PREFIX = "metrics:blood:";
    /** 版本号 key 只有这一种拼法：读和写都走 verKey()，避免两处格式各写一遍写歪 */
    private static final String VER_KEY_FMT = "metrics:blood:ver:%d";

    @Resource
    private RedisTemplate<String,Object> redisTemplate;// 存 JSON，跟 RedisConfig 的 GenericJackson2 一致
    @Resource
    private  StringRedisTemplate stringRedisTemplate;
    @Resource
    private BusinessMetrics businessMetrics;

    public String key(Integer userId, int days, LocalDate date) {
        return KEY_PREFIX + userId + ":" + days + ":" + date + ":" + version(userId);
    }

    public long version(Integer userId) {
        try {
            String v = stringRedisTemplate.opsForValue().get(verKey(userId));
            return v == null ? 0L : Long.parseLong(v);
        } catch (Exception e) {
            // Redis 不可用 / 值被写坏：按版本 0 处理，主流程继续
            log.warn("读取血糖指标缓存版本号失败，按 0 处理: userId={}, err={}", userId, e.getMessage());
            return 0L;
        }
    }

    public BloodGlucoseMetrics get(String key){
        if(key == null||key.isBlank()){
            return null;
        }
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached == null) {
                businessMetrics.cacheMiss("metrics");
                return null;                 // 未命中 or 已过期
            }
            if (cached instanceof BloodGlucoseMetrics m) {
                businessMetrics.cacheHit("metrics");
                return m;                    // 命中
            }
            // 类型不符：缓存被污染，当作未命中处理
            return null;
        } catch (Exception e) {
            // Redis 不可用 / 反序列化失败 → 降级为未命中，保证主流程可用
            businessMetrics.cacheError("metrics","get");
            log.warn("读取血糖指标缓存失败，降级为回源查询: key={}, err={}", key, e.getMessage());
            return null;
        }
    }
    public void put(String key, BloodGlucoseMetrics m) {
        if (key == null || key.isBlank() || m == null) return;
        try {
            redisTemplate.opsForValue().set(key, m, TTL);
        } catch (Exception e) {
            // 写缓存失败不影响主流程
            businessMetrics.cacheError("metrics","put");
            log.warn("写入血糖指标缓存失败，忽略: key={}, err={}", key, e.getMessage());
        }
    }

    /** 数据变了就 INCR 版本号，旧缓存自然失效（不需要 KEYS 扫描删除） */
    public void invalidate(Integer userId) {
        if (userId == null) {
            return;
        }
        try {
            // 计数器必须走 StringRedisTemplate：Redis 的 INCR 只认纯数字字符串，
            // 用 JSON 序列化器写进去会变成带类型信息的 JSON，后续 INCR 直接报错
            stringRedisTemplate.opsForValue().increment(verKey(userId));
        } catch (Exception e) {
            businessMetrics.cacheError("metrics","invalidate");
            log.warn("失效血糖指标缓存失败，旧数据将靠 TTL 自然过期: userId={}, err={}", userId, e.getMessage());
        }
    }

    private String verKey(Integer userId) {
        return String.format(VER_KEY_FMT, userId);
    }
}
