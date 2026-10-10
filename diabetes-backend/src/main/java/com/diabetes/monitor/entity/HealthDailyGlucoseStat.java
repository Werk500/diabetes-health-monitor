package com.diabetes.monitor.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("health_daily_glucose_stat")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthDailyGlucoseStat {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer userId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate statDate;

    private Integer sampleCount;      // 当日有效读数条数
    private Double avgGlucose;        // 当日平均血糖
    private Double fastingAvg;        // 当日空腹均值
    private Double postprandialAvg;   // 当日餐后2h均值
    private Double maxGlucose;        // 当日最高
    private Double minGlucose;        // 当日最低
    private Double tirPercent;        // 当日 TIR(%)
    private Integer hypoEventCount;   // 当日低血糖事件数
    private Integer calcVersion;      // 计算口径版本

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
