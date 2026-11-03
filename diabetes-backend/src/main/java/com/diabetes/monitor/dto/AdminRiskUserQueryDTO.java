package com.diabetes.monitor.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminRiskUserQueryDTO {
    @Min(1)
    private long page = 1;

    @Min(1)
    @Max(100)
    private long size = 10;

    @Size(max = 50)
    private String keyword;

    @Min(1)
    @Max(3)
    private Integer riskLevel;
}
