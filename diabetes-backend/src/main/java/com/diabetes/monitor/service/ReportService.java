package com.diabetes.monitor.service;

import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

public interface ReportService {

    /**
     * 生成健康报告 PDF。
     * @param startDate 统计开始日（null = 默认近 7 天窗口）
     * @param endDate   统计结束日（null = 今天）
     */
    ResponseEntity<byte[]> generateReport(Integer userId, LocalDate startDate, LocalDate endDate);
}
