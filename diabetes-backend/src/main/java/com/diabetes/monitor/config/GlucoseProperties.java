package com.diabetes.monitor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 把 application.yml 中 app.glucose.* 绑定成 Spring Bean。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.glucose")
public class GlucoseProperties {
    private double low;
    private double high;
    private double tirTarget;
    private double cvTarget;
    private int hypoMergeMinutes;
}
