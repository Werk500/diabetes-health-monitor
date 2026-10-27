package com.diabetes.monitor.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class AdminAuditQueryDTO {
    @Min(value = 1, message = "页码不能小于 1")
    private long page = 1;

    @Min(value = 1, message = "每页数量不能小于 1")
    @Max(value = 100, message = "每页最多查询 100 条")
    private long size = 20;

    private String keyword;

    private String actionCode;

    @Min(value = 0, message = "操作结果只能为 0 或 1")
    @Max(value = 1, message = "操作结果只能为 0 或 1")
    private Integer success;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;
}
