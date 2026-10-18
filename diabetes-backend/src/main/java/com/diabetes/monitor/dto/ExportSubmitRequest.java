package com.diabetes.monitor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ExportSubmitRequest {
    @NotBlank(message = "导出类型不能为空")
    private String type;          // EXCEL / PDF

    private LocalDate startDate;
    private LocalDate endDate;    // 两个都为空 = 导出全部（或默认最近 30 天）
}
