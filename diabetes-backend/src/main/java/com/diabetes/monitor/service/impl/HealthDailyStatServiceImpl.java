package com.diabetes.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diabetes.monitor.domain.metric.BloodGlucoseMetrics;
import com.diabetes.monitor.domain.metric.BloodGlucoseMetricsCalculator;
import com.diabetes.monitor.domain.metric.GlucoseReading;
import com.diabetes.monitor.dto.DailyGlucoseStatVO;
import com.diabetes.monitor.entity.HealthDailyGlucoseStat;
import com.diabetes.monitor.entity.HealthRecordBloodSugar;
import com.diabetes.monitor.mapper.HealthDailyGlucoseStatMapper;
import com.diabetes.monitor.service.HealthDailyStatService;
import com.diabetes.monitor.service.HealthRecordBloodSugarService;
import com.diabetes.monitor.service.MetricsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class HealthDailyStatServiceImpl implements HealthDailyStatService {

    private static final int DEFAULT_DAYS = 30;   // 前端不传 / 传了非法值时的默认窗口
    private static final int MAX_DAYS = 90;       // 上限：防止一次拉太多天
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    @Resource
    private HealthRecordBloodSugarService bloodSugarService;
    @Resource
    private BloodGlucoseMetricsCalculator bloodGlucoseMetricsCalculator;
    @Resource
    private HealthDailyGlucoseStatMapper healthDailyGlucoseStatMapper;
    @Resource
    private MetricsService metricsService;

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

    /** 读聚合表：区间内每天的聚合结果（不包含今日兜底） */
    @Override
    public List<HealthDailyGlucoseStat> listByRange(Integer userId, LocalDate from, LocalDate to) {

        if(from.isAfter(to)) {
            return List.of();
        }

        return healthDailyGlucoseStatMapper.selectList(new LambdaQueryWrapper<HealthDailyGlucoseStat>()
                .eq(HealthDailyGlucoseStat::getUserId, userId)
                .ge(HealthDailyGlucoseStat::getStatDate, from)
                .le(HealthDailyGlucoseStat::getStatDate, to)
                .orderByAsc(HealthDailyGlucoseStat::getStatDate));
    }

    /** 趋势：聚合表 + 今日实时兜底，按日期升序补齐断档 */
    @Override
    public List<DailyGlucoseStatVO> trend(Integer userId, int days) {
        int d = (days<=0)? DEFAULT_DAYS : Math.min(days, MAX_DAYS);

        LocalDate today = LocalDate.now(ZONE);
        LocalDate from = today.minusDays(d - 1);

        // 1) 聚合表里 [from, 昨天] 的部分
        Map<LocalDate, DailyGlucoseStatVO> byDate = listByRange(userId, from, today.minusDays(1))
                .stream().collect(Collectors.toMap(HealthDailyGlucoseStat::getStatDate, this::toVO));

        // 2) 今日兜底：days=1 时 A1 的窗口正好是"今天 00:00 ~ 明天 00:00"，
        //    而且它自带 Redis 缓存，所以今天这段不会反复打 DB
        BloodGlucoseMetrics todayMetrics = metricsService.getBloodSugarMetrics(userId, 1);
        if(todayMetrics!=null&&todayMetrics.getSampleCount()>0){
            byDate.put(today,toVO(today,todayMetrics));
        }

        // 3) 从 from 顺序补到 today：无记录的天用 empty 占位，
        //    前端按索引对齐日期轴时才不会错位
        List<DailyGlucoseStatVO> result = new ArrayList<>(d);
        for(LocalDate date = from; !date.isAfter(today);date = date.plusDays(1)){
            result.add(byDate.getOrDefault(date,DailyGlucoseStatVO.empty(date)));
        }

        return result;
    }

    private DailyGlucoseStatVO toVO(HealthDailyGlucoseStat stat) {
        DailyGlucoseStatVO vo = new DailyGlucoseStatVO();
        vo.setStatDate(stat.getStatDate());
        vo.setSampleCount(stat.getSampleCount());
        vo.setAvgGlucose(stat.getAvgGlucose());
        vo.setFastingAvg(stat.getFastingAvg());
        vo.setPostprandialAvg(stat.getPostprandialAvg());
        vo.setMaxGlucose(stat.getMaxGlucose());
        vo.setMinGlucose(stat.getMinGlucose());
        vo.setTirPercent(stat.getTirPercent());
        vo.setHypoEventCount(stat.getHypoEventCount());
        return vo;
    }

    /** 今日实时指标 → VO（表里还没有这一天的行） */
    private DailyGlucoseStatVO toVO(LocalDate date, BloodGlucoseMetrics m) {
        DailyGlucoseStatVO vo = DailyGlucoseStatVO.empty(date);
        vo.setSampleCount(m.getSampleCount());
        vo.setAvgGlucose(m.getMeanGlucose());
        vo.setFastingAvg(m.getFastingAvg());
        vo.setPostprandialAvg(m.getPostprandialAvg());
        vo.setMaxGlucose(m.getMaxGlucose());
        vo.setMinGlucose(m.getMinGlucose());
        vo.setTirPercent(m.getTirPercent());
        vo.setHypoEventCount(m.getHypoEventCount());
        return vo;
    }
}
