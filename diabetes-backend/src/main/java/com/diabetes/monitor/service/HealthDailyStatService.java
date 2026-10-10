package com.diabetes.monitor.service;

import java.time.LocalDate;

public interface HealthDailyStatService {
    int aggregateOneDay(Integer userId, LocalDate date);   // 单用户单日：复用 A1 的 Calculator
    int backfill(Integer userId, LocalDate from, LocalDate to);
}
