package com.diabetes.monitor.domain.metric;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 结果对象
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BloodGlucoseMetrics {
    // ---- 样本情况 ----
    private int sampleCount;          // 有效样本数
    private int droppedCount;         // 被丢弃的脏数据数
    private boolean enough;           // 样本是否充足（>=5 条）

    // ---- 集中趋势 ----
    private Double meanGlucose;       // 平均值
    private Double fastingAvg;        // 空腹均值（measureType=1）
    private Double postprandialAvg;   // 餐后2h均值（measureType=2）
    private Double randomAvg;         // 随机均值（其余）

    // ---- 波动性 ----
    private Double sd;                // 样本标准差（n-1），n<2 时为 null
    private Double cv;                // 变异系数 %，mean<=0 时为 null

    // ---- 范围内占比 ----
    private Double tirPercent;        // 3.9~10.0 占比
    private Double tarPercent;        // >10.0 占比
    private Double tbrPercent;        // <3.9 占比

    // ---- 事件计数 ----
    private int hypoEventCount;       // 低血糖事件数（按 120 分钟归并）
    private int severeHypoCount;      // <3.0 的条数
    private int hyperCount;           // >10.0 的条数

    // ---- 长期指标与极值 ----
    private Double gmiPercent;        // 估算糖化血红蛋白 %
    private Double maxGlucose;
    private Double minGlucose;
    private boolean tirAchieved;      // tirPercent >= tirTarget

    // ---- 统计窗口 ----
    private LocalDateTime windowFrom;
    private LocalDateTime windowTo;
}
