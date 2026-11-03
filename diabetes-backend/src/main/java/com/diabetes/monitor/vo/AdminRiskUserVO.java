package com.diabetes.monitor.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminRiskUserVO {

    private Integer userId;
    private String username;
    private String realName;
    private String phone;

    private Integer riskLevel;
    private Integer riskScore;

    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss",
            timezone = "Asia/Shanghai"
    )
    private LocalDateTime lastMeasureTime;

    private Integer inactiveDays;
    private Integer recentRecordCount;
    private Integer abnormalCount;
    private Double highestBloodSugar;
    private String riskReasons;
}
