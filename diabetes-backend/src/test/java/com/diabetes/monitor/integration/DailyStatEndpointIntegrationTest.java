package com.diabetes.monitor.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diabetes.monitor.common.cache.MetricsCacheManager;
import com.diabetes.monitor.domain.metric.BloodGlucoseMetrics;
import com.diabetes.monitor.entity.HealthDailyGlucoseStat;
import com.diabetes.monitor.entity.HealthRecordBloodSugar;
import com.diabetes.monitor.mapper.HealthDailyGlucoseStatMapper;
import com.diabetes.monitor.service.HealthRecordBloodSugarService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 日聚合查询侧验收（走完整 HTTP 链路：JWT 鉴权 -> Controller -> Service -> MySQL / Redis）。
 * <p>
 * 覆盖：过去几天读聚合表、今天走实时兜底且不落库、无记录的天补 0 占位、days 夹紧、补算幂等。
 */
@DisplayName("日聚合接口验收：断档补齐 / 今日兜底 / 幂等补算")
class DailyStatEndpointIntegrationTest extends BaseIntegrationTest {

    private static final Integer USER_ID = 2;   // test001
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    /** 本用例插入的血糖记录主键，用于精确清理 */
    private final List<Integer> createdReadingIds = new ArrayList<>();

    @Resource
    private HealthRecordBloodSugarService bloodSugarService;
    @Resource
    private HealthDailyGlucoseStatMapper statMapper;
    @Resource
    private MetricsCacheManager cacheManager;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @AfterEach
    void cleanUp() {
        LocalDate today = LocalDate.now(ZONE);
        // 只删本用例插入的行（按主键），绝不按"时间范围"批量删——
        // 曾经写成"删 user 2 最近 10 天的记录"，结果把演示数据一起清了
        if (!createdReadingIds.isEmpty()) {
            bloodSugarService.removeByIds(createdReadingIds);
            createdReadingIds.clear();
        }
        // 聚合表只涉及本用例补算过的这两天
        statMapper.delete(new LambdaQueryWrapper<HealthDailyGlucoseStat>()
                .eq(HealthDailyGlucoseStat::getUserId, USER_ID)
                .ge(HealthDailyGlucoseStat::getStatDate, today.minusDays(2))
                .lt(HealthDailyGlucoseStat::getStatDate, today));
        // 版本号 +1：让本用例缓存的今日指标失效，避免污染其他用例
        cacheManager.invalidate(USER_ID);
    }

    @Test
    @DisplayName("过去几天读聚合表、今天走实时兜底、无记录的天补 0 占位")
    void trendFillsGapsAndFallsBackToRealtimeForToday() throws Exception {
        LocalDate today = LocalDate.now(ZONE);
        LocalDate d1 = today.minusDays(1);
        LocalDate d2 = today.minusDays(2);

        givenReading(d2, 6.0, 1, LocalTime.of(7, 30));
        givenReading(d2, 8.0, 3, LocalTime.of(9, 30));
        givenReading(d2, 5.5, 4, LocalTime.of(22, 0));
        givenReading(d1, 5.8, 1, LocalTime.of(7, 30));
        givenReading(d1, 7.4, 1, LocalTime.of(12, 0));
        givenReading(d1, 9.0, 3, LocalTime.of(15, 0));
        givenReading(d1, 3.5, 1, LocalTime.of(18, 0));
        givenReading(today, 6.4, 1, LocalTime.of(7, 30));
        givenReading(today, 8.2, 3, LocalTime.of(9, 30));

        String token = getTestUserToken();

        // 只补算"过去"两天，今天故意不聚合（模拟定时任务还没跑到今天）
        callBackfill(token, d2, d1);
        assertThat(countStatRows(d2)).isEqualTo(1);
        assertThat(countStatRows(d1)).isEqualTo(1);
        assertThat(countStatRows(today)).isZero();

        // days=3 -> [d2, d1, today]
        List<Map<String, Object>> data = callTrend(token, 3);
        assertThat(data).hasSize(3);
        assertThat(data.get(0).get("statDate")).isEqualTo(d2.toString());
        assertThat(data.get(1).get("statDate")).isEqualTo(d1.toString());
        assertThat(data.get(2).get("statDate")).isEqualTo(today.toString());

        assertThat(data.get(0).get("sampleCount")).isEqualTo(3);
        assertThat(data.get(1).get("sampleCount")).isEqualTo(4);
        assertThat(data.get(2).get("sampleCount")).isEqualTo(2);   // 今日实时兜底

        // 兜底不落库：聚合表里仍然没有今天的行
        assertThat(countStatRows(today)).isZero();

        // 今日这段确实走了 A1 的缓存（key = metrics:blood:{userId}:1:{today}:{ver}），
        // 并且对象能完整往返序列化 —— 此前 Redis 序列化器缺 JSR310 模块，
        // 写入恒失败且异常被吞，缓存形同虚设，这条断言就是它的回归守卫
        String cacheKey = cacheManager.key(USER_ID, 1, today);
        assertThat(stringRedisTemplate.hasKey(cacheKey)).isTrue();
        BloodGlucoseMetrics cached = cacheManager.get(cacheKey);
        assertThat(cached).isNotNull();
        assertThat(cached.getSampleCount()).isEqualTo(2);
        assertThat(cached.getMeanGlucose()).isEqualTo(7.3);   // (6.4 + 8.2) / 2

        // days=7 -> 长度固定 7，前 4 天无记录 -> sampleCount=0 且指标为 null（断档占位）
        List<Map<String, Object>> week = callTrend(token, 7);
        assertThat(week).hasSize(7);
        assertThat(week.get(0).get("statDate")).isEqualTo(today.minusDays(6).toString());
        assertThat(week.get(0).get("sampleCount")).isEqualTo(0);
        assertThat(week.get(0).get("avgGlucose")).isNull();
        assertThat(week.get(6).get("sampleCount")).isEqualTo(2);
    }

