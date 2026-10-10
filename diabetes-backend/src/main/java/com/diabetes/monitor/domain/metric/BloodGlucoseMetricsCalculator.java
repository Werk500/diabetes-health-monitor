package com.diabetes.monitor.domain.metric;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 纯计算（无 Spring、无 DB）
 */
public final  class BloodGlucoseMetricsCalculator {

    private static final double MIN_VALID = 0.0;      // 低于此值视为脏数据
    private static final double MAX_VALID = 33.3;     // 高于此值视为脏数据
    private static final int MIN_SAMPLE = 5;          // 少于此条数 enough=false
    private static final double SEVERE_LOW = 3.0;     // 严重低血糖线（固定临床值，不走配置）

    /**
     * 测量类型字典：以 sql/init.sql 为准，全项目统一
     * 1-空腹 2-餐前 3-餐后2h 4-睡前 5-凌晨
     */
    private static final int TYPE_FASTING = 1;
    private static final int TYPE_BEFORE_MEAL = 2;
    private static final int TYPE_AFTER_MEAL = 3;


    private final GlucoseThresholds thresholds;


    public BloodGlucoseMetricsCalculator(GlucoseThresholds thresholds) {
        this.thresholds = thresholds;
    }

    public BloodGlucoseMetrics calculate(List<GlucoseReading> raw, LocalDateTime windowFrom, LocalDateTime windowTo) {
        // ---- 1. 过滤脏数据 + 裁窗口 + 排序（事件归并依赖有序） ----
        // valid : 通过校验、且落在窗口内的读数
        List<GlucoseReading> valid = new ArrayList<>();

        // dropped : 脏数据计数（用于数据质量反馈），与"窗口外"区分开
        int dropped = 0;
        if(raw!=null){
            for (GlucoseReading r : raw) {
                // 脏数据判定：对象为 null、值缺失、时间缺失、值超出人体生理范围
                if(r == null || r.getValue() == null || r.getMeasureTime() == null
                ||r.getValue() <= MIN_VALID || r.getValue() > MAX_VALID) {
                    dropped++;
                    continue;
                }

                // 窗口外：不算脏数据，静默忽略（左闭右开 [windowFrom, windowTo)）
                if(r.getMeasureTime().isBefore(windowFrom) || !r.getMeasureTime().isBefore(windowTo))
                {
                    continue;
                }

                valid.add(r);
            }
        }

        // 事件检测（如低血糖事件归并）依赖时间有序，故此处必须排序
        valid.sort(Comparator.comparing(GlucoseReading::getMeasureTime));

        // ---- 2. 先把"样本情况"定下来 ----
        // 无论后续能否算出派生指标，样本元信息一定先写入 builder
        int n = valid.size();
        BloodGlucoseMetrics.BloodGlucoseMetricsBuilder b = BloodGlucoseMetrics.builder()
                .sampleCount(n)
                .droppedCount(dropped)
                .enough(n >= MIN_SAMPLE) // 样本是否达到最小分析门槛
                .windowFrom(windowFrom)
                .windowTo(windowTo);

        // ---- 3. 空样本早返回：所有派生的 Double 字段保持 null，绝不给 0 ----
        // 关键语义：0 表示"测得 0"，null 表示"无法计算"，二者在医疗场景不可混淆
        if(n==0){
            return b.build();
        }

        // ---- 4. 集中趋势 ----
        double mean = mean(valid);                       // 整体均值
        Double fastingAvg = groupMean(valid, TYPE_FASTING);          // 1 → 空腹均值
        Double postprandialAvg = groupMean(valid, TYPE_AFTER_MEAL);  // 3 → 餐后2h均值
        Double otherAvg = groupMean(valid, null);                    // 其余（餐前/睡前/凌晨/未填）

        // ---- 5. 波动性：先算原始值，最后统一舍入 ----
        // 先不舍入 sd，避免中间精度损失影响 cv
        Double sd = sampleSd(valid, mean);               // 样本标准差（n-1 分母）
        // CV 仅在 sd 存在且 mean 为正时有意义；否则 null
        Double cv = (sd == null || mean <= 0) ? null : round1(sd / mean * 100);

        // ---- 6. 范围内占比与计数 ----
        // TIR / TAR / TBR 三者之和理论上应 = 100%（受舍入影响可能有 ±0.1 偏差）
        Double tir = round1(percentInRange(valid));      // Time In Range
        Double tar = round1(percentAbove(valid));        // Time Above Range
        Double tbr = round1(percentBelow(valid));        // Time Below Range
        int hyperCount = countAbove(valid, thresholds.getHigh());  // 高血糖次数（阈值可配置）
        int severeHypoCount = countBelow(valid, SEVERE_LOW);       // 严重低血糖次数（固定常量）

        // ---- 7. 低血糖事件（入参已排序） ----
        // 相邻低血糖读数会被归并为一次"事件"，避免一次持续低血糖被重复计数
        int hypoEventCount = countHypoEvents(valid);

        // ---- 8. 极值与 GMI ----
        // orElseThrow 安全：此处 n > 0，stream 必非空
        Double max = round1(valid.stream().mapToDouble(GlucoseReading::getValue).max().orElseThrow());
        Double min = round1(valid.stream().mapToDouble(GlucoseReading::getValue).min().orElseThrow());
        // GMI 是反映长期（约3个月）血糖控制的指标，样本不足时计算无意义 → null
        Double gmi = n >= MIN_SAMPLE ? gmi(mean) : null;

        // ---- 9. 组装结果 ----
        return b.meanGlucose(round1(mean))
                .fastingAvg(fastingAvg)
                .postprandialAvg(postprandialAvg)
            .otherAvg(otherAvg)
                .sd(round1(sd))
                .cv(cv)
                .tirPercent(tir)
                .tarPercent(tar)
                .tbrPercent(tbr)
                .hypoEventCount(hypoEventCount)
                .severeHypoCount(severeHypoCount)
                .hyperCount(hyperCount)
                .gmiPercent(gmi)
                .maxGlucose(max)
                .minGlucose(min)
                // TIR 达标判定：需 tir 非 null 且达到阈值
                .tirAchieved(tir != null && tir >= thresholds.getTirTarget())
                .build();
    }

