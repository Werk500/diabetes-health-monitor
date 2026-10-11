package com.diabetes.monitor.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DailyGlucoseStatVO {

    private LocalDate statDate;
    private Integer sampleCount;
    private Double avgGlucose;
    private Double fastingAvg;
    private Double postprandialAvg;
    private Double maxGlucose;
    private Double minGlucose;
    private Double tirPercent;
    private Integer hypoEventCount;

    /** 无记录的一天：占位，保证日期连续 */
    public static DailyGlucoseStatVO empty(LocalDate date) {
        DailyGlucoseStatVO vo = new DailyGlucoseStatVO();
        vo.setStatDate(date);
        vo.setSampleCount(0);
        vo.setHypoEventCount(0);
        return vo;
    }
}