    @Test
    @DisplayName("days 超上限被夹紧到 90")
    void trendClampsDays() throws Exception {
        String token = getTestUserToken();
        assertThat(callTrend(token, 1000)).hasSize(90);
    }

    @Test
    @DisplayName("补算幂等：同一天连补两次仍然只有一行；非法区间被拒绝")
    void backfillIsIdempotentAndValidatesRange() throws Exception {
        LocalDate today = LocalDate.now(ZONE);
        LocalDate d1 = today.minusDays(1);
        givenReading(d1, 5.8, 1, LocalTime.of(7, 30));
        givenReading(d1, 8.0, 3, LocalTime.of(9, 30));

        String token = getTestUserToken();
        callBackfill(token, d1, d1);
        callBackfill(token, d1, d1);
        assertThat(countStatRows(d1)).isEqualTo(1);

        // from 晚于 to -> 业务错误码（不是 200）
        MvcResult bad = mockMvc.perform(post("/api/record/bloodSugar/daily/backfill/" + USER_ID)
                        .header("Authorization", "Bearer " + token)
                        .param("from", today.toString())
                        .param("to", d1.toString()))
                .andReturn();
        assertThat(parseResponse(bad).get("code")).isNotEqualTo(200);
    }

    // ---------- helpers ----------

    private void givenReading(LocalDate date, double value, int measureType, LocalTime time) {
        HealthRecordBloodSugar e = new HealthRecordBloodSugar();
        e.setUserId(USER_ID);
        e.setBloodSugar(value);
        e.setMeasureType(measureType);
        e.setMeasureTime(LocalDateTime.of(date, time));
        bloodSugarService.save(e);
        createdReadingIds.add(e.getId());
    }

    private void callBackfill(String token, LocalDate from, LocalDate to) throws Exception {
        mockMvc.perform(post("/api/record/bloodSugar/daily/backfill/" + USER_ID)
                        .header("Authorization", "Bearer " + token)
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isOk())
                .andReturn();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> callTrend(String token, int days) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/record/bloodSugar/daily/" + USER_ID)
                        .header("Authorization", "Bearer " + token)
                        .param("days", String.valueOf(days)))
                .andExpect(status().isOk())
                .andReturn();
        Map<String, Object> resp = parseResponse(result);
        assertThat(resp.get("code")).isEqualTo(200);
        return (List<Map<String, Object>>) resp.get("data");
    }

    private long countStatRows(LocalDate date) {
        return statMapper.selectCount(new LambdaQueryWrapper<HealthDailyGlucoseStat>()
                .eq(HealthDailyGlucoseStat::getUserId, USER_ID)
                .eq(HealthDailyGlucoseStat::getStatDate, date));
    }
}
