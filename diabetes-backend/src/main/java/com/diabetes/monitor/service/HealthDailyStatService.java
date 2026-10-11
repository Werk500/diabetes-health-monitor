package com.diabetes.monitor.service;

import com.diabetes.monitor.dto.DailyGlucoseStatVO;
import com.diabetes.monitor.entity.HealthDailyGlucoseStat;

import java.time.LocalDate;
import java.util.List;

public interface HealthDailyStatService {
    int aggregateOneDay(Integer userId, LocalDate date);   // 单用户单日：复用 A1 的 Calculator
    int backfill(Integer userId, LocalDate from, LocalDate to);

    /** 读聚合表：区间内每天的聚合结果（不包含今日兜底） */
    List<HealthDailyGlucoseStat> listByRange(Integer userId, LocalDate from, LocalDate to);

    /** 趋势：聚合表 + 今日实时兜底，按日期升序补齐断档 */
    List<DailyGlucoseStatVO> trend(Integer userId, int days);
}
