package com.diabetes.monitor.domain.metric;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 纯读模型（不依赖 entity）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GlucoseReading {

    private Double value;//血糖数值
    private Integer measureType;//测量类型
    private LocalDateTime measureTime;//测量时间
}
