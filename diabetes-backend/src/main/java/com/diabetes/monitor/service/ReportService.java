package com.diabetes.monitor.service;

import org.springframework.http.ResponseEntity;

public interface ReportService {
    ResponseEntity<byte[]> generateReport(Integer userId);
}
