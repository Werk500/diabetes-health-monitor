package com.diabetes.monitor.config;

import com.diabetes.monitor.domain.metric.BloodGlucoseMetricsCalculator;
import com.diabetes.monitor.domain.metric.GlucoseThresholds;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 唯一桥接点——把 Spring 的配置对象翻译成纯域对象，并注册 Calculator Bean。
 */
@Configuration
public class MetricConfig {
    @Bean
    public BloodGlucoseMetricsCalculator bloodGlucoseMetricsCalculator(GlucoseProperties p) {
        return new BloodGlucoseMetricsCalculator(new GlucoseThresholds(
                p.getLow(), p.getHigh(), p.getTirTarget(), p.getCvTarget(), p.getHypoMergeMinutes()));
    }
}