    /**
     * 分组均值：measureType 传具体值取该组，传 null 取"其余"组（未定义类型的记录）；
     * 该组无数据返回 null
     */
    private Double groupMean(List<GlucoseReading> rs, Integer measureType) {
        List<GlucoseReading> sub = rs.stream()
                .filter(r -> measureType == null
                        ? !isCoreType(r.getMeasureType())
                        : measureType.equals(r.getMeasureType()))
                .toList();
        return sub.isEmpty() ? null : round1(mean(sub));

    }

    /** 是否属于"空腹(1) / 餐后2h(3)"这两个核心分组；其余（餐前/睡前/凌晨/未填）都归入 otherAvg */
    private static boolean isCoreType(Integer type) {
        return type != null && (type == TYPE_FASTING || type == TYPE_AFTER_MEAL);
    }

    /** 均值：调用前保证集合非空 */
    private double mean(List<GlucoseReading> rs) {
        double mean = 0;
        for (GlucoseReading r : rs) {
            mean += r.getValue();
        }
        return mean / rs.size();
    }

    /** 样本标准差（n-1）：少于 2 条返回 null，不能返回 0 */
    private Double sampleSd(List<GlucoseReading> rs, double mean) {
        if (rs.size() < 2) {
            return null;
        }
        double sumSq = 0;
        for (GlucoseReading r : rs) {
            double d = r.getValue() - mean;
            sumSq += d * d;
        }
        return Math.sqrt(sumSq / (rs.size() - 1));
    }

    /** 低血糖事件归并：回到范围内即结束当前事件；相邻低血糖间隔 <= 阈值视为同一事件 */
    private int countHypoEvents(List<GlucoseReading> sorted) {
        double low = thresholds.getLow();
        int events = 0;
        boolean inEvent = false;
        LocalDateTime lastHypoAt = null;

        for (GlucoseReading r : sorted) {
            if (r.getValue() >= low) {          // 回到范围内 -> 事件结束
                inEvent = false;
                continue;
            }
            boolean continued = inEvent && lastHypoAt != null
                    && Duration.between(lastHypoAt, r.getMeasureTime()).toMinutes()
                    <= thresholds.getHypoMergeMinutes();
            if (!continued) {
                events++;
            }
            inEvent = true;
            lastHypoAt = r.getMeasureTime();
        }
        return events;
    }

    /** 统一在此处舍入：BigDecimal 走十进制字符串，避免 Math.round 的二进制误差 */
    private static Double round1(Double v) {
        return v == null ? null : BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    //TIR（目标范围内时间占比）%
    private double percentInRange(List<GlucoseReading> rs) {
        long c = rs.stream().filter(r -> r.getValue() >= thresholds.getLow()
                && r.getValue() <= thresholds.getHigh()).count();
        return c * 100.0 / rs.size();
    }

    //TAR（高于目标范围占比）%
    private double percentAbove(List<GlucoseReading> rs) {
        return countAbove(rs, thresholds.getHigh()) * 100.0 / rs.size();
    }
    //TBR（低于目标范围占比）%
    private double percentBelow(List<GlucoseReading> rs) {
        return countBelow(rs, thresholds.getLow()) * 100.0 / rs.size();
    }

    //高血糖次数
    private static int countAbove(List<GlucoseReading> rs, double bound) {
        return (int) rs.stream().filter(r -> r.getValue() > bound).count();
    }
    //低血糖次数
    private static int countBelow(List<GlucoseReading> rs, double bound) {
        return (int) rs.stream().filter(r -> r.getValue() < bound).count();
    }

    /** GMI = 3.31 + 0.02392 × 平均血糖(mg/dL)，换算成 mmol/L 系数即 0.4311 */
    private static Double gmi(double meanMmolL) {
        return round1(3.31 + 0.4311 * meanMmolL);
    }

}
