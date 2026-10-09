package com.diabetes.monitor.domain.metric;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 阈值常量对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GlucoseThresholds {

    private double low;              // 目标范围下界 3.9
    private double high;             // 目标范围上界 10.0
    private double tirTarget;        // TIR 达标线 70
    private double cvTarget;         // CV 上限 36
    private int hypoMergeMinutes;    // 低血糖事件归并窗口 120
}
